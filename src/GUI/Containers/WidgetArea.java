
package GUI.Containers;

import GUI.ContextManager;
import GUI.DragUtil;
import GUI.GUI;
import GUI.WidgetTransfer;
import GUI.Window;
import Layout.AltState;
import Layout.AltStateHandler;
import Layout.Container;
import Layout.UniContainer;
import Layout.Widget;
import java.io.IOException;
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
 *
 */
public final class WidgetArea implements AltState, ContainerArea  {
    
    private final AnchorPane root = new AnchorPane();
    @FXML private AnchorPane content;           // pane widgets will get paste onto
    @FXML private AnchorPane controls;
    @FXML private Label title;
    @FXML private Button menuB;
    @FXML private Button propB;
    @FXML private Region activator;
    @FXML private Region deactivator;
    
    private Widget widget;
    private AnchorPane widgetPane = null;
    private final UniContainer container;
    
    // animations (initializing here will cause them malfunction)
    private FadeTransition contrAnim;
    private FadeTransition contAnim;
    private TranslateTransition blurAnim;
    
    
    private final AltStateHandler stateHandler = new AltStateHandler(){
        @Override public void in() { showControls(); }
        @Override public void out() { hideControls(); }
    };
    
    public WidgetArea(UniContainer con) {
        container = con;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("WidgetArea.fxml"));
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            Log.err("Widget Container failed to load. " + e.getMessage());
        }
        
        title.setText("Empty");
        contrAnim = new FadeTransition(Duration.millis(GUI.duration_LM), controls);
        contAnim = new FadeTransition(Duration.millis(GUI.duration_LM), content);
        blurAnim = new TranslateTransition(Duration.millis(GUI.duration_LM), content);
  
        BoxBlur blur = new BoxBlur(0, 0, 1);
        content.translateZProperty().addListener( o -> {
            if(GUI.blur_layoutMode) {
                blur.setWidth(content.getTranslateZ());
                blur.setHeight(content.getTranslateZ());
                content.setEffect(blur);
            }
        });
        activator.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
            if (!stateHandler.isAlt() && !stateHandler.isLocked())
                stateHandler.in();
        });
        controls.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            if (!stateHandler.isAlt())
                stateHandler.out();
        });
        root.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            if (!stateHandler.isAlt())
                stateHandler.out();
        });
        
        // support drag from
        root.setOnDragDetected( e -> {
            if (!stateHandler.isAlt()) return;              // disallow in normal mode
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
    
    
    @Override
    public Container getContainer() {
        return container;
    }
    @Override
    public Widget getWidget() {
        return widget;
    }
    @Override
    public void loadWidget(Widget w) {
        if (w == null) { 
            title.setText("Empty");
            widget = w;
            return; 
        }
        
        content.getChildren().remove(widgetPane);
        widgetPane = (AnchorPane) w.load();
        content.getChildren().add(widgetPane);
        AnchorPane.setBottomAnchor(widgetPane, 0.0);
        AnchorPane.setLeftAnchor(widgetPane, 0.0);
        AnchorPane.setRightAnchor(widgetPane, 0.0);
        AnchorPane.setTopAnchor(widgetPane, 0.0);
        
        widgetPane.setVisible(true);
        title.setText(w.getName());
        
        // disable if empty settings
        propB.setDisable(w.getFields().isEmpty());
        
        widget = w;
    }



    @Override
    public void refreshWidget() {
        if (hasWidget())
            widget.getController().refresh();
    }
        
    public AnchorPane getPane() {
        return root;
    }
    
    public void settings() {
        if (hasWidget())
            ContextManager.openFloatingSettingsWindow(widget);
    }
    
    public void choose() {
        ContextManager.showMenu(ContextManager.widgetsMenu, menuB, container);
    }
    
    public void detach() {
        if (hasWidget()) {        
            Window w = ContextManager.openFloatingWindow(Widget.EMPTY());
                   w.setSize(root.getWidth(), root.getHeight());
            container.swapChildren(widget, w.getLayout(), w.getLayout().getChild());
        }
    }
    
    @Override
    public void close() {
        container.setChild(null);
        container.close();
    }
    
/******************************************************************************/
    
    @FXML
    @Override
    public void show() {
        stateHandler.in();
        stateHandler.setAlt(true);
    }
    @FXML
    @Override
    public void hide() {
        stateHandler.out();
        stateHandler.setAlt(false);
    }
    @FXML
    public void setLocked(boolean val) {
        stateHandler.setLocked(val);
    }
    @FXML
    public void toggleLocked() {
        if (stateHandler.isLocked())
            stateHandler.setLocked(false);
        else
            stateHandler.setLocked(true);
    }
    
    private void showControls() {
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
        deactivator.setMouseTransparent(false);
    }
    private void hideControls() {
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
        deactivator.setMouseTransparent(true);
    }
}