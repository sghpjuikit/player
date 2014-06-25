
package GUI;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
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

/**
 * 
 * @author uranium
 */
@IsConfigurable(group = "Notification")
public final class NotifierManager {
    
    @IsConfig(name = "Show notifications.", info = "Turn notification on and off completely")
    public static boolean showNotification = true;
    @IsConfig(name = "Show notifications about playback status")
    public static boolean showStatusNotification = true;
    @IsConfig(name = "Show notifications about playing item")
    public static boolean showSongNotification = true;
    @IsConfig(name = "Notification autohide delay", info = "Time it takes for the notification to hide on its own")
    public static double notificationDuration = 2500;
    @IsConfig(name = "Animate notifications", info = "Use animations on the notification")
    public static boolean notifAnimated = true;
    @IsConfig(name = "Fade in/out animation duration for notification")
    public static double notifFadeTime = 500;
    @IsConfig(name = "Close notification when clicked anywhere.")
    public static boolean notifAutohide = true;
    @IsConfig(name = "Close notification when clicked.")
    public static boolean notifCloseOnClick = true;
    @IsConfig(name = "Show application on notification click.")
    public static boolean notifclickOpenApp = true;
    @IsConfig(name = "Notification position.")
    public static PopOver.ScreenCentricPos notifPos = PopOver.ScreenCentricPos.ScreenBottomRight;
    
/******************************************************************************/
    
    private static Notification n;

    /**
     * Sets up application notification behavior.
     */
    public static void initialize() {
        // create notification
        n = new Notification();
        
        // show notification on playback status change
        PLAYBACK.statusProperty().addListener((observable, oldV, newV) -> {
            if (newV == PAUSED || newV ==PLAYING || newV == STOPPED)
                playbackStatusChange(oldV,newV);
        });
        
        // show notification on song change
        Player.addOnItemChange(NotifierManager::songChange);
    }
    
    /** Show notification for custom content. */
    public static void showNotification(Node content, String title) {
        NotifierManager.showNotification(content, title, OTHER);
    }
    /** Show notification for text. */
    public static void showTextNotification(String text, String title) {
        NotifierManager.showNotification(text, title, TEXT);
    }
    
/******************************************************************************/
    
    private static void songChange(Metadata oldI, Metadata newI) {
        if (showSongNotification)
            NotifierManager.showNotification(newI , "New Song", SONG);
    }
    
    private static void playbackStatusChange(Status oldS, Status newS) {
        if (!showStatusNotification || newS == null) return;
        
        Metadata m = Player.getCurrentMetadata();
        String text = "Playback change : " + PLAYBACK.getStatus().toString();
        NotifierManager.showNotification(m, text, PLAYBACK_STATUS);
    }
    
    /**
     * @param content
     * @param title Description of the event. Example: "New Playing".
     */
    private static void showNotification(Object content, String title, NotificationType type) {
        if (showNotification) {
            // build content
            n.setContent(content, title, type);
            // set properties (that could have changed from last time)
            // avoid setting properties when not needed
            if(n.isHideOnClick() != notifCloseOnClick) n.setHideOnClick(notifCloseOnClick);
            if(n.isAutoHide() != notifAutohide) n.setAutoHide(notifAutohide);
            if(n.isAnimated() != notifAnimated) n.setAnimated(notifAnimated);
            n.setAnimDuration(Duration.millis(notifFadeTime));
            n.setDuration(Duration.millis(notificationDuration));
            // show
            n.show(notifPos);
        }
    }
    
    public static void free() {
        if(n!=null) {
            n.hideImmediatelly();
            n = null;
        }
    }
}
