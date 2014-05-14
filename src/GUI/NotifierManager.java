
package GUI;

import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import Configuration.Configuration;
import GUI.objects.Pickers.Notification;
import GUI.objects.Pickers.Notification.NotificationType;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;

/**
 * @author uranium
 */
public final class NotifierManager {
    private final static Notification n = new Notification();

    public static void initialize() {
        
        PlaylistManager.playingItemProperty().addListener((ObservableValue<? extends PlaylistItem> ov, PlaylistItem t, PlaylistItem t1) -> {
            if (PlaylistManager.isItemPlaying())
                songChange();
        });
        PLAYBACK.statusProperty().addListener((ObservableValue<? extends Status> ov, Status t, Status t1) -> {
            MediaPlayer.Status stat = PLAYBACK.getStatus();
            if (stat == Status.PAUSED || stat ==Status.PLAYING || stat == Status.STOPPED)
                playbackStatusChange();
        });
    }
    
    private static void songChange() {
        if (Configuration.showSongNotification) {
            PlaylistItem newlyPlayed = PlaylistManager.getPlayingItem();
            Metadata m = newlyPlayed.getMetadata();
            NotifierManager.showNotification(m , "New Song", NotificationType.Song);
        }
    }
    
    private static void playbackStatusChange() {
        if (!Configuration.showStatusNotification)
            return;
        if (PLAYBACK.getStatus() == null)
            return;
        
        PlaylistItem playing = PlaylistManager.getPlayingItem();
        Metadata m = playing == null ? null : playing.getMetadata();
        NotifierManager.showNotification(m, "Playback change : " + PLAYBACK.getStatus().toString(),NotificationType.Playback);
    }
    
    public static void showNotification(Node content, String title) {
        NotifierManager.showNotification(content, title, NotificationType.Other);
    }
    public static void showTextNotification(String text, String title) {
        NotifierManager.showNotification(text, title, NotificationType.Text);
    }
    
    /**
     * @param content
     * @param title Description of the event. Example: "New Playing".
     */
    private static void showNotification(Object content, String title, Notification.NotificationType type) {
        if (Configuration.showNotification) {
            n.setContent(content, title, type);
            n.show(Configuration.notifPos);
        }
    }
}
