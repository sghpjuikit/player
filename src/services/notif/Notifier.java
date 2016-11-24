
package services.notif;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

import org.reactfx.Subscription;

import audio.Player;
import audio.playback.PLAYBACK;
import audio.tagging.Metadata;
import gui.infonode.ItemInfo;
import gui.objects.Text;
import gui.objects.popover.Notification;
import gui.objects.popover.PopOver;
import layout.widget.WidgetFactory;
import layout.widget.feature.SongReader;
import services.Service.ServiceBase;
import util.access.VarAction;
import util.access.VarEnum;
import util.action.IsAction;
import util.action.IsActionable;
import util.conf.IsConfig;
import util.conf.IsConfig.EditMode;
import util.conf.IsConfigurable;

import static gui.objects.popover.PopOver.ScreenPos.Screen_Bottom_Right;
import static gui.objects.popover.PopOver.ScreenUse.APP_WINDOW;
import static javafx.scene.media.MediaPlayer.Status.*;
import static javafx.util.Duration.millis;
import static layout.widget.WidgetManager.WidgetSource.NEW;
import static main.App.APP;

/** Provides notification functionality. */
@IsActionable
@IsConfigurable("Notifications")
public final class Notifier extends ServiceBase {

	@IsAction(name = "Notification hide")
	public static void notifHide() {
		APP.use(Notifier.class, Notifier::hideNotification);
	}
	@IsAction(name = "Notify now playing", desc = "Shows notification about currently playing song.", global = true, keys = "ALT + N")
	public static void notifNowPlaying() {
		APP.use(Notifier.class, nm -> nm.songChange(Player.playingItem.get()));
	}

	private static Notification n;
	private static Node songNotifGui;
	private static SongReader songNotifInfo;
	private Subscription d1, d2;

	@IsConfig(name = "On playback status change")
	public boolean showStatusNotification = true;
	@IsConfig(name = "On playing song change")
	public boolean showSongNotification = true;
	@IsConfig(name = "Autohide", info = "Whether notification hides on mouse click anywhere within the application", editable = EditMode.NONE)
	public boolean notifAutohide = false;
	@IsConfig(name = "Autohide delay", info = "Time it takes for the notification to hide on its own")
	public Duration notificationDuration = millis(2500);
	@IsConfig(name = "Animate", info = "Use animations on the notification")
	public boolean notifAnimated = true;
	@IsConfig(name = "Animation duration")
	public Duration notifFadeTime = millis(500);
	@IsConfig(name = "Position", info = "Position within the virtual bounding box, which is relative to screen or window")
	public PopOver.ScreenPos notifPos = Screen_Bottom_Right;
	@IsConfig(name = "Position relative to", info = "Determines screen for positioning. Main screen, application window screen or all screens as one")
	public PopOver.ScreenUse notifScr = APP_WINDOW;
	@IsConfig(name = "On click left", info = "Left click action")
	public final VarAction onClickL = new VarAction("Show application", null);
	@IsConfig(name = "On click right", info = "Right click action")
	public final VarAction onClickR = new VarAction("Notification hide", null);

	@IsConfig(name = "Playback change graphics")
	public final VarEnum<String> graphics = new VarEnum<String>("Normal",
		() -> APP.widgetManager.getFactories()
					.filter(f -> f.hasFeature(SongReader.class))
					.map(WidgetFactory::nameGui)
					.append("Normal")
					.append("Normal - no cover")
					.toList()
		,
		v -> {
			if ("Normal".equals(v)) {
				ItemInfo ii = new ItemInfo(true);
				songNotifInfo = ii;
				songNotifGui = ii;
				((Pane)songNotifGui).setPrefSize(-1,-1);
			}
			else if ("Normal - no cover".equals(v)) {
				ItemInfo ii = new ItemInfo(false);
				songNotifInfo = ii;
				songNotifGui = ii;
				((Pane)songNotifGui).setPrefSize(-1,-1);
			} else {
				APP.widgetManager.find(v, NEW, true).ifPresent( wf -> {
					songNotifGui = wf.load();
					songNotifInfo = (SongReader)wf.getController();
					((Pane)songNotifGui).setPrefSize(700, 300);
				});
			}
		});


	public Notifier() {
		super(true);
	}

	@Override
	public void start() {
		// create notification
		n = new Notification();

		// show notification on playback status change
		ChangeListener<Status> statusListener = (o,ov,nv) -> {
			if (nv == PAUSED || nv ==PLAYING || nv == STOPPED)
				playbackChange(nv);
		};
		PLAYBACK.statusProperty().addListener(statusListener);
		d2 = () -> PLAYBACK.statusProperty().removeListener(statusListener);

		// show notification on song change
		d1 = Player.playingItem.onChange(this::songChange);
	}

	@Override
	public boolean isRunning() {
		return n!=null;
	}

	@Override
	public void stop() {
		if (d1!=null) d1.unsubscribe();
		if (d2!=null) d2.unsubscribe();
		if (n!=null) n.hideImmediatelly();
		n = null;
	}

	@Override
	public boolean isSupported() { return true; }

	@Override
	public boolean isDependency() { return false; }

	/** Show notification for custom content. */
	public void showNotification(Node content, String title) {
		if (isRunning()) {
			// build content
			n.setContent(content, title);
			// set properties (that could have changed from last time)
			n.setAutoHide(notifAutohide);
			n.setAnimated(notifAnimated);
			n.setAnimDuration(notifFadeTime);
			n.setDuration(notificationDuration);
			n.lClickAction = onClickL.getValueAction();
			n.rClickAction = onClickR.getValueAction();
			// show
			n.screen_preference = notifScr;
			n.show(notifPos);
		}
	}

	/** Show notification displaying given text. */
	public void showTextNotification(String text, String title) {
		if (isRunning()) {
			Text message = new Text(text);
				 message.setWrappingWidthNatural(true);
			StackPane root = new StackPane(message);
					  root.setMinSize(150, 70);
			// textContainer.setPadding(Insets.EMPTY);

			showNotification(root, title);
		}
	}

	/** Hide notification if showing, otherwise does nothing. */
	public void hideNotification() {
		if (isRunning()) {
			n.hide();
		}
	}

	private void songChange(Metadata m) {
		if (showSongNotification) {
			String title = "Now playing";
			songNotifInfo.read(m);

			showNotification(songNotifGui, title);
		}
	}

	private void playbackChange(Status s) {
		if (showSongNotification || s == null) {
			String title = "Playback change : " + s;
			SongReader i = new ItemInfo(false);
					   i.read(Player.playingItem.get());

			showNotification((Node)i, title);
		}
	}

}