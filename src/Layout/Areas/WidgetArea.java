
package Layout.Areas;

import GUI.DragUtil;
import Layout.Component;
import Layout.UniContainer;
import Layout.Widgets.Widget;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.input.Dragboard;
import static javafx.scene.input.MouseButton.PRIMARY;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;

/**
 * Implementation of Area for UniContainer.
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
        
        // support css styling
        content.getStyleClass().setAll(Area.bgr_STYLECLASS);
        
        // support drag from
        root.setOnDragDetected( e -> {
            // disallow in normal mode & primary button drag only
            if (controls.isShowingWeak() && e.getButton()==PRIMARY) {
                Dragboard db = root.startDragAndDrop(TransferMode.ANY);
                DragUtil.setComponent(container,widget,db);
                e.consume();
            }
        });
        // accept drag onto
        root.setOnDragOver(DragUtil.componentDragAcceptHandler);
        // handle drag onto
        root.setOnDragDropped( e -> {
            if (DragUtil.hasComponent()) {
                container.swapChildren(1,DragUtil.getComponent());
                e.setDropCompleted(true);
                e.consume();
            }
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
        // feature awaits decisio - does this make sense? // no // yes
//        if(w.isEmpty()) content.getStyleClass().clear();
//        else content.getStyleClass().addAll(Area.bgr_STYLECLASS);
        // load widget
        Node wNode = w.load();
        content.getChildren().clear();
        content.getChildren().add(wNode);
        AnchorPane.setBottomAnchor(wNode, 0.0);
        AnchorPane.setLeftAnchor(wNode, 0.0);
        AnchorPane.setRightAnchor(wNode, 0.0);
        AnchorPane.setTopAnchor(wNode, 0.0);
        
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