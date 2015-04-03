
package Layout.Areas;

import GUI.DragUtil;
import GUI.GUI;
import static GUI.GUI.openAndDo;
import Layout.Component;
import Layout.Container;
import Layout.Widgets.Widget;
import java.io.IOException;
import static java.util.Collections.singletonList;
import java.util.List;
import static java.util.Objects.requireNonNull;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import static util.Util.setAnchors;

/**
 * Implementation of Area for UniContainer.
 */
public final class WidgetArea extends Area<Container> {
    
    @FXML private AnchorPane content;
    @FXML public StackPane content_padding;
    
    private Widget widget = Widget.EMPTY();     // never null
    
    /**
     @param c container to make contract with
     @param i index of the child within the container
     */
    public WidgetArea(Container c, int i) {
        super(c,i);
        
        // load graphics
        try {
            FXMLLoader loader = new FXMLLoader(this.getClass().getResource("WidgetArea.fxml"));
                       loader.setRoot(content_root);
                       loader.setController(this);
                       loader.load();
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage());
        }
        // load controls
        controls = new AreaControls(this);
        content_padding.getChildren().addAll(controls.root);
        setAnchors(controls.root, 0d);
        
        // support css styling - 
//        content.getStyleClass().setAll(Area.bgr_STYLECLASS);
        
        // support drag from - done in controls: AreaControls.class
        // return graphics to normal
        root.setOnDragDone( e -> content.pseudoClassStateChanged(draggedPSEUDOCLASS, false));
        // accept drag onto
        root.setOnDragOver(DragUtil.componentDragAcceptHandler);
        // handle drag onto
        root.setOnDragDropped( e -> {
            if (DragUtil.hasComponent()) {
                container.swapChildren(index,DragUtil.getComponent());
                e.setDropCompleted(true);
                e.consume();
            }
        });
       
        if(GUI.isLayoutMode()) show(); else hide();
    }
    
    /** @return currently active widget. */
    public Widget getWidget() {
        return widget;
    }
    /**
     * This implementation returns widget of this area.
     */
    @Override
    public Widget getActiveWidget() {
        return widget;
    }
    
    /**
     * This implementation returns widget of this area.
     * @return singleton list of this area's only widget. Never null. Never
 containsKey null.
     */
    @Override
    public List<Widget> getActiveWidgets() {
        return singletonList(widget);
    }
    
    public void loadWidget(Widget w) {
        requireNonNull(w,"widget must not be null");
        
        widget = w;
        
        // load widget
        Node wNode = w.load();
        content.getChildren().clear();
        content.getChildren().add(wNode);
        setAnchors(wNode,0);
        openAndDo(content_root, null);
        
        // put controls to new widget
        controls.title.setText(w.name());
        controls.propB.setDisable(w.getFields().isEmpty());
        
        // put container properties (just in case)
        setPadding(container.properties.getD("padding"));
        
        // put up activity node
        Node an = w.getController().getActivityNode();
        if(an!=null) {
            an.setUserData(this);
            setActivityContent(an);
            setActivityVisible(false);
        }
        wNode.setUserData(this);
        
    }
    
    @Override
    public void refresh() {
        widget.getController().refresh();
    }

    @Override
    public void add(Component c) {
        container.addChild(index, c);
    }
    
    @Override
    public AnchorPane getContent() {
        return content;
    }

    @Override
    public void close() {}
}