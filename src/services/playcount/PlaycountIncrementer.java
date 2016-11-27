package services.playcount;

import java.util.ArrayList;
import java.util.List;

import javafx.util.Duration;

import org.reactfx.Subscription;

import audio.Player;
import audio.playback.PLAYBACK;
import audio.playback.PlayTimeHandler;
import audio.tagging.Metadata;
import audio.tagging.MetadataReader;
import audio.tagging.MetadataWriter;
import services.Service.ServiceBase;
import services.notif.Notifier;
import services.tray.TrayService;
import util.access.V;
import util.action.IsAction;
import util.conf.IsConfig;
import util.conf.IsConfigurable;

import static java.awt.TrayIcon.MessageType.INFO;
import static javafx.util.Duration.seconds;
import static main.App.APP;
import static services.playcount.PlaycountIncrementer.PlaycountIncStrategy.*;
import static util.functional.Util.max;
import static util.functional.Util.min;

/** Playcount incrementing service. */
@IsConfigurable(value = "Playback.Playcount.Incrementing")
public class PlaycountIncrementer extends ServiceBase {

	@IsConfig(name="Incrementing strategy", info = "Playcount strategy for incrementing playback.")
	public final V<PlaycountIncStrategy> when = new V<>(ON_PERCENT,this::apply);
	@IsConfig(name="Increment at percent", info = "Percent at which playcount is incremented.")
	public final V<Double> when_percent = new V<>(0.4,this::apply);
	@IsConfig(name="Increment at time", info = "Time at which playcount is incremented.")
	public final V<Duration> when_time = new V<>(seconds(5),this::apply);
	@IsConfig(name="Show notification", info = "Shows notification when playcount is incremented.")
	public final V<Boolean> show_notif = new V<>(false);
	@IsConfig(name="Show tray bubble", info = "Shows tray bubble notification when playcount is incremented.")
	public final V<Boolean> show_bubble = new V<>(false);
	@IsConfig(name="Delay writing", info = "Delays writing playcount to tag for more seamless "
			+ "playback experience. In addition, reduces multiple consecutive increments in a row "
			+ "to a single operation. The writing happens when different song starts playing "
			+ "(but the data in the application may update visually even later).")
	public final V<Boolean> delay = new V<>(true);

	private final List<Metadata> queue = new ArrayList<>();
	private final Runnable inc = this::increment;
	private PlayTimeHandler incHandler;
	private boolean running = false;
	private Subscription d = null;

	public PlaycountIncrementer() {
		super(false);
	}

	@Override
	public void start() {
		running = true;
		apply();
		d = Player.playingItem.onChange((o, n) -> incrementQueued(o));
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void stop() {
		running = false;
		apply();
		d.unsubscribe();
		// note that we are potentially losing queued playcounts here. If the service never starts
		// again, all queued playcounts will never get written to tag
		// we could just write them all to tag right here, but that may cause problems such as
		// unwanted application close delay.
		// This should not be a big problem, unless user listened to single song lot of the times
		// in a loop and then disabled this service and closed the app. We really do not care about
		// such scenario. Plus, few lost playcounts are no big deal.
		//
		// fix below:
		// queue.stream().distinct().forEach(this::incrementQueued);
	}

	@Override
	public boolean isDependency() {
		return false;
	}

	@Override
	public boolean isSupported() {
		return true;
	}

	/**
	 * Increments playcount of currently playing song. According to settings now or schedules it for
	 * later. Also throws notifications if set.
	 */
	@IsAction(name = "Increment playcount", desc = "Rises the number of times the song has been played by one and updates the song tag.")
	public void increment() {
		Metadata m = Player.playingItem.get();
		if (!m.isEmpty() && m.isFileBased() ) {
			if (delay.get()) {
				queue.add(m);
				if (show_notif.get()) APP.use(Notifier.class, n -> n.showTextNotification("Song playcount incrementing scheduled", "Playcount"));
				if (show_bubble.get()) APP.use(TrayService.class, t -> t.showNotification("Tagger", "Playcount incremented scheduled", INFO));
			} else {
				int pc = 1 + m.getPlaycount();
				MetadataWriter.use(m, w -> w.setPlaycount(pc), ok -> {
					if (ok) {
						if (show_notif.get()) APP.use(Notifier.class, n -> n.showTextNotification("Song playcount incremented to: " + pc, "Playcount"));
						if (show_bubble.get()) APP.use(TrayService.class, t -> t.showNotification("Tagger", "Playcount incremented to: " + pc, INFO));
					}
				});
			}
		}
	}

	private void apply() {
		removeOld();
		if (!running) return;

		if (when.get() == ON_PERCENT) {
			incHandler = new PlayTimeHandler(total -> total.multiply(when_percent.get()), inc);
			PLAYBACK.onPlaybackAt.add(incHandler);
		} else if (when.get() == ON_TIME) {
			incHandler = new PlayTimeHandler(total -> when_time.get(), inc);
			PLAYBACK.onPlaybackAt.add(incHandler);
		} else if (when.get() == ON_TIME_AND_PERCENT) {
			incHandler = new PlayTimeHandler(total -> min(when_time.get(),total.multiply(when_percent.get())), inc);
			PLAYBACK.onPlaybackAt.add(incHandler);
		} else if (when.get() == ON_TIME_OR_PERCENT) {
			incHandler = new PlayTimeHandler(total -> max(when_time.get(),total.multiply(when_percent.get())), inc);
			PLAYBACK.onPlaybackAt.add(incHandler);
		} else if (when.get() == ON_START) {
			PLAYBACK.onPlaybackStart.add(inc);
		} else if (when.get() == ON_END) {
			PLAYBACK.onPlaybackEnd.add(inc);
		} else if (when.get() == NEVER) {}
	}

	private void removeOld() {
		PLAYBACK.onPlaybackAt.remove(incHandler);
		PLAYBACK.onPlaybackEnd.remove(inc);
		PLAYBACK.onPlaybackStart.remove(inc);
	}

	private void incrementQueued(Metadata m) {
		int δ = (int) queue.stream().filter(i -> i.same(m)).count();
		if (δ>0) {
			queue.removeIf(i -> i.same(m));
			int p = δ + m.getPlaycount();
			Player.IO_THREAD.execute(() -> {
				MetadataWriter.useNoRefresh(m, w -> w.setPlaycount(p));
				Player.refreshItemWith(MetadataReader.readMetadata(m), true);
			});
		}
	}

	/** Strategy for incrementing playcount. */
	public enum PlaycountIncStrategy {
		/** Increment when song starts playing. */
		ON_START,
		/** Increment when song stops playing naturally. */
		ON_END,
		/** Increment when song is playing for specified time. */
		ON_TIME,
		/** Increment when song is playing for portion of its time. */
		ON_PERCENT,
		/** Increment when song is playing for specified time or portion of its time. */
		ON_TIME_OR_PERCENT,
		/** Increment when song is playing for specified time and portion of its time. */
		ON_TIME_AND_PERCENT,
		/** Never increment. */
		NEVER
   }
}