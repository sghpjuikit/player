package sp.it.pl.layout.container;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import org.jetbrains.annotations.NotNull;
import sp.it.pl.layout.AltState;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.ComponentDb;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.main.AppKt;
import sp.it.pl.ui.objects.window.stage.WindowHelperKt;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.Util.stream;

/**
 * Component able to store other Components.
 * <p/>
 * The key element for layouts and their modularity.
 * <p/>
 * Containers are components storing their children and with layout-defining
 * behavior such as loading itself and its content and supporting layout
 * operations requiring the awareness of the component within layout hierarchy.
 * <p/>
 Containers are not graphical components, Containers wrap them. This creates
 an abstraction layer that allows for defining layout hierarchy - layout maps
 separately from scene-graph - the graphical hierarchy.
 Layout mapB is complete hierarchical structure of Components spanning from
 single Container called root.
 <p/>
 Containers need to be lightweight wrappers able to be serialized so its layout
 mapB can be later be reconstructed during/by deserialization.
 <p/>
 * The children of the container are indexed in order to identify their position
 * within the container. How the indexes are interpreted is left up on the container's
 * implementation logic. The children collection is implemented as Map<Integer,
 * Component>.
 * <p/>
 * Container is called pure, if it can contain only containers.
 * Container is called leaf, if it can contain only non-containers.
 * Note the difference between containing and able to contain! The pure and leaf
 * containers can have their own class implementation.
 * <p/>
 Container implementation (extending class) must handle
 - adding the child to its child mapB (includes the index interpretation)
 - removing previously assigned children
 - reload itself so the layout change transforms into graphical change.
 NOTE: invalid index (for example out of range) must be ignored for some
 behavior to work correctly.This is because indexOf() method returns invalid (but still number)
 index if component is not found. Therefore such index must be ignored.
 */
public abstract class Container<G extends ComponentUi> extends Component implements AltState {

    private AnchorPane rootImpl;
    public G ui;

    public Container(ComponentDb state) {
        super(state);
    }

    /*
     * Properly links up this container with its children and propagates this
     * call down on the children and so on.
     * This method is required to fully setParentRec the layout after deserialization
     * because some field values can not be serialized and need to be manually
     * initialized.
     * Use on layout reload, immediately after the container.load() method.
     */
    // TODO: remove
    public void setParentRec() {
        for (Component c: getChildren().values()) {
            if (c instanceof Container) {
                c.setParent(this);
                ((Container<?>) c).setParentRec();
            }
        }
    }

    /** @return the children */
    public abstract Map<Integer, Component> getChildren();
    boolean b = false;

    // TODO: this should close previous component
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
        Integer i = indexOf(c);
        if (i!=null) {
            addChild(i, null);
            c.close();
        }
    }

    /**
     * Removes child of this container at specified index.
     * <p/>
     * Equivalent to: addChild(index, null);   // TODO: no it isnt! fix
     * @param index of the child to remove. Null is ignored.
     */
    public void removeChild(Integer index) {
        if (index!=null) {
            Component c = getChildren().get(index); // capture before reload
            addChild(index, null);  // reload
            if (c!=null) c.close();
            closeWindowIfEmpty();
        }
    }

    /**
     * Swaps children in the layout.
     *
     * @param i1 index of the child of this container to swap.
     * @param toParent container containing the child to swap with
     * @param toChild child to swap with
     */
    @SuppressWarnings({"UnnecessaryLocalVariable","ConstantConditions"})
    public void swapChildren(Container<?> toParent, Integer i1, Component toChild) {
        Container<?> c1 = this;
        Container<?> c2 = toParent;

        if (toParent==null || i1==null ) return;

        Component w1 = c1.getChildren().get(i1);
        Component w2 = toChild;

        if (w2==null) return;

        int i2 = c2.indexOf(w2);

        String w1n = w1==null ? "null" : w1.getName();
        String w2n = w2==null ? "null" : w2.getName();
        getLogger(Container.class).info("Swapping widgets {} and {}", w1n, w2n);

        c1.addChild(i1, w2);
        c2.addChild(i2, w1);

        c1.closeWindowIfEmpty();
        c2.closeWindowIfEmpty();
    }

    protected boolean isEmptyForCloseWindowIfEmpty() {
        return getAllWidgets().findFirst().isEmpty() && getAllContainers(true)
            .noneMatch(it -> it.properties.keySet().stream().noneMatch(itt -> itt.startsWith("reloading=")));
    }

    protected void closeWindowIfEmpty() {
        var rp = getRootParent();
        var isEmpty = rp!=null && rp.isEmptyForCloseWindowIfEmpty();
        var w = rp==null ? null : rp.getWindow();
        var aw = w==null ? null : WindowHelperKt.asAppWindow(w);
        var awIsEmpty = aw!=null && aw.getLayout()!=null && aw.getLayout().isEmptyForCloseWindowIfEmpty();

        if (AppKt.APP.windowManager.getWindowDisallowEmpty().getValue() && isEmpty && awIsEmpty)
            aw.hide();
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

    /** @return available index for child or null if none available. */
    public abstract Integer getEmptySpot();

    /**
     * Returns all components in layout mapB of which this is the root. In other
     * words all children recursively. The root (this) is included in the list.
     *
     * @return components
     */
    public Stream<Component> getAllChildren() {
        List<Component> out = new ArrayList<>();
                        out.add(this);

        for (Component w: getChildren().values()) {
            if (w!=null) out.add(w);
            if (w instanceof Container)
                out.addAll(((Container<?>)w).getAllChildren().collect(toList()));
        }
        return out.stream();
    }

    /**
     * Returns all widgets in layout mapB of which this is the root. In other words
     * all widget children recursively.
     *
     * @return widgets
     */
    public Stream<Widget> getAllWidgets() {
        List<Widget> out = new ArrayList<>();
        for (Component w: getChildren().values()) {
            if (w instanceof Container)
                out.addAll(((Container<?>)w).getAllWidgets().collect(toList()));
            else if (w instanceof Widget)
                out.add((Widget) w);
        }
        return out.stream();
    }
    
    /**
     * Returns all containers in layout mapB of which this is the root. In other words
     * all container children recursively. The root (this) is included in the list.
     *
     * @return containers
     */
    public Stream<Container<?>> getAllContainers(boolean include_self) {
        Stream<Container<?>> s1 = include_self ? stream(this) : stream();
        Stream<Container<?>> s2 = getChildren().values().stream()
            .filter(c -> c instanceof Container)
            .flatMap(c -> ((Container<?>) c).getAllContainers(true));
        return stream(s1, s2);
    }

    /**
     * Loads the graphical element this container wraps. Furthermore all the children
     * get loaded too.
     * Use for as the first load of the controller to assign the parent_pane.
     * Here, the term parent is not parent Container, but instead the very AnchorPane
     * this container will be loaded into.
     *
     * @return the result of the call to {@link #load()}
     */
    public Node load(AnchorPane parentPane){
        rootImpl = parentPane;
        return load();
    }

    /**
     * Effectively a reload.
     * Loads the whole container and its children - the whole layout sub branch
     * having this container as root - to its parent_pane. The parent_pane must be assigned
     * before calling this method.
     * <p/>
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public abstract Node load();

    @Override
    public void focus() {
        getAllWidgets().findFirst().ifPresent(Widget::focus);
    }

    /**
     * Closes this container and its content. Can not be undone. Any direct or indirect children
     * component will be closed.
     * This has an effect of closing the whole layout branch spanning from this
     * container.
     * <p/>
     * If this container is root (in case of {@link Layout}, only its children will close.
     */
    @Override
    public void close() {
        super.close();

        getAllWidgets().collect(toList()).forEach(Widget::close);   // TODO: fix some widgets closing twice

        if (getParent()!=null) {
            getParent().removeChild(this);   // remove from layout graph
            removeGraphicsFromSceneGraph(); // remove from scene graph if attached to it
        } else {
            // remove all children
            list(getChildren().keySet()).forEach(this::removeChild);
        }
        // free resources of all guis, we need to do this because we do not
        // close the sub containers, they cant even override this method to
        // implement their own implementation because it will not be invoked
        getAllContainers(false).forEach(Container::disposeGraphics);
    }

    private void removeGraphicsFromSceneGraph() {
        if (ui!=null) rootImpl.getChildren().remove(ui.getRoot());
    }

	private void disposeGraphics() {
        if (ui!=null) ui.dispose();
    }

    /**
     * Returns the root.
     *
     * Set the root of this container. The container is attached to the scene
     * graph through this root. The root is parent node of all the nodes of
     * this container (including its children).
     *
     * @return the root or null if none.
     */
    public AnchorPane getRoot() {
        return rootImpl;
    }

    public AnchorPane root() {
        return rootImpl;
    }

    @Override
    public void show() {
        if (ui!=null) ui.show();
        stream(getChildren().values()).filter(AltState.class::isInstance).map(AltState.class::cast)
            .forEach(AltState::show);
    }

    @Override
    public void hide() {
        if (ui!=null) ui.hide();
        stream(getChildren().values()).filter(AltState.class::isInstance).map(AltState.class::cast)
            .forEach(AltState::hide);
    }

    protected final void setChildrenParents() {
        getChildren().values().forEach(it -> {
            if (it instanceof Container<?>)
                it.setParent(this);
        });
    }
}