
package GUI;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.tagging.Metadata;
import GUI.objects.Pickers.Notification;
import GUI.objects.Pickers.Notification.NotificationType;
import static GUI.objects.Pickers.Notification.NotificationType.OTHER;
import static GUI.objects.Pickers.Notification.NotificationType.PLAYBACK_STATUS;
import static GUI.objects.Pickers.Notification.NotificationType.SONG;
import static GUI.objects.Pickers.Notification.NotificationType.TEXT;
import javafx.scene.Node;
import javafx.scene.media.MediaPlayer.Status;
import static javafx.scene.media.MediaPlayer.Status.PAUSED;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.scene.media.MediaPlayer.Status.STOPPED;

/**
 * @author uranium
 */
public final class NotifierManager {
    private static Notification n;

    /**
     * Sets up application notification behavior.
     * Dont invoke. Invoked Automatically. */
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
        if (Notification.showSongNotification)
            NotifierManager.showNotification(newI , "New Song", SONG);
    }
    
    private static void playbackStatusChange(Status oldS, Status newS) {
        if (!Notification.showStatusNotification || newS == null) return;
        
        Metadata m = Player.getCurrentMetadata();
        String text = "Playback change : " + PLAYBACK.getStatus().toString();
        NotifierManager.showNotification(m, text, PLAYBACK_STATUS);
    }
    
    /**
     * @param content
     * @param title Description of the event. Example: "New Playing".
     */
    private static void showNotification(Object content, String title, NotificationType type) {
        if (Notification.showNotification) {
            n.setContent(content, title, type);
            n.show(Notification.notifPos);
        }
    }
    public static void free() {
        if(n!=null) {
            n.hideImmediatelly();
            n = null;
        }
    }
}
