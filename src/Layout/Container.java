
package Layout;

import Configuration.PropertyMap;
import GUI.DragUtil.WidgetTransfer;
import GUI.GUI;
import Layout.Areas.ContainerNode;
import Layout.Widgets.Widget;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import util.Log;
import static util.functional.FunctUtil.isNotNULL;

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
    AnchorPane root;
    @XStreamOmitField
    Container parent;
    
    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.getClass().getName();
    }
    
    /**
     * Equivalent to hasParent()
     * @return true if container is root - has no parent
     */
    public boolean isRoot() {
        return parent == null;
    }
    
    /** 
     * Equivalent to !isRoot()
     * @return whether has parent */
    public boolean hasParent() {
        return parent != null;
    }
    
    /** @return parent container of this container */
    public Container getParent() {
        return parent;
    }
    
    /** @return the children */
    public abstract Map<Integer, Component> getChildren();
    boolean b = false;
    
    /**
     * Adds component to specified index as child of the container.
     * @param index index of a child. Determines its position within container.
     * Null value allowed, but will be ignored.
     * @param c component to add. Null removes existing component from given
     * index.
     */
    public abstract void addChild(Integer index, Component c);
    
    /**
     * Removes child of this container if it exists.
     * @param c component to remove
     */
    public void removeChild(Component c) {
        removeChild(indexOf(c)); 
    }
    
    /**
     * Removes child of this container at specified index.
     * <p>
     * Equivalent to: addChild(index, null);
     * @param index of the child to remove. Null is ignored.
     */
    public void removeChild(Integer index) {
        Log.deb("Removing component at "+ index + " from container");
        addChild(index, null);
    }
    
    /**
     * Swaps children in the layout.
     * @param w1 child of this container to swap.
     * @param toParent container containing the child to swap with
     * @param toChild child to swap with
     */
    public void swapChildren(Container toParent, int i1, Component toChild) {
        Container c1 = this;
        Container c2 = toParent;
        // avoid pointless operation but dont rely on equals
        // subclass can overide it and cause problems
        // note: we can rely on this check only because we 
        // use Unary Container for all widgets. If we had
        // polynary Container (multiple children) this would
        // prevent them from being swapped
        if (c1==c2) return;
        
        Component w1 = c1.getChildren().get(i1);
        Component w2 = toChild;
        int i2 = c2.indexOf(w2);
        
        String w1n = w1==null ? "null" : w1.getName();
        String w2n = w2==null ? "null" : w2.getName();
        Log.deb("Swapping widgets " + w1n + " and " + w2n);
        
        c1.addChild(i1, w2);
        c2.addChild(i2, w1);
    }
    
    /**
     * Convenience method. Equivalent to: swapChildren(wt.container, i1, wt.child)
     * @see #swapChildren(Layout.Container, int, Layout.Component)
     * @see WidgetTransfer
     */
    public void swapChildren(int i1, WidgetTransfer wt) {
        swapChildren(wt.container, i1, wt.child);
    }
    
    /**
     * Returns index of a child or null if no child or parameter null.
     * @param c component
     * @return index of a child or null if no child
     */
    public Integer indexOf(Component c) {
        if (c==null) return null;
        
        for (Map.Entry<Integer, ? extends Component> entry: getChildren().entrySet()) {
            if (entry.getValue().equals(c))
                return entry.getKey();
        }
        
        return null;
    }
    
    /**
     * Equivalent to: parent.indexOf(this)
     * @throws NullPointerException when this container is root
     * @return 
     */
    public final int indexInParent() {
        return parent.indexOf(this);
    }
    
    /**
     * Returns all components in layout map of which this is the root. In other 
     * words all children recursively. The root (this) is included in the list.
     * @return 
     */
    public Stream<Component> getAllChildren() {
        List<Component> out = new ArrayList<>();
                        out.add(this);
                        
        for (Component w: getChildren().values()) {
            if(w!=null) out.add(w);
            if (w instanceof Container)
                out.addAll(((Container)w).getAllChildren().collect(Collectors.toList()));
        }
        return out.stream();
    }
    /**
     * Returns all widgets in layout map of which this is the root. In other words
     * all widget children recursively.
     * @return 
     */
    public Stream<Widget> getAllWidgets() {
        List<Widget> out = new ArrayList<>();
        for (Component w: getChildren().values()) {
            if (w instanceof Container)
                out.addAll(((Container)w).getAllWidgets().collect(Collectors.toList()));
            else
            if (w instanceof Widget)
                out.add((Widget)w);
        }
        return out.stream();
    }
    /**
     * Returns all containers in layout map of which this is the root. In other words
     * all container children recursively. The root (this) is included in the list.
     * @return 
     */
    public Stream<Container> getAllContainers() {
        Stream<Container> outs = Stream.of(this);
        Stream<Container> subs = getChildren().values().stream()
                .filter(c -> c instanceof Container)
                .flatMap(c -> ((Container)c).getAllContainers());
        return Stream.concat(outs,subs);
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
        root = _parent;
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
     * <p>
     * In practice, this removes this container from layout map (as a child from
     * its parent container) and from scene graph (all its graphics will be
     * removed).
     * <p>
     * This has an effect of closing the whole layout branch spanning from this
     * container.
     * <p>
     * If the container is root (in case of {@link Layout} for example, this
     * method preserves the container itself, but all its children will undergo
     * the same procedure as they normally would - the container remains empty.
     */
    public void close() {
        // close children
        //    we close all widgets to free their resources and then close
        //    this container to cut off its branch from scene graph and
        //    layout graph.
        //    We want to avoid recursively closing every container by one
        getAllWidgets().map(Widget::getController)
                // there might be passive widgets that avoided being loaded
                .filter(isNotNULL)
                .forEach(c->c.close());
        
        if (!isRoot()) {
            // remove from layout graph
            parent.removeChild(this);
            // remove from scene graph if attached to it
            removeGraphicsFromSceneGraph();
        } else {
            // remove all children 
            getChildren().keySet().forEach(this::removeChild);
        }
        // free resources of all guis, we need to do this because we do not
        // close the sub containers, they cant even override this method to
        // implement their own implementation because it will not be invoked
//        getAllContainers().forEach(Container::closeGraphics);
        // BUG throws stackoverflow of some kind
    }
    
    protected void removeGraphicsFromSceneGraph() {
        // to do: make sure the layout brachn under this container does not
        // cause a memory leak
        ContainerNode a = getGraphics();
        if(a!=null) root.getChildren().remove(a.getRoot()); 
    }
    protected void closeGraphics() {
        ContainerNode a = getGraphics();
        if(a!=null) a.close();
    }
    
    /**
     * Set the root of this container. The container is attached to the scene
     * graph through this root. The root is parent node of all the nodes of
     * this container (including its children).
     * @param pane 
     */
    public void setRoot(AnchorPane pane) {
        root = pane;
    }
    
    /**
     * Returns the root. See {@link #getRoot()}
     * @return the root or null if none.
     */
    public AnchorPane getRoot() {
        return root;
    }
    
    public abstract ContainerNode getGraphics();

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
    
    /** Locks container. */
    public void setLocked(boolean val) {
        properties.set("locked", val);
    }
    /** Changes the lock on/off. */
    public void toggleLock() {
        setLocked(!isLocked());
    }
    /**
     * Whether the container is locked. The effect of lock is not implicit and
     * might vary. Generally, the container becomes immune against certain
     * changes.
     * <p>
     * Note that the method {@link #isUnderLock()} may be better fit for use.
     * This method has use mostly internally.
     * @return true if this container is locked. Note that container can still
     * be under lock from its parent 
     */
    public boolean isLocked() {
        return (Boolean) properties.get("locked", false);
    }
    /**
     * @return true if this container is under locked either its own or one of 
     * its parents or under whole of GUI's.
     */
    public boolean isUnderLock() {
        return isLocked() || (hasParent() && parent.isUnderLock()) || GUI.isLayoutLocked();
    }

    @Override
    public boolean equals(Object o) {        
        return this==o;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(this.properties);
        hash = 67 * hash + Objects.hashCode(this.root);
        hash = 67 * hash + Objects.hashCode(this.parent);
        hash = 67 * hash + (this.b ? 1 : 0);
        return hash;
    }
    
/******************************************************************************/
    
    @Override
    public void show() {
        if(getGraphics()!=null) getGraphics().show();
        getChildren().values().stream().filter(child -> child instanceof AltState)
                .forEach(child -> ((AltState) child).show());
        
    }
    @Override
    public void hide() {
        if(getGraphics()!=null) getGraphics().hide();
        getChildren().values().stream().filter(child -> child instanceof AltState)
                .forEach(child -> ((AltState) child).hide()); 
    }
}
