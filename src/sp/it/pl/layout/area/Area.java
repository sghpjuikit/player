
package sp.it.pl.layout.area;

import java.util.ArrayList;
import java.util.List;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import sp.it.pl.gui.objects.window.stage.Window;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.container.Container;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.util.graphics.Util;
import static javafx.css.PseudoClass.getPseudoClass;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.dev.Util.noØ;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.graphics.Util.setAnchor;

/**
 * Graphical part of the container within layout.
 * The container - area is 1:1 non null relationship. Container makes up for the
 * abstract side, this class represents the graphical side.
 * <p/>
 * The lifecycle of the graphics entirely depends on the lifecycle of the
 * container. Instances of this class can not live outside of container's
 * life cycle. Note that the opposite does not necessarily hold true.
 */
public abstract class Area<T extends Container<?>> implements ContainerNode {

    public static final PseudoClass DRAGGED_PSEUDOCLASS = getPseudoClass("dragged");
    public static final List<String> bgr_STYLECLASS = list("block", "area");
    public static final String WIDGET_AREA_CONTROLS_STYLECLASS = "widget-control";
    public static final String CONTAINER_AREA_CONTROLS_STYLECLASS = "container-control";

    /** Container this are is associated with. The relationship can not be
      changed. */
    public final T container;
    /** Index of the child, the area is for or null if for many. Decides whether
      the area belongs to the specific child, or whole container. */
    public final Integer index;
    public final AnchorPane content_root = new AnchorPane();
    /** The root of this area. */
    public final AnchorPane root = new AnchorPane();
    public AreaControls controls;
    /** The root of activity content. ContainsKey custom content. */
    public final StackPane activityPane;

    /**
     * @param c container to make contract with
     * @param i index of the child, the area is for or null if for many
     */
    public Area(T c, Integer i) {
        // init final 1:1 container-area relationship
	    noØ(c);
        container = c;
        index = i;

        setAnchor(root, content_root,0d);

        // init properties
        c.properties.getOrPut(Double.class, "padding", 0d);

        // load controls
        activityPane = new StackPane();
        activityPane.setPickOnBounds(false);
        content_root.getChildren().add(activityPane);
        Util.setAnchors(activityPane, 0d);
        activityPane.toFront();
    }

    /** @return all oomponents wrapped in the area. By default returns all
     * components of the container associated with the area.*/
    public final List<Component> getAllComponents() {
        return new ArrayList<>(container.getChildren().values());
    }

    /** @return all active coponents - components that are being actively
     * displayed. */
    abstract public List<Widget> getActiveWidgets();

    /** @return the primary active component. */
    abstract public Widget getWidget();

    /**
     * Refresh area. Refreshes the content - wrapped components by calling their
     * refresh() method. Some components might not support this behavior and
     * will do nothing.
     * <p/>
     * Implementation decides which components need and will get refreshed.
     * <p/>
     * Default implementation refreshes all active widgets (ignores containers).
     */
    public void refresh() {
        for (Component c: getActiveWidgets()) {
            if (c instanceof Widget)
                ((Widget)c).getController().refresh();
        }
    }
    /**
     * Adds component to the area.
     * <p/>
     * Implementation decides how exactly. Simple
     * implementation storing single component would remove the old component
     * from layout map and add new one to the parent container.
     * @param c
     */
    abstract public void add(Component c);

    /**
     * Detaches the content into standalone content. Opens new window.
     * <p/>
     * Default implementation detaches the first active component. Does nothing
     * if no active component available.
     */
    public void detach() {
        Component c = getWidget();
        if (c==null) return;
        c.getParent().addChild(c.indexInParent(),null);
        // detach into new window
        Window w = APP.windowManager.createWindow(c);
        // set size to that of a source (also add header & border space)
        w.setSize(root.getWidth()+10, root.getHeight()+30);
    }

/******************************************************************************/

    /** Returns the content. */
    abstract public AnchorPane getContent();

    @Override
    public Pane getRoot() {
        return root;
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
        Component c = getWidget();
        if (c!=null)
            c.locked.set(val);
    }
    public boolean isLocked() {
        Component c = getWidget();
        return c!=null && c.locked.get();
    }
    public final boolean isUnderLock() {
        Component c = getWidget();
        return c==null ? container.lockedUnder.get() : c.lockedUnder.get();
    }
    @FXML
    public void toggleLocked() {
        Component c = getWidget();
        if (c!=null)
            c.locked.set(!c.locked.get());
    }

/**************************** activity node ***********************************/

    public final void setActivityVisible(boolean v) {
        if (activityPane!=null) activityPane.setVisible(v);
        getContent().setOpacity(v ? 0.2 : 1);
    }
}
