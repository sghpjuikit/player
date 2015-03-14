
package GUI.objects.PopOver;

import GUI.objects.PopOver.PopOver.ScreenCentricPos;
import javafx.scene.Node;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import util.async.FxTimer;

/**
 * Notification popover.
 */
public class Notification extends PopOver {
        
    private final FxTimer closer = new FxTimer(5000, 1, this::hide);
    private final StackPane root = new StackPane();
    
    /** Executes on left mouse click. Default null. */
    public Runnable lClickAction = null;
    /** Executes on right mouse click. Default null. */
    public Runnable rClickAction = null;
    
    public Notification() {
        detached.set(false);
        detachable.set(false);
        setHideOnEscape(false);
        setArrowSize(0);
        setArrowIndent(0);
        setCornerRadius(0);
        setAutoFix(false);
        setAutoHide(false);
        getSkinn().setTitleAsOnlyHeaderContent();
        getSkinn().root.getStyleClass().add("notification");
        
        setContentNode(root);
        root.setOnMouseClicked(e -> {
            if(e.getButton()==PRIMARY && lClickAction!=null ) lClickAction.run();
            if(e.getButton()==SECONDARY && rClickAction!=null ) rClickAction.run();
        });
        root.setOnMouseEntered(e-> closer.pause());
        root.setOnMouseExited(e-> closer.unpause());
    }
    
    @Override
    public void show(ScreenCentricPos pos) {
        super.show(pos);
        closer.restart();
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
    public void setContent(Node n, String title) {
        headerVisible.set(!title.isEmpty());
        this.title.set(title);
        n.setMouseTransparent(true);
        setContentNode(null);
        root.getChildren().setAll(n);
        setContentNode(root);
    }
    

    /** Returns time this notification will remain visible. Default 5 seconds. */
    public Duration getDuration() {
        return closer.getPeriod();
    }

    /** Sets time this notification will remain visible. */
    public void setDuration(Duration duration) {
        closer.setPeriod(duration);
    }
}