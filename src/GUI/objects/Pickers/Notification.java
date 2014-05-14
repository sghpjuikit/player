
package GUI.objects.Pickers;

import AudioPlayer.tagging.Metadata;
import Configuration.ConfigManager;
import Configuration.Configuration;
import GUI.WindowBase;
import GUI.objects.PopOver.PopOver;
import GUI.objects.PopOver.PopOver.ScreenCentricPos;
import GUI.objects.Thumbnail;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import main.App;
import utilities.Log;

/**
 *
 * @author uranium
 */
public class Notification {
    private final PopOver THIS = new PopOver();
    // content
    private final AnchorPane songNotif = new AnchorPane();
    private final AnchorPane textNotif = new AnchorPane();
    private Timer closer;
    Thumbnail t;
    
    @FXML private Label indexL;
    @FXML private Label songL;
    @FXML private Label artistL;
    @FXML private Label albumL;
    @FXML private Label typeL;
    @FXML private AnchorPane coverContainer;
    
    @FXML private BorderPane textContainer;
    @FXML private Label titleText;
    
    public Notification() {
        THIS.setDetachedTitle("");
        THIS.setDetached(false);
        THIS.setDetachable(false);
        THIS.setHideOnEscape(false);
        THIS.setArrowSize(0);
        THIS.setArrowIndent(0);
        THIS.setCornerRadius(0);
        THIS.setAutoFix(false);
        
        buildContent();
    }

    private void buildContent() {
        try {
            //load
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Notification.fxml"));
            fxmlLoader.setRoot(songNotif);
            fxmlLoader.setController(this);
            fxmlLoader.load();
            
            // close on click
            songNotif.setOnMouseClicked( e -> {
                WindowBase w = App.getInstance().getWindow();
                if (Configuration.notifclickOpenApp && w.isMinimized())
                        w.setMinimized(false);
                else
                    if (Configuration.notifCloseOnClick)
                        hide();
            });
        } catch (IOException ex) {
            Log.err("Notifier source data coudlnt be read.");
        }
        
        try {
            //load
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("NotificationText.fxml"));
            fxmlLoader.setRoot(textNotif);
            fxmlLoader.setController(this);
            fxmlLoader.load();
            
            // close on click
            textNotif.setOnMouseClicked( e-> {
                WindowBase w = App.getInstance().getWindow();
                if (Configuration.notifclickOpenApp && w.isMinimized())
                        w.setMinimized(false);
                else
                    if (Configuration.notifCloseOnClick)
                        hide();
            });
        } catch (IOException ex) {
            Log.err("Notifier source data coudlnt be read.");
        }
        
        t = new Thumbnail(coverContainer.getPrefHeight());
        coverContainer.getChildren().add(t.getPane());
    }
    
    public void show(ScreenCentricPos pos) {
        THIS.setAutoHide(Configuration.notifCloseOnClickAny);
        THIS.setAnimated(Configuration.notificationAnimated);
        THIS.setAnimDuration(Duration.millis(Configuration.notifFadeTime));
        
        THIS.show(pos);
        
        if (closer != null) closer.cancel();
        closer = new Timer(true);
        closer.schedule( new TimerTask() {
            @Override public void run() {
                Platform.runLater(() -> hide() );
            }
        }, (long) Configuration.notificationDuration);
    }

    public void hide() {
        closer.cancel();
        THIS.setAnimDuration(Duration.millis(Configuration.notificationDuration));
        THIS.hide();
    }
    
    /**
     * Sets content of the notifier using data from provided metadata.
     * @param content of the notification. Content type depends on type parameter
     * and is not arbitrary. If type is PLAYBACK or SONG, content must be Metadata.
     * For TEXT it must be String and for OTHER Node is required.
     * @param type 
     * @param title 
     * @throws NullPointerException if param null
     * @throws ClassCastException if 
     * content not Metadata if type PLAYBACK or SONG
     * content not Node if type OTHER
     * content not String if type TEXT
     */
    public void setContent(Object content, String title, NotificationType type) {
        typeL.setText(title);
        
        if (type == NotificationType.Song || type == NotificationType.Playback) {
            
            Metadata m = (Metadata)content;
            if (m == null) { // prevent displaying previous info
                t.loadImage((Image)null);
                indexL.setText("");
                songL.setText("");
                artistL.setText("");
                albumL.setText("");
            } else {
                t.loadImage(m.getCoverFromAnySource());
                indexL.setText(m.getPlaylistIndexInfo());
                songL.setText(m.getTitle());
                artistL.setText(m.getArtistOrAlbumArist());
                albumL.setText(m.getAlbum());
            }
            THIS.setContentNode(songNotif);
        } else
        if (type == NotificationType.Other) {
            THIS.setContentNode((Node)content);
        } else
        if (type == NotificationType.Text) {
//            BorderPane c = new BorderPane();
//                       c.setPadding(new Insets(10));
//                       c.setCenter(new Text((String)content));
//            THIS.setContentNode(c);
            titleText.setText(title);
            Label txt = new Label((String)content);
                  txt.setWrapText(true);
//            textContainer.setCenter(new Text((String)content));
            textContainer.setCenter(txt);
            THIS.setContentNode(textNotif);
        }
    }
    

    
    public enum NotificationType {
        Playback,
        Song,
        Text,
        Other;
    }
}