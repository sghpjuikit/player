package sp.it.pl.layout.container;

import com.thoughtworks.xstream.annotations.XStreamOmitField;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import sp.it.pl.layout.AltState;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.area.ContainerNode;
import sp.it.pl.layout.container.bicontainer.BiContainer;
import sp.it.pl.layout.container.layout.Layout;
import sp.it.pl.layout.widget.Widget;
import sp.it.util.type.ClassName;
import static java.util.stream.Collectors.toList;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import static org.slf4j.LoggerFactory.getLogger;
import static sp.it.pl.layout.widget.EmptyWidgetKt.getEmptyWidgetFactory;
import static sp.it.pl.main.AppDragKt.getText;
import static sp.it.pl.main.AppDragKt.hasText;
import static sp.it.pl.main.AppDragKt.installDrag;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.functional.UtilKt.consumer;

/**
 * Component able to store other Components.
 * <p/>
 * The key element for layouts and their modularity.
 * <p/>
 * Containers are components storing their children and with layout-defining
 * behavior such as loading itself and its content and supporting layout
 * operations requiring the awarenes of the component within layout hierarchy.
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
 - reload itself so the layout change trasforms into graphical change.
 NOTE: invalid index (for example out of range) must be ignored for some
 behavior to work correctly.This is because indexOf() method returns invalid (but still number)
 index if component is not found. Therefore such index must be ignored.
 */
public abstract class Container<G extends ContainerNode> extends Component implements AltState {

    public static Container testControlContainer() {
        BiContainer root = new BiContainer(HORIZONTAL);

        BiContainer c11 = new BiContainer(VERTICAL);
        BiContainer c12 = new BiContainer(VERTICAL);
        root.addChild(1, c11);
        root.addChild(2, c12);

        BiContainer c21 = new BiContainer(HORIZONTAL);
        BiContainer c22 = new BiContainer(HORIZONTAL);
        c12.addChild(1, c21);
        c12.addChild(2, c22);

        c11.addChild(1, getEmptyWidgetFactory().create());
        c11.addChild(2, getEmptyWidgetFactory().create());
        c21.addChild(1, getEmptyWidgetFactory().create());
        c21.addChild(2, getEmptyWidgetFactory().create());
        c22.addChild(1, getEmptyWidgetFactory().create());
        c22.addChild(2, getEmptyWidgetFactory().create());

        return root;
    }

    public static Container testDragContainer() {
        Widget w = getEmptyWidgetFactory().create();
        BiContainer root = new BiContainer(HORIZONTAL);
        root.addChild(1,w);

        root.load();
        installDrag(
            root.getRoot(),
            MaterialDesignIcon.DICE_2,
            "Accepts text containing digit '2' and does nothing"
              + "\n\t• Release mouse to drop drag and execute action"
              + "\n\t• Continue moving to try elsewhere",
            e -> hasText(e.getDragboard()) && getText(e.getDragboard()).contains("2"),
            consumer(e -> {})
        );
        installDrag(
            w.load(),
            MaterialDesignIcon.DICE_2,
            "Accepts text containing digit '2' and does nothing"
              + "\n\t• Release mouse to drop drag and execute action"
              + "\n\t• Continue moving to try elsewhere",
            e -> hasText(e.getDragboard()) && getText(e.getDragboard()).contains("2"),
            consumer(e -> {})
        );

        return root;
    }

    @XStreamOmitField protected AnchorPane root;
    @XStreamOmitField private Container parent;
    @XStreamOmitField public G ui;

    @Override
    public String getName() {
        return ClassName.of(getClass());
    }

    @Override
    public Container getParent() {
        return parent;
    }

    // TODO: make private
    public void setParent(Container c) {
        parent = c;
        lockedUnder.initLocked(c);
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
                ((Container)c).setParent(this);
                ((Container)c).setParentRec();
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
        }
    }

    /**
     * Swaps children in the layout.
     *
     * @param i1 index of the child of this container to swap.
     * @param toParent container containing the child to swap with
     * @param toChild child to swap with
     */
    public void swapChildren(Container toParent, Integer i1, Component toChild) {
        Container<?> c1 = this;
        Container<?> c2 = toParent;

        if (toParent==null || i1==null ) return;

        Component w1 = c1.getChildren().get(i1);
        Component w2 = toChild;

        if (w2==null) return;

        int i2 = c2.indexOf(w2);

        String w1n = w1==null ? "null" : w1.getName();
        String w2n = w2==null ? "null" : w2.getName();
        getLogger(Container.class).info("Swapping widgets {} and {}", w1n,w2n);

        c1.addChild(i1, w2);
        c2.addChild(i2, w1);
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
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    public Stream<Container<?>> getAllContainers(boolean include_self) {
        Stream<Container<?>> s1 = include_self ? stream(this) : stream();
        Stream<Container<?>> s2 = getChildren().values().stream()
            .filter(c -> c instanceof Container)
            .flatMap(c -> ((Container) c).getAllContainers(true));
        return stream(s1, s2);
    }

    /**
     * Loads the graphical element this container wraps. Furthermore all the children
     * get loaded too.
     * Use for as the first load of the controller to assign the parent_pane.
     * Here, the term parent is not parent Container, but instead the very AnchorPane
     * this container will be loaded into.
     *
     * @param parentPane
     * @return the result of the call to {@link #load()}
     */
    public Node load(AnchorPane parentPane){
        root = parentPane;
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
        getAllWidgets().collect(toList()).forEach(Widget::close);   // TODO: fix some widgets closing twice

        if (parent!=null) {
            // remove from layout graph
            parent.removeChild(this);
            lockedUnder.dispose();
            // remove from scene graph if attached to it
            removeGraphicsFromSceneGraph();
        } else {
            // remove all children
            list(getChildren().keySet()).forEach(this::removeChild);
        }
        // free resources of all guis, we need to do this because we do not
        // close the sub containers, they cant even override this method to
        // implement their own implementation because it will not be invoked
        getAllContainers(false).forEach(Container::closeGraphics);
    }

    protected void removeGraphicsFromSceneGraph() {
        // to do: make sure the layout branch under this container does not
        // cause a memory leak
        if (ui!=null) root.getChildren().remove(ui.getRoot());
    }

    protected void closeGraphics() {
        if (ui!=null) ui.close();
    }

    /**
     * Set the root of this container. The container is attached to the scene
     * graph through this root. The root is parent node of all the nodes of
     * this container (including its children).
     *
     * @param rootPane
     */
    public void setRoot(AnchorPane rootPane) {
        root = rootPane;
    }

    /**
     * Returns the root.
     *
     * @return the root or null if none.
     * @see #getRoot()
     */
    public AnchorPane getRoot() {
        return root;
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

    /**
     * Invoked just after deserialization.
     *
     * @implSpec Resolve object by initializing non-deserializable fields or providing an alternative instance (e.g. to
     * adhere to singleton pattern).
     */
    protected Object readResolve() throws ObjectStreamException {
        super.readResolve();

        getChildren().values().forEach(it -> {
            if (it instanceof Container<?>)
                ((Container) it).setParent(this);
        });

        return this;
    }
}