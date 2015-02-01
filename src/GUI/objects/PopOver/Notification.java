
package GUI.objects.PopOver;

import AudioPlayer.tagging.Metadata;
import GUI.objects.ItemInfo;
import static GUI.objects.PopOver.Notification.NotificationType.OTHER;
import static GUI.objects.PopOver.Notification.NotificationType.PLAYBACK_STATUS;
import static GUI.objects.PopOver.Notification.NotificationType.SONG;
import static GUI.objects.PopOver.Notification.NotificationType.TEXT;
import GUI.objects.PopOver.PopOver;
import GUI.objects.PopOver.PopOver.ScreenCentricPos;
import GUI.objects.Text;
import java.io.IOException;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import util.TODO;
import util.async.FxTimer;

/**
 * Notification popover.
 */
@TODO(purpose = TODO.Purpose.PERFORMANCE_OPTIMIZATION, note = "use only one grahics")
public class Notification extends PopOver {
        
    private ItemInfo songNotif;
    private final AnchorPane textNotif = new AnchorPane();  // text content
    private final FxTimer closer = new FxTimer(1000, 1, this::hide);
    
    // content 
    @FXML private BorderPane textContainer;
    @FXML private Label titleText;
    
    public Notification() {
        setTitle("");
        setDetached(false);
        setDetachable(false);
        setHideOnEscape(false);
        setArrowSize(0);
        setArrowIndent(0);
        setCornerRadius(0);
        setAutoFix(false);
        setAutoHide(false);
        buildContent();
    }

    private void buildContent() {
        songNotif = new ItemInfo();
        
        try {
            //load
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("NotificationText.fxml"));
            fxmlLoader.setRoot(textNotif);
            fxmlLoader.setController(this);
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException("Notifier source data coudlnt be read.", e);
        }
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
        
        if (type == SONG || type == PLAYBACK_STATUS) {
            songNotif.setData(title, (Metadata)content);
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
                 message.setWrappingWidthNatural(true);
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
    
    private Duration duration = Duration.seconds(5);

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
    
    
    private EventHandler<MouseEvent> onClickLH = null;
    private EventHandler<MouseEvent> onClickRH = null;
    
    public void setOnClickL(Runnable onClickL) {
        if(onClickLH!=null) {
            songNotif.removeEventHandler(MOUSE_CLICKED, onClickLH);
            textNotif.removeEventHandler(MOUSE_CLICKED, onClickLH);
        }
        
        onClickLH = onClickL==null ? null : e -> {
            if(e.getButton()==PRIMARY) {
                onClickL.run();
                e.consume();
            }
        };
        
        if(onClickLH!=null) {
            songNotif.addEventHandler(MOUSE_CLICKED, onClickLH);
            textNotif.addEventHandler(MOUSE_CLICKED, onClickLH);
        }
    }
    public void setOnClickR(Runnable onClickR) {
        if(onClickRH!=null) {
            songNotif.removeEventHandler(MOUSE_CLICKED, onClickRH);
            textNotif.removeEventHandler(MOUSE_CLICKED, onClickRH);
        }
        
        onClickRH = onClickR==null ? null : e -> {
            if(e.getButton()==SECONDARY) {
                onClickR.run();
                e.consume();
            }
        };
        
        if(onClickRH!=null) {
            songNotif.addEventHandler(MOUSE_CLICKED, onClickRH);
            textNotif.addEventHandler(MOUSE_CLICKED, onClickRH);
        }
    }

    
    public enum NotificationType {
        PLAYBACK_STATUS,
        SONG,
        TEXT,
        OTHER;
    }
}