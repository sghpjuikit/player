
package Layout.Areas;

import GUI.DragUtil;
import GUI.WidgetTransfer;
import Layout.Component;
import Layout.UniContainer;
import Layout.Widgets.Widget;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import static javafx.scene.input.MouseButton.PRIMARY;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;

/**
 * Parametrized implementation of Area for UniContainer.
 */
public final class WidgetArea extends UniArea {
    
    @FXML private AnchorPane content;
    
    private Widget widget = Widget.EMPTY();     // never null
    
    public WidgetArea(UniContainer con) {
        super(con);
        // load graphics
        try {
            FXMLLoader loader = new FXMLLoader(this.getClass().getResource("WidgetArea.fxml"));
                       loader.setRoot(root);
                       loader.setController(this);
                       loader.load();
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage());
        }
        // load controls
        controls = new AreaControls(this);
        root.getChildren().add(controls.root);
        AnchorPane.setBottomAnchor(controls.root, 0.0);
        AnchorPane.setTopAnchor(controls.root, 0.0);
        AnchorPane.setLeftAnchor(controls.root, 0.0);
        AnchorPane.setRightAnchor(controls.root, 0.0);
        
        // support drag from
        root.setOnDragDetected( e -> {
            if (!controls.isShowing()) return;  // disallow in normal mode
            if (e.getButton()==PRIMARY) {       // primary button drag only
                ClipboardContent c = new ClipboardContent();
                c.put(DragUtil.widgetDF, new WidgetTransfer(widget, container));
                Dragboard db = root.startDragAndDrop(TransferMode.ANY);
                          db.setContent(c);
                e.consume();
            }
        });
        // support drag onto
        root.setOnDragOver( e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(DragUtil.widgetDF))
                e.acceptTransferModes(TransferMode.ANY);
            e.consume();
        });
        root.setOnDragDropped( e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(DragUtil.widgetDF)) {
                WidgetTransfer wt = (WidgetTransfer) db.getContent(DragUtil.widgetDF);
                container.swapChildren(widget, wt.getContainer(),wt.getWidget());
            }
            e.consume();
        });
        
        controls.hide();
    }
    
    /** @return currently active widget. */
    public Widget getWidget() {
        return widget;
    }
    /**
     * This implementation returns widget of this area.
     */
    @Override
    public Component getActiveComponent() {
        return widget;
    }
    
    /**
     * This implementation returns widget of this area.
     * @return singleton list of this area's only widget. Never null. Never
     * contains null.
     */
    @Override
    public List<Component> getActiveComponents() {
        return Collections.singletonList(widget);
    }
    
    public void loadWidget(Widget w) {
        Objects.requireNonNull(w,"widget must not be null");
        
        widget = w;
        
//        if(widget.isEmpty()) content.getStyleClass().setAll("darker");
//        else 
        content.getStyleClass().setAll(Area.bgr_STYLECLASS);
        
        // load widget
        AnchorPane widgetPane;
        widgetPane = (AnchorPane) w.load();
        content.getChildren().clear();
        content.getChildren().add(widgetPane);
        AnchorPane.setBottomAnchor(widgetPane, 0.0);
        AnchorPane.setLeftAnchor(widgetPane, 0.0);
        AnchorPane.setRightAnchor(widgetPane, 0.0);
        AnchorPane.setTopAnchor(widgetPane, 0.0);
        
        // set controls to new widget
        controls.title.setText(w.getName());                // set title
        controls.propB.setDisable(w.getFields().isEmpty()); // disable properties button if empty settings
        
        // set container properties (just in case)
        setPadding(container.properties.getD("padding"));
        setLocked(container.properties.getB("locked"));
    }

    @Override
    public void refresh() {
        widget.getController().refresh();
    }

    @Override
    public void add(Component c) {
        container.setChild(c);
    }
    
    @Override
    public AnchorPane getContent() {
        return content;
    }
}