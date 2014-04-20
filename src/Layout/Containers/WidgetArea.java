
package Layout.Containers;

import GUI.ContextManager;
import GUI.DragUtil;
import GUI.GUI;
import GUI.WidgetTransfer;
import GUI.Window;
import Layout.UniContainer;
import Layout.Widgets.Widget;
import java.io.IOException;
import java.util.Objects;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.BoxBlur;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import utilities.Log;

/**
 * Parametrized implementation of Area for UniContainer.
 */
public final class WidgetArea extends UniArea {

    @FXML private AnchorPane content;           // pane widgets will get paste onto
    @FXML private AnchorPane controls;
    @FXML private Label title;
    @FXML private Button menuB;
    @FXML private Button propB;
    @FXML private Region activator;
    @FXML private Region deactivator;
    
    private Widget widget = Widget.EMPTY();
    private AnchorPane widgetPane = null;
    
    // animations (initializing here will cause them malfunction)
    private FadeTransition contrAnim;
    private FadeTransition contAnim;
    private TranslateTransition blurAnim;
    
    public WidgetArea(UniContainer con) {
        super(con);
                
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("WidgetArea.fxml"));
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            Log.err("Widget Container failed to load. " + e.getMessage());
        }
        
        contrAnim = new FadeTransition(Duration.millis(GUI.duration_LM), controls);
        contAnim = new FadeTransition(Duration.millis(GUI.duration_LM), content);
        blurAnim = new TranslateTransition(Duration.millis(GUI.duration_LM), content);
  
        BoxBlur blur = new BoxBlur(0, 0, 1);
        blur.widthProperty().bind(content.translateZProperty());
        blur.heightProperty().bind(content.translateZProperty());
        content.translateZProperty().addListener( o -> {
            content.setEffect(blur);
        });
        activator.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
            if (!layoutMode.isAlt() && !layoutMode.isLocked())
                layoutMode.in();
        });
        controls.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            if (!layoutMode.isAlt())
                layoutMode.out();
        });
        
        root.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            if (!layoutMode.isAlt())
                layoutMode.out();
        });
        
        // support drag from
        root.setOnDragDetected( e -> {
            if (!layoutMode.isAlt()) return;              // disallow in normal mode
            if (e.getButton()==MouseButton.PRIMARY) {       // primary button drag only
                ClipboardContent c = new ClipboardContent();
                c.put(DragUtil.widgetDataFormat, new WidgetTransfer(widget, container));
                Dragboard db = root.startDragAndDrop(TransferMode.ANY);
                          db.setContent(c);
                e.consume();
            }
        });
        // support drag onto
        root.setOnDragOver( e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(DragUtil.widgetDataFormat))
                e.acceptTransferModes(TransferMode.ANY);
            e.consume();
        });
        root.setOnDragDropped( e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(DragUtil.widgetDataFormat)) {
                WidgetTransfer wt = (WidgetTransfer) db.getContent(DragUtil.widgetDataFormat);
                container.swapChildren(widget, wt.getContainer(),wt.getWidget());
            }
            e.consume();
        });
        

        
        hide();
    }
    
    /**
     * @return currently active widget.
     */
    public Widget getWidget() {
        return widget;
    }
    
    public void loadWidget(Widget w) {
        Objects.requireNonNull(w,"widget must not be null");
        
        widget = w;
        
        content.getChildren().remove(widgetPane);
        widgetPane = (AnchorPane) w.load();
        content.getChildren().add(widgetPane);
        AnchorPane.setBottomAnchor(widgetPane, 0.0);
        AnchorPane.setLeftAnchor(widgetPane, 0.0);
        AnchorPane.setRightAnchor(widgetPane, 0.0);
        AnchorPane.setTopAnchor(widgetPane, 0.0);
        
        title.setText(w.getName());
        
        // disable properties button if empty settings
        propB.setDisable(w.getFields().isEmpty());
        
        // set padding
        setPadding(container.properties.getD("padding"));
    }

    @Override
    public void refreshWidget() {
        widget.getController().refresh();
    }
        
    public AnchorPane getPane() {
        return root;
    }
    
    public void settings() {
        ContextManager.openFloatingSettingsWindow(widget);
    }
    
    public void choose() {
        ContextManager.showMenu(ContextManager.widgetsMenu, menuB, container);
    }
    
    public void detach() {     
        Window w = ContextManager.openFloatingWindow(Widget.EMPTY());
               w.setSize(root.getWidth(), root.getHeight());
        container.swapChildren(widget, w.getLayout(), w.getLayout().getChild());
    }
    
    @Override
    protected void showControls() {
        contrAnim.stop();
        contAnim.stop();
        blurAnim.stop();
        contrAnim.setToValue(1);
        contAnim.setToValue(1-GUI.opacity_LM);
        blurAnim.setToZ(GUI.blur_LM);
        contrAnim.play();
        if(GUI.opacity_layoutMode)
            contAnim.play();
        if(GUI.blur_layoutMode)
            blurAnim.play();
        activator.toBack();
        content.setMouseTransparent(true);
        controls.setMouseTransparent(false);
        deactivator.setMouseTransparent(false);
    }
    @Override
    protected void hideControls() {
        contrAnim.stop();
        contAnim.stop();
        blurAnim.stop();
        contrAnim.setToValue(0);
        contAnim.setToValue(1);
        blurAnim.setToZ(0);
        contrAnim.play();
        if(GUI.opacity_layoutMode)
            contAnim.play();
        if(GUI.blur_layoutMode)
            blurAnim.play();
        activator.toFront();
        content.setMouseTransparent(false);
        controls.setMouseTransparent(true);
        deactivator.setMouseTransparent(true);
    }
}