
package AudioPlayer.services.playcount;

import javafx.util.Duration;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playback.PlayTimeHandler;
import AudioPlayer.services.Service.ServiceBase;
import AudioPlayer.services.notif.Notifier;
import AudioPlayer.services.tray.TrayService;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataWriter;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import main.App;
import util.access.Accessor;

import static AudioPlayer.services.playcount.PlaycountIncrementer.PlaycountIncrStrategy.*;
import static java.awt.TrayIcon.MessageType.INFO;
import static javafx.util.Duration.seconds;
import static util.functional.Util.max;
import static util.functional.Util.min;

/** Playcount incrementing service. */
@IsConfigurable(value = "Playcount Incrementing")
public class PlaycountIncrementer extends ServiceBase {
    
    @IsConfig(name="Incrementing strategy", info = "Playcount strategy for incrementing playback.")
    public final Accessor<PlaycountIncrStrategy> when = new Accessor<>(ON_PERCENT,this::apply);
    @IsConfig(name="Increment at percent", info = "Percent at which playcount is incremented.")
    public final Accessor<Double> when_percent = new Accessor<>(0.4,this::apply);
    @IsConfig(name="Increment at time", info = "Time at which playcount is incremented.")
    public final Accessor<Duration> when_time = new Accessor<>(seconds(5),this::apply);
    @IsConfig(name="Show notification", info = "Shows notification when playcount is incremented.")
    public final Accessor<Boolean> show_notif = new Accessor<>(false);
    @IsConfig(name="Show tray bubble", info = "Shows tray bubble notification when playcount is incremented.")
    public final Accessor<Boolean> show_bubble = new Accessor<>(false);

    private final Runnable incr = this::increment;
    private PlayTimeHandler incrHand;
    private boolean running = false;
    
        
    public PlaycountIncrementer() {
        super(false);
    }
    
    @Override
    public void start() {
        running = true;
        apply();
    }

    @Override
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public void stop() {
        running = false;
        apply();
    }

    @Override
    public boolean isDependency() {
        return false;
    }

    @Override
    public boolean isSupported() {
        return true;
    }
    
    public void increment() {
        Metadata m = Player.playingtem.get();
        if (!m.isEmpty() && m.isFileBased() ) {
            int p = m.getPlaycount()+1;
            MetadataWriter.use(m, w -> w.setPlaycount(p), ok -> {
                if(ok) {
                    if(show_notif.get()) App.use(Notifier.class, n -> n.showTextNotification("Song playcount incremented to: " + p, "Update"));
                    if(show_bubble.get()) App.use(TrayService.class, t -> t.showNotification("Tagger", "Playcount incrememted to: " + p, INFO));
                }
            });
        }
    };
    
    private void apply() {
        removeOld();
        if(!running) return;
        
        if (when.get() == ON_PERCENT) {
            incrHand = new PlayTimeHandler(total -> total.multiply(when_percent.get()),incr);
            PLAYBACK.addOnPlaybackAt(incrHand);
        } else if (when.get() == ON_TIME) {
            incrHand = new PlayTimeHandler(total -> when_time.get(), incr);
            PLAYBACK.addOnPlaybackAt(incrHand);
        } else if (when.get() == ON_TIME_AND_PERCENT) {
            incrHand = new PlayTimeHandler(total -> min(when_time.get(),total.multiply(when_percent.get())),incr);
            PLAYBACK.addOnPlaybackAt(incrHand);
        } else if (when.get() == ON_TIME_OR_PERCENT) {
            incrHand = new PlayTimeHandler(total -> max(when_time.get(),total.multiply(when_percent.get())),incr);
            PLAYBACK.addOnPlaybackAt(incrHand);
        } else if (when.get() == ON_START) {
            PLAYBACK.addOnPlaybackStart(incr);
        } else if (when.get() == ON_END) {
            PLAYBACK.addOnPlaybackEnd(incr);
        } else if (when.get() == NEVER) {}
    }
    
    private void removeOld() {
        if(incrHand!=null) PLAYBACK.removeOnPlaybackAt(incrHand);
        PLAYBACK.removeOnPlaybackStart(incr);
        PLAYBACK.removeOnPlaybackEnd(incr);
    }
    
    
    /** Strategy for incrementing playcount. */
   public static enum PlaycountIncrStrategy {
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
       NEVER;
   }
}