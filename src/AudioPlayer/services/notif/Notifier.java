
package AudioPlayer.services.notif;

import action.IsAction;
import action.IsActionable;
import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.services.Service.ServiceBase;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NEW;
import Layout.Widgets.feature.SongReader;
import gui.InfoNode.ItemInfo;
import gui.objects.PopOver.Notification;
import gui.objects.PopOver.PopOver;
import static gui.objects.PopOver.PopOver.ScreenCentricPos.Screen_Bottom_Right;
import static gui.objects.PopOver.PopOver.ScreenUse.APP_WINDOW;
import gui.objects.Text;
import java.util.List;
import static java.util.stream.Collectors.toList;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer.Status;
import static javafx.scene.media.MediaPlayer.Status.*;
import javafx.util.Duration;
import static javafx.util.Duration.millis;
import main.App;
import org.reactfx.Subscription;
import util.access.AccessorAction;
import util.access.AccessorEnum;

/** Provides notification functionality. */
@IsActionable
@IsConfigurable("Notifications")
public final class Notifier extends ServiceBase {
    
    @IsAction(name = "Notification hide")
    public static void notifHide() {
        App.use(Notifier.class, Notifier::hideNotification);
    }
    @IsAction(name = "Notify now playing", descr = "Shows notification about currently playing song.", global = true, shortcut = "ALT + N")
    public static void notifNowPlaying() {
        App.use(Notifier.class, nm -> nm.songChange(Player.playingtem.get()));
    }
    
    
    private static Notification n;
    private static Node songNotifGui;
    private static SongReader songNotifInfo;
    
    // dependencies
    private Subscription d1, d2;
    
/*****************************   CONFIGURATION   ******************************/
    
    @IsConfig(name = "On playback status change")
    public boolean showStatusNotification = true;
    @IsConfig(name = "On playing song change")
    public boolean showSongNotification = true;
    @IsConfig(name = "Autohide delay", info = "Time it takes for the notification to hide on its own")
    public Duration notificationDuration = millis(2500);
    @IsConfig(name = "Animate", info = "Use animations on the notification")
    public boolean notifAnimated = true;
    @IsConfig(name = "Animation duration")
    public Duration notifFadeTime = millis(500);
    @IsConfig(name = "Hide on click anywhere", editable = false)
    public boolean notifAutohide = false;
    @IsConfig(name = "Screen Position")
    public PopOver.ScreenCentricPos notifPos = Screen_Bottom_Right;
    @IsConfig(name = "Screen", info = "Decides which screen to use for positioning. Main screen, application window screen or all screens as one")
    public PopOver.ScreenUse notifScr = APP_WINDOW;
    @IsConfig(name = "On click left")
    public final AccessorAction onClickL = new AccessorAction("Show application", null);
    @IsConfig(name = "On click right")
    public final AccessorAction onClickR = new AccessorAction("Notification hide", null);
    
    @IsConfig(name = "Playback change graphics")
    public final AccessorEnum<String> graphics = new AccessorEnum<>("Normal", 
        v -> {
            if("Normal".equals(v)) {
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
                Widget wf = WidgetManager.find(w->w.name().equals(v), NEW, true).get();
                songNotifGui = wf.load();
                songNotifInfo = (SongReader)wf.getController();
                ((Pane)songNotifGui).setPrefSize(700, 300);
            }
        },() -> {
            List<String> l = WidgetManager.getFactories()
                        .filter(f->f.hasFeature(SongReader.class))
                        .map(f->f.name()).collect(toList());
            l.add("Normal");
            l.add("Normal - no cover");
            return l;
        });
    

    
    public Notifier() {
        super(true);
    }
    
    /** {@inheritDoc} */
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
        d1 = Player.playingtem.subscribeToChanges(this::songChange);
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isRunning() {
        return n!=null;
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        if(d1!=null) d1.unsubscribe();
        if(d2!=null) d1.unsubscribe();
        if(n!=null) n.hideImmediatelly();
        n = null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSupported() { return true; }

    /** {@inheritDoc} */
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
                       i.read(Player.playingtem.get());
                     
            showNotification((Node)i, title);
        }
    }
    
}