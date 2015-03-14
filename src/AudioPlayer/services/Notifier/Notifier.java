
package AudioPlayer.services.Notifier;

import Action.IsAction;
import Action.IsActionable;
import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.services.Service;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.InfoNode.SongInfo;
import GUI.InfoNode.ItemInfo;
import GUI.objects.PopOver.Notification;
import GUI.objects.PopOver.PopOver;
import static GUI.objects.PopOver.PopOver.ScreenCentricPos.ScreenTopRight;
import GUI.objects.Text;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NEW;
import java.util.List;
import static java.util.stream.Collectors.toList;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer.Status;
import static javafx.scene.media.MediaPlayer.Status.*;
import javafx.util.Duration;
import main.App;
import org.reactfx.Subscription;
import util.access.AccessorAction;
import util.access.AccessorEnum;

/**
 * 
 * @author uranium
 */
@IsConfigurable("Notification")
@IsActionable
public final class Notifier implements Service {
    
/*****************************   CONFIGURATION   ******************************/
    
    private static Notification n;
    private static Node songNotifGui;
    private static SongInfo songNotifInfo;
    private static List<String> getSongGuiFactoryNames() {
        List<String> l = WidgetManager.getFactories()
                            .filter(f->f.hasFeature(SongInfo.class))
                            .map(f->f.name()).collect(toList());
        l.add("Normal");
        l.add("Normal - no cover");
        return l;
    }
    
    // dependencies
    private Subscription d1, d2;
    
    @IsConfig(name = "Show notifications.", info = "Turn notification on and off completely")
    public static boolean showNotification = true;
    @IsConfig(name = "Show playback status notifications")
    public static boolean showStatusNotification = true;
    @IsConfig(name = "Show no playing notifications")
    public static boolean showSongNotification = true;
    @IsConfig(name = "Notification autohide delay", info = "Time it takes for the notification to hide on its own")
    public static double notificationDuration = 2500;
    @IsConfig(name = "Animate notifications", info = "Use animations on the notification")
    public static boolean notifAnimated = true;
    @IsConfig(name = "Fade in/out animation duration for notification")
    public static double notifFadeTime = 500;
    @IsConfig(name = "Close notification on click anywhere")
    public static boolean notifAutohide = true;
    @IsConfig(name = "Notification position")
    public static PopOver.ScreenCentricPos notifPos = ScreenTopRight;
    @IsConfig(name = "On left click")
    public static final AccessorAction onClickL = new AccessorAction("Show application", null);
    @IsConfig(name = "On right click")
    public static final AccessorAction onClickR = new AccessorAction("Notification hide", null);
    
    @IsConfig(name = "Playback change graphics")
    public static final AccessorEnum<String> graphics = new AccessorEnum<String>("Normal", 
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
                songNotifInfo = (SongInfo)wf.getController();
//                songNotifInfo = WidgetManager.find(SongInfo.class, NEW).get();
//                songNotifGui = ((Controller)songNotifInfo).getWidget().load();
                ((Pane)songNotifGui).setPrefSize(700, 300);
            }
//            App.use(Notifier.class,nnn->{
//                if(nnn.initialized) nnn.notifNowPlaying();
//                else nnn.initialized = true;
//            });
        },Notifier::getSongGuiFactoryNames){
            
        };
    
    @IsAction(name = "Notification hide")
    public static void notifHide() {
        App.use(Notifier.class, Notifier::hideNotification);
    }
    @IsAction(name = "Notify now playing", description = "Shows notification about currently playing song.", global = true, shortcut = "ALT + N")
    public static void notifNowPlaying() {
        App.use(Notifier.class, nm -> nm.songChange(Player.playingtem.get()));
    }
    
    
/*******************************   SERVICE   **********************************/
    
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
    
/******************************** PRIVATE *************************************/
    
    private void songChange(Metadata m) {
        if (showSongNotification) {
            String title = "Now playing";
            songNotifInfo.setValue(m);
            
            showNotification(songNotifGui, title);
        }
    }
    
    private void playbackChange(Status s) {
        if (showSongNotification || s == null) {
            String title = "Playback change : " + s;
            SongInfo i = new ItemInfo(false);
                     i.setValue(Player.playingtem.get());
                     
            showNotification((Node)i, title);
        }
    }
    
    /** Show notification for custom content. */
    public void showNotification(Node content, String title) {
        if (showNotification) {
            // build content
            n.setContent(content, title);
            // set properties (that could have changed from last time)
            n.setAutoHide(notifAutohide);
            n.setAnimated(notifAnimated);
            n.setAnimDuration(Duration.millis(notifFadeTime));
            n.setDuration(Duration.millis(notificationDuration));
            n.lClickAction = onClickL.getValueAction();
            n.rClickAction = onClickR.getValueAction();
            // show
            n.show(notifPos);
        }
    }
    
    /** Show notification for text. */
    public void showTextNotification(String text, String title) {
        Text message = new Text(text);
             message.setWrappingWidthNatural(true);
        StackPane root = new StackPane(message);
                  root.setMinSize(150, 70);
        // textContainer.setPadding(Insets.EMPTY);

        showNotification(root, title);
    }
    
    private void hideNotification() {
        n.hide();
    }
    
}