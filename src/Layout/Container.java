
package Layout;

import Configuration.PropertyMap;
import Layout.Widgets.Widget;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

/**
 * @author uranium
 * Component able to store other Components.
 * <p>
 * The key element for layouts and their modularity.
 * <p>
 * Containers are components storing their children and with layout-defining
 * behavior such as loading itself and its content and supporting layout
 * operations requiring the awarenes of the component within layout hierarchy.
 * <p>
 * Containers are not graphical components, Containers wrap them. This creates
 * an abstraction layer that allows for defining layout hierarchy - layout maps
 * separately from scene-graph - the graphical hierarchy.
 * Layout map is complete hierarchical structure of Components spanning from
 * single Container called root.
 * <p>
 * Containers need to be lightweight wrappers able to be serialized so its layout
 * map can be later be reconstructed during/by deserialization.
 * <p>
 * The children of the container are indexed in order to identify their position
 * within the container. How the indexes are interpreted is left up on the container's
 * implementation logic. The children collection is implemented as Map<Integer,
 * Component>.
 * <p>
 * Container is called pure, if it can contain only containers.
 * Container is called leaf, if it can contain only non-containers.
 * Note the difference between containing and able to contain! The pure and leaf
 * containers can have their own class implementation.
 * <p>
 * Container implementation (extending class) must handle
 * - adding the child to its child map (includes the index interpretation)
 * - removing previously assigned children
 * - reload itself so the layout change trasforms into graphical change.
 * NOTE: invalid index (for example out of range) must be ignored for some
 * behavior to work correctly.This is because indexOf() method returns invalid (but still number)
 * index if component is not found. Therefore such index must be ignored.
 */
public abstract class Container extends Component implements AltState {
    
    /*
     * Property map - map of properties. The map serves as unified property
     * storage mechanism to allow easy manipulation (serialization, etc) of
     * properties.
     * Its responsibility of the wrapped component to maintain updated state
     * reflecting the properties in the map.
     */
    public final PropertyMap properties = new PropertyMap();
    @XStreamOmitField
    AnchorPane parent_pane;
    @XStreamOmitField
    Container parent;
    
    @Override
    public String getName() {
        return this.getClass().getName();
    }
    
    /**
     * Equivalent to hasParent()
     * @return true if container is root - has no parent
     */
    public boolean isRoot() {
        return (parent == null);
    }
    
    /** 
     * Equivalent to isRoot()
     * @return whether has parent */
    public boolean hasParent() {
        return (parent == null);
    }
    
    /** @return parent container of this container */
    public Container getParent() {
        return parent;
    }
    
    /** @return the children */
    public abstract Map<Integer, ? extends Component> getChildren();
    boolean b = false;
    /**
     * Adds component to specified index as child of the container.
     * @param index index of a child. Determines its position within container.
     * Null value allowed, but will be ignored.
     * @param c component to add. Null allowed - sets empty content at specified
     * position.
     */
    public void addChild(Integer index, Component c) {
        System.out.println("B" +b+" index "+index + " c "+c);
        if(c==null) {
            if(index!=null) {
                if(b) return;
                Component cm = getChildren().get(index);
                if(cm!=null){
                    if(cm instanceof Widget) {
                        ((Widget)cm).getController().OnClosing();
                    }
                    else if(cm instanceof Container) {
                        if(b) return;
                        b = true;
                        ((Container)cm).close();
                    }
                }
            }
        }
    }
    
    /**
     * Removes child of this container if it exists.
     * @param c component to remove
     */
    public void removeChild(Component c) {System.out.println("index is "+ indexOf(c));
        removeChild(indexOf(c)); 
    }
    
    /**
     * Removes child of this container at specified index.
     * @param index of the child to remove. Null is ignored.
     */
    public void removeChild(Integer index) {System.out.println("REMOVE INDEX "+index + " "+getChildren().size());
        if(index==null) return;
        addChild(index, null);
    }
    
    /**
     * Swaps children in the layout.
     * @param w1 child of this widget to swap.
     * @param c2 child to swap with
     * @param w2 container containing the child to swap with
     */
    public void swapChildren(Component w1, Container c2, Component w2) {        // Log.deb("swapping "+w1.getName() + " with " + w2.getName());
        Container c1 = this;
        if (c1.equals(c2)) return;
        
        if(w1==null) w1 = Widget.EMPTY();
        if(w2==null) w2 = Widget.EMPTY();
        
        Integer i1 = c1.indexOf(w1);
        Integer i2 = c2.indexOf(w2);
        c1.addChild(i1, w2);
        c2.addChild(i2, w1);
        c1.load();
        c2.load();
    }
    
    /**
     * Returns index of a child or null if no child
     * @param c component
     * @return index of a child or null if no child
     */
    public Integer indexOf(Component c) {
        for (Map.Entry<Integer, ? extends Component> entry: getChildren().entrySet()) {
            if (entry.getValue().equals(c))
                return entry.getKey();
        }
        
        return null;
    }
    
    /**
     * Returns all components in layout map of which this is the root. In other 
     * words all children recursively. The root (this) is included in the list.
     * @return 
     */
    public List<Component> getAllChildren() {
        List<Component> out = new ArrayList<>();
                        out.add(this);
        for (Component w: getChildren().values()) {
            if(w!=null) out.add(w);
            if (w instanceof Container)
                out.addAll(((Container)w).getAllChildren());
        }
        return out;
    }
    /**
     * Returns all widgets in layout map of which this is the root. In other words
     * all widget children recursively.
     * @return 
     */
    public List<Widget> getAllWidgets() {
        List<Widget> out = new ArrayList<>();
        for (Component w: getChildren().values()) {
            if (w instanceof Container)
                out.addAll(((Container)w).getAllWidgets());
            else
            if (w instanceof Widget)
                out.add((Widget)w);
        }
        return out;
    }
    /**
     * Returns all containers in layout map of which this is the root. In other words
     * all container children recursively. The root (this) is included in the list.
     * @return 
     */
    public List<Container> getAllContainers() {
        List<Container> out = new ArrayList<>();
                        out.add(this);
        for (Component c: getChildren().values()) {
            if (c instanceof Container) {
                out.add((Container)c);
                out.addAll(((Container)c).getAllContainers());
            }
        }
        return out;
    }
    
    /**
     * Loads the graphical element this container wraps. Furthermore all the children
     * get loaded too.
     * Use for as the first load of the controller to assign the parent_pane.
     * Here, the term parent isnt parent Container, but instead the very AnchorPane
     * this container will be loaded into.
     * @param _parent
     * @return 
     */
    public Node load(AnchorPane _parent){
        parent_pane = _parent;
        return load();   
    }
    
    /**
     * Effectively a reload.
     * Loads the whole container and its children - the whole layout sub branch
     * having this container as root - to its parent_pane. The parent_pane must be assigned
     * before calling this method.
     * @return 
     */
    @Override
    public abstract Node load();
    
    /**
     * Closes this container and its content. Can not be undone.
     * In practice, this method removes this container as a child from its 
     * parent container.
     * If the container is root, this method is a no-op.
     */
    public void close() {
        System.out.println("SIZE "+getAllWidgets().size());
//        getAllWidgets().stream().map(w->w.getController()).forEach(c->{
//            System.out.println(c.getClass().getName());
//            System.out.println(indexOf(c.getWidget()) + " " + c.getWidget().getName());
//            c.OnClosing();
//        });
        if (!isRoot())
            parent.removeChild(this);
    }
    
    public void setParentPane(AnchorPane pane) {
        parent_pane = pane;
    }

    /**
     * Properly links up this container with its children and propagates this
     * call down on the children and so on.
     * This method is required to fully initialize the layout after deserialization
     * because some field values can not be serialized and need to be manually
     * initialized.
     * Use on layout reload, immediately after the container.load() method.
     */
    public void initialize() {
        for (Component c: getChildren().values()) {
            if (c instanceof Container) {
                ((Container)c).parent = this;
                ((Container)c).initialize();
            }
        }
    }
    
/******************************************************************************/
    
    @Override
    public void show() {
        getChildren().values().stream().filter(child -> child instanceof AltState)
                .forEach(child -> ((AltState) child).show());
    }
    @Override
    public void hide() {
        getChildren().values().stream().filter(child -> child instanceof AltState)
                .forEach(child -> ((AltState) child).hide()); 
    }
}
