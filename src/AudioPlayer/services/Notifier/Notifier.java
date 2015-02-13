
package AudioPlayer.services.Notifier;

import Action.IsAction;
import Action.IsActionable;
import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.services.Service;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.objects.PopOver.Notification;
import GUI.objects.PopOver.Notification.NotificationType;
import static GUI.objects.PopOver.Notification.NotificationType.*;
import GUI.objects.PopOver.PopOver;
import static GUI.objects.PopOver.PopOver.ScreenCentricPos.ScreenTopRight;
import javafx.scene.Node;
import javafx.scene.media.MediaPlayer.Status;
import static javafx.scene.media.MediaPlayer.Status.*;
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
public final class Notifier implements Service {
    
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
    public static PopOver.ScreenCentricPos notifPos = ScreenTopRight;
    @IsConfig(name = "On Left Click.")
    public static final AccessorAction onClickL = new AccessorAction("Show application", null);
    @IsConfig(name = "On Right Click.")
    public static final AccessorAction onClickR = new AccessorAction("Notification hide", null);
    
    @IsAction(name = "Notification hide")
    public static void notifHide() {
        App.use(Notifier.class, Notifier::hideNotification);
    }
    @IsAction(name = "Notify now playing", description = "Shows notification about currently playing song.", global = true, shortcut = "ALT + N")
    public static void notifNowPlaying() {
        App.use(Notifier.class, nm -> nm.playbackChange(PLAYBACK.getStatus()));
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
                playbackChange(nv);
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
    
    private void playbackChange(Status newS) {
        if (!showStatusNotification || newS == null) return;
        
        Metadata m = Player.playingtem.get();
        String text = "Playback change : " + PLAYBACK.getStatus();
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
        n.hide();
    }
    
}