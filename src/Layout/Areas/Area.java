
package Layout.Areas;

import GUI.objects.Window.stage.ContextManager;
import GUI.objects.Window.stage.Window;
import Layout.Component;
import Layout.Container;
import Layout.Layout;
import Layout.Widgets.Widget;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.BoxBlur;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import util.Closable;
import static util.Util.setAnchors;

/**
 * Graphical part of the container within layout.
 * The container - area is 1:1 non null relationship. Container makes up for the
 * abstract side, this class represents the graphical side.
 * <p>
 * The lifecycle of the graphics entirely depends on the lifecycle of the
 * container. Instances of this class can not live outside of container's
 * life cycle. Note that the opposite doesnt necessarily hold true.
 * <p>
 * @author uranium
 */
public abstract class Area<T extends Container> implements ContainerNode, Closable {
    
    public static final PseudoClass draggedPSEUDOCLASS = PseudoClass.getPseudoClass("dragged");
    public static final List<String> bgr_STYLECLASS = Arrays.asList("area", "block");
    
    /** Container this are is associated with. The relationship can not be 
      changed. */
    public final T container;
    /** Index of the child, the area is for or null if for many. Decides whether
      the area belongs to the specific child, or whole container. */
    public final Integer index;
    public final AnchorPane content_root = new AnchorPane();
    /** The root of this area. */
    public final AnchorPane root = new AnchorPane();
    AreaControls controls;
    /** The root of activity content. ContainsKey custom content. */
    public final StackPane activityPane;
    
    /**
     * @param c container to make contract with
     * @param i index of the child, the area is for or null if for many
     */
    public Area(T c, Integer i) {
        // init final 1:1 container-area relationship
        Objects.requireNonNull(c);
        container = c;
        index = i;
        
        root.getChildren().addAll(content_root);
        setAnchors(content_root, 0d);
        
        // init properties
        c.properties.initProperty(Double.class, "padding", 0d);
        
        // init behavior
        root.addEventFilter(ScrollEvent.SCROLL,e -> {
            if(controls.isShowing()) {
                if(e.getDeltaY()<0) collapse();
                else if(e.getDeltaY()>0) expand();
                e.consume();
            }
        });
        root.setOnMouseClicked(e->{
            if(controls.isShowing())
                if(e.getButton()==MouseButton.MIDDLE) {
                    setPadding(0);
                    e.consume();
                }
        });
        
        // load controls
        activityPane = new StackPane();
        activityPane.setPickOnBounds(false);
        content_root.getChildren().add(activityPane);
        setAnchors(activityPane, 0d);
        activityPane.toFront();
    }
    
    /** @return all oomponents wrapped in the area. By default returns all
     * components of the container associated with the area.*/
    public final List<Component> getAllComponents() {
        return new ArrayList(container.getChildren().values());
    }
    
    /** @return all active coponents - components that are being actively 
     * displayed. */
    abstract public List<Widget> getActiveWidgets();
    
    /** @return the primary active component. */
    abstract public Widget getActiveWidget();
    
    /**
     * Refresh area. Refreshes the content - wrapped components by calling their
     * refresh() method. Some components might not support this behavior and
     * will do nothing.
     * <p>
     * Implementation decides which components need and will get refreshed.
     * <p>
     * Default implementation refreshes all active widgets (ignores containers).
     */
    public void refresh() {
        for(Component c: getActiveWidgets()) {
            if(c instanceof Widget) 
                ((Widget)c).getController().refresh();
        }
    }
    /**
     * Adds component to the area.
     * <p>
     * Implementation decides how exactly. Simple
     * implementation storing single component would remove the old component
     * from layout map and add new one to the parent container.
     * @param c 
     */
    abstract public void add(Component c);
    
    /**
     * Detaches the content into standalone content. Opens new window.
     * <p>
     * Implementation decides specifics of the operation. {@link PolyArea} could
     * detach only some components or itself or only active component.
     * <p>
     * Default implementation detaches the first active component. Does nothing
     * if no active component available.
     */
    public void detach() {
        // get first active component
        Component c = getActiveWidget();
        
        // detach into new window
        // create new window with no content (not even empty widget)
        Window w = ContextManager.showWindow(null);
               // put size to that of a source (also add jeader & border space)
               w.setSize(root.getWidth()+10, root.getHeight()+30);
        // change content
        Layout c2 = w.getLayoutAggregator().getActive();
        Component w2 = c2.getChild();
            // watch out indexOf returns null if param null, but that will not happen here
        int i1 = container.indexOf(c);
        container.swapChildren(c2,i1,w2);
    }
    
/******************************************************************************/
    
    /** Returns the content. */
    abstract public AnchorPane getContent();
    
    @Override
    public Pane getRoot() {
        return root;
    }
    
/******************************************************************************/
    
    public final void expand() {
        changePadding(-1);
    }
    public final void collapse() {
        changePadding(+1);
    }
    public final void changePadding(double by) {
        Insets pad = root.getPadding();
        double to = pad.getTop()+by;
        if(to<0) to = 0;
        else if(to>root.getWidth()/2) to = root.getWidth()/2;
        else if(to>root.getHeight()/2) to = root.getHeight()/2;
        
        setPadding(to);
    }
    public final void setPadding(double to) {
        // update properties if changed
        if(root.getPadding().getTop()!=to)
            container.properties.put("padding", to);
        root.setPadding(new Insets(to));
    }
    
/******************************* layout mode **********************************/
        
    @FXML
    @Override
    public void show() {
        controls.show();
    }
    @FXML
    @Override
    public void hide() {
        controls.hide();
    }
    @FXML
    public void setLocked(boolean val) {
        container.locked.set(val);
    }
    public boolean isLocked() {
        return container.locked.get();
    }
    public final boolean isUnderLock() {
        return container.isUnderLock();
    }
    @FXML
    public void toggleLocked() {
        container.locked.set(!container.locked.get());
    }
    
/**************************** activity node ***********************************/
    
    public final void setActivityVisible(boolean v) {
        activityPane.setVisible(v);
        activityPane.getStyleClass().addAll(bgr_STYLECLASS);
        activityPane.pseudoClassStateChanged(draggedPSEUDOCLASS, v);
        getContent().setOpacity(v ? 0.2 : 1);
        getContent().setEffect(v ? new BoxBlur(2, 2, 1) : null);
        getContent().setMouseTransparent(v);
    }
    
    public final void setActivityContent(Node n) {
        activityPane.getChildren().setAll(n);
        StackPane.setAlignment(n, Pos.CENTER);
    }
}
