
package AudioPlayer.services.Notifier;

import Action.Action;
import Action.IsAction;
import Action.IsActionable;
import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.services.Service;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.objects.Pickers.Notification;
import GUI.objects.Pickers.Notification.NotificationType;
import static GUI.objects.Pickers.Notification.NotificationType.OTHER;
import static GUI.objects.Pickers.Notification.NotificationType.PLAYBACK_STATUS;
import static GUI.objects.Pickers.Notification.NotificationType.SONG;
import static GUI.objects.Pickers.Notification.NotificationType.TEXT;
import GUI.objects.PopOver.PopOver;
import javafx.scene.Node;
import javafx.scene.media.MediaPlayer.Status;
import static javafx.scene.media.MediaPlayer.Status.PAUSED;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.scene.media.MediaPlayer.Status.STOPPED;
import javafx.util.Duration;
import main.App;
import org.reactfx.Subscription;
import util.access.AccessorAction;

/**
 * 
 * @author uranium
 */
@IsConfigurable("Notification")
@IsActionable
public final class NotifierManager implements Service {
    
/*****************************   CONFIGURATION   ******************************/
    
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
    @IsConfig(name = "Close notification when clicked anywhere.")
    public static boolean notifAutohide = true;
    @IsConfig(name = "Notification position.")
    public static PopOver.ScreenCentricPos notifPos = PopOver.ScreenCentricPos.ScreenBottomRight;
    @IsConfig(name = "On Left Click.")
    public static final AccessorAction onClickL = new AccessorAction(Action.getAction("Show/Hide application"), null);
    @IsConfig(name = "On Right Click.")
    public static final AccessorAction onClickR = new AccessorAction(Action.getAction("Notification hide"), null);
    
    @IsAction(name = "Notification hide")
    public static void hideNotif() {
        App.use(NotifierManager.class, nm -> {
            if(nm.isRunning()) nm.hideNotification();
        });
    }
    
/*******************************   SERVICE   **********************************/
    
    /** {@inheritDoc} */
    @Override
    public void start() {
        // create notification
        n = new Notification();
        
        // show notification on playback status change
        PLAYBACK.statusProperty().addListener((o,ov,nv) -> {
            if (nv == PAUSED || nv ==PLAYING || nv == STOPPED)
                playbackStatusChange(nv);
        });
        
        // show notification on song change
        playingItemMonitoring = Player.playingtem.subscribeToChanges(this::songChange);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRunning() {
        return n!=null;
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        if(playingItemMonitoring!=null) playingItemMonitoring.unsubscribe();
        if(n!=null) n.hideImmediatelly();
        n = null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSupported() { return true; }

    /** {@inheritDoc} */
    @Override
    public boolean isDependency() { return false; }
    
/*********************************   API   ************************************/
    
    /** Show notification for custom content. */
    public void showNotification(Node content, String title) {
        showNotification(content, title, OTHER);
    }
    /** Show notification for text. */
    public void showTextNotification(String text, String title) {
        showNotification(text, title, TEXT);
    }
    
/******************************** PRIVATE *************************************/
    
    private static Notification n;
    private Subscription playingItemMonitoring;
    
    private void songChange(Metadata newI) {
        if (showSongNotification)
            showNotification(newI , "New Song", SONG);
    }
    
    private void playbackStatusChange(Status newS) {
        if (!showStatusNotification || newS == null) return;
        
        Metadata m = Player.playingtem.get();
        String text = "Playback change : " + PLAYBACK.getStatus().toString();
        showNotification(m, text, PLAYBACK_STATUS);
    }
    
    private void showNotification(Object content, String title, NotificationType type) {
        if (showNotification) {
            // build content
            n.setContent(content, title, type);
            // set properties (that could have changed from last time)
            n.setAutoHide(notifAutohide);
            n.setAnimated(notifAnimated);
            n.setAnimDuration(Duration.millis(notifFadeTime));
            n.setDuration(Duration.millis(notificationDuration));
            n.setOnClickL(onClickL.getValueAction());
            n.setOnClickR(onClickR.getValueAction());
            // show
            n.show(notifPos);
        }
    }
    
    private void hideNotification() {
        if(!isRunning()) throw new IllegalStateException("Notification service not running");
        n.hide();
    }
    
}