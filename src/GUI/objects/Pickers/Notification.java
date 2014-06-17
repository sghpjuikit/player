
package GUI.objects.Pickers;

import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.Window;
import static GUI.objects.Pickers.Notification.NotificationType.OTHER;
import static GUI.objects.Pickers.Notification.NotificationType.PLAYBACK_STATUS;
import static GUI.objects.Pickers.Notification.NotificationType.SONG;
import static GUI.objects.Pickers.Notification.NotificationType.TEXT;
import GUI.objects.PopOver.PopOver;
import GUI.objects.PopOver.PopOver.ScreenCentricPos;
import GUI.objects.Thumbnail;
import java.io.IOException;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import main.App;
import utilities.FxTimer;
import utilities.Log;

/**
 *
 * @author uranium
 */
@IsConfigurable(group = "Notification")
public class Notification {
    
    @IsConfig(info = "show notifications")
    public static boolean showNotification = true;
    @IsConfig(info = "show notifications about playback status")
    public static boolean showStatusNotification = true;
    @IsConfig(info = "show notifications about playing item")
    public static boolean showSongNotification = true;
    @IsConfig(info = "time for notification to autohide")
    public static double notificationDuration = 2500;
    @IsConfig(info = "Fade in/out notifications")
    public static boolean notificationAnimated = true;
    @IsConfig(info = "Fade in/out time for notification")
    public static double notifFadeTime = 500;
    @IsConfig(info = "Closes notification when clicked anywhere.")
    public static boolean notifCloseOnClickAny = true;
    @IsConfig(info = "Closes notification when clicked on it.")
    public static boolean notifCloseOnClick = true;
    @IsConfig(info = "Deminimize application on notification click when minimized.")
    public static boolean notifclickOpenApp = true;
    @IsConfig(info = "Position of notification.")
    public static ScreenCentricPos notifPos = ScreenCentricPos.ScreenBottomRight;
    
    private final PopOver root = new PopOver();             // popup
    private final AnchorPane songNotif = new AnchorPane();  // song content
    private final AnchorPane textNotif = new AnchorPane();  // txet content
    private final FxTimer closer = FxTimer.create(          // close delay timer
                            Duration.millis(notificationDuration), this::hide);
    // content 
    Thumbnail t;
    @FXML private Label indexL;
    @FXML private Label songL;
    @FXML private Label artistL;
    @FXML private Label albumL;
    @FXML private Label typeL;
    @FXML private AnchorPane coverContainer;
    
    @FXML private BorderPane textContainer;
    @FXML private Label titleText;
    
    private final EventHandler onClickHandler = e-> {
        Window w = App.getWindow();
        // if app minimized deminimize
        if (notifclickOpenApp) {
             if(w.isMinimized()) w.setMinimized(false);
             else w.focus();
        }
        // close notification if set
        if (notifCloseOnClick) hide();
    };
    
    public Notification() {
        root.setDetachedTitle("");
        root.setDetached(false);
        root.setDetachable(false);
        root.setHideOnEscape(false);
        root.setArrowSize(0);
        root.setArrowIndent(0);
        root.setCornerRadius(0);
        root.setAutoFix(false);
        
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
            songNotif.setOnMouseClicked(onClickHandler);
            
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
            textNotif.setOnMouseClicked(onClickHandler);
        } catch (IOException ex) {
            Log.err("Notifier source data coudlnt be read.");
        }
        
        t = new Thumbnail(coverContainer.getPrefHeight());
        coverContainer.getChildren().add(t.getPane());
    }
    
    public void show(ScreenCentricPos pos) {
        // set properties
        root.setAutoHide(notifCloseOnClickAny);
        root.setAnimated(notificationAnimated);
        root.setAnimDuration(Duration.millis(notifFadeTime));
        // show
        root.show(pos);
        // start delayed hide
        if (closer != null) closer.stop();
        closer.restart(Duration.millis(notificationDuration));
    }

    public void hide() {
        // set properties
        root.setAutoHide(notifCloseOnClickAny);
        root.setAnimated(notificationAnimated);
        root.setAnimDuration(Duration.millis(notifFadeTime));
        // hide
        root.hide();
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
        
        if (type == SONG || type == PLAYBACK_STATUS) {
            
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
            root.setContentNode(songNotif);
        } else
        if (type == OTHER) {
            root.setContentNode((Node)content);
        } else
        if (type == TEXT) {
//            BorderPane c = new BorderPane();
//                       c.setPadding(new Insets(10));
//                       c.setCenter(new Text((String)content));
//            THIS.setContentNode(c);
            titleText.setText(title);
            Label txt = new Label((String)content);
                  txt.setWrapText(true);
//            textContainer.setCenter(new Text((String)content));
            textContainer.setCenter(txt);
            root.setContentNode(textNotif);
        }
    }
    

    
    public enum NotificationType {
        PLAYBACK_STATUS,
        SONG,
        TEXT,
        OTHER;
    }
}