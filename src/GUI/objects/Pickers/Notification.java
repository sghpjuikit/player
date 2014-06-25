
package GUI.objects.Pickers;

import AudioPlayer.tagging.Metadata;
import GUI.Window;
import static GUI.objects.Pickers.Notification.NotificationType.OTHER;
import static GUI.objects.Pickers.Notification.NotificationType.PLAYBACK_STATUS;
import static GUI.objects.Pickers.Notification.NotificationType.SONG;
import static GUI.objects.Pickers.Notification.NotificationType.TEXT;
import GUI.objects.PopOver.PopOver;
import GUI.objects.PopOver.PopOver.ScreenCentricPos;
import GUI.objects.Text;
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
public class Notification extends PopOver{
        
    private final AnchorPane songNotif = new AnchorPane();  // song content
    private final AnchorPane textNotif = new AnchorPane();  // text content
    private final FxTimer closer = FxTimer.create(Duration.seconds(1), this::hide);// close delay timer
    
    // properties   
    private boolean openAppOnClick = true;
    private Duration duration = Duration.seconds(5);
    
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
        if (isOpenAppOnClick()) {
             if(w.isMinimized()) w.setMinimized(false);
             else w.focus();
        }
    };
    
    public Notification() {
        setTitle("");
        setDetached(false);
        setDetachable(false);
        setHideOnEscape(false);
        setArrowSize(0);
        setArrowIndent(0);
        setCornerRadius(0);
        setAutoFix(false);
        setAutoHide(true);
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
    
    @Override
    public void show(ScreenCentricPos pos) {
        super.show(pos);
        // start delayed hide
        if (closer != null) closer.stop();
        closer.restart(duration);
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
            setContentNode(songNotif);
            // call relayout
            songNotif.applyCss();
            songNotif.layout();
            songNotif.autosize();
        } else
        if (type == OTHER) {
            setContentNode((Node)content);
        } else
        if (type == TEXT) {
            String text = (String)content;
            // set roughly dynamical wrapping width to keep the text nore
            // roughly rectangular - with slight bias horizontal bias
            // this has surprisingly great result
            Text message = new Text(text);
                 message.setWrappingWidthNaturally();
            titleText.setText(title);
            textContainer.setCenter(message);
//            textContainer.setPadding(Insets.EMPTY);
            setContentNode(textNotif);
            // call relayout (dont remove)
            textNotif.applyCss();
            textNotif.layout();
            textNotif.autosize();
        }
        
    }

    /**
     * Returns time this notification will remain visible. Default 5 seconds.
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * Sets time this notification will remain visible.
     */
    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    /**
     * Returns whether click on this notification opens application. Default is
     * true.
     */
    public boolean isOpenAppOnClick() {
        return openAppOnClick;
    }

    /**
     * Sets whether click on this notification opens application
     */
    public void setOpenAppOnClick(boolean openAppOnClick) {
        this.openAppOnClick = openAppOnClick;
    }

    
    public enum NotificationType {
        PLAYBACK_STATUS,
        SONG,
        TEXT,
        OTHER;
    }
}