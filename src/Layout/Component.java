
package Layout;

import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.UUID;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.Scene;

import org.atteo.classindex.IndexSubclasses;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import Layout.container.Container;
import Layout.widget.Widget;
import Layout.widget.Widget.LoadType;
import gui.GUI;
import gui.objects.Window.stage.Window;
import main.App;
import util.collections.map.PropertyMap;

import static Layout.widget.Widget.LoadType.AUTOMATIC;
import static util.dev.Util.log;

/**
 * @author uranium
 *
 * Defines wrapper of loadable graphical component.
 * Basis for wrappers - containers or wrapped widgets.
 */
@IndexSubclasses()
public abstract class Component {

    /** Unique ID. Permanent. Persists application life cycle. */
    public final UUID id = UUID.randomUUID();
    /**
     * Simple storage for component state. Persists application life cycle. All data will serialize
     * and deserialize.
     */
    public final PropertyMap<String> properties = new PropertyMap<>();
    /** Denotes weather component loading is delayed until user manually requests it.  */
    public final ObjectProperty<LoadType> loadType = new SimpleObjectProperty<>(AUTOMATIC);

    /** @return name */
    abstract public String getName();

    /**
     * Loads the graphical element this container wraps.
     * @return root node of the loaded container
     */
    abstract public Node load();

    /**
     * Return if this is at the top of the hierarchy.
     * Equivalent to hasParent().
     *
     * @return true if container is root - has no parent
     */
    public boolean isRoot() {
        return !hasParent();
    }

    /**
     * Equivalent to getParent()!=null and !isRoot()
     *
     * @return true if parent not null
     */
    public boolean hasParent() {
        return getParent() != null;
    }

    /**
     * Parent component. Root component (at the top of the hierarchy) has no parent. Component,
     * which is not part of the hierarchy or is not loaded yet will have no parent either.
     * <p>
     * Use to traverse hierarchy.
     *
     * @return parent container of this container */
    abstract public Container<?> getParent();

    /**
     * Equivalent to nullsafe version of: getParent().indexOf(this)
     * @return parent.indexOf(this) or null if no parent
     */
    public final Integer indexInParent() {
        Container p = getParent();
        return p==null ? null : p.indexOf(this);
    }

    /**
     * Top level parent - root of the hierarchy.
     * <p>
     * If this component has no parent:
     * <ul>
     * <li> {@link Widget} returns null
     * <li> {@link Container} returns itself
     * </ul>
     */
    public Container<?> getRootParent() {
        if(hasParent())
            return getParent().getRootParent();
        else {
            if(this instanceof Container) return (Container) this;
            else return null;
        }
    }

    /** Window containing this component. Null if not loaded or not in any window. */
    public Window getWindow() {
        Window w = null;
        if(this instanceof Container) {
            Node root = ((Container)this).getRoot();
            Scene scene = root==null ? null : root.getScene();
            javafx.stage.Window stage = scene==null ? null : scene.getWindow();
            w = stage==null ? null : (Window)stage.getProperties().get("window");
        }
        if(this instanceof Widget) {
            Node root = ((Widget)this).getGraphics();
            Scene scene = root==null ? null : root.getScene();
            javafx.stage.Window stage = scene==null ? null : scene.getWindow();
            w = stage==null ? null : (Window)stage.getProperties().get("window");
        }
        return w==null ? Window.getActive() : w;
    }

//    /** @return whether this component is currently open*/
//    public boolean isOpen() {
//        // check if this isnt standalone widget (not in a layout)
//        if(APP.widgetManager.standaloneWidgets.contains(this)) return true;
//
//        Component c = this;
//        Component p = this;
//        do {
//            p = c instanceof Widget ? null : ((Container)c).getParent();
//            if(p!=null) c = p;
//        } while(p!=null);
//
//        // top container is always layout
//        return c instanceof Layout;
//    }

    /**
     * Creates a launcher for this component in given directory. Launcher is a
     * file, opening which by this application opens this component with its
     * current settings.
     */
    public void exportFxwl(File dir) {
        File f = new File(dir,getName() + ".fxwl");
        try {
            App.APP.serializators.toXML(this, f);
        } catch (IOException ex) {
            log(Component.class).error("Failed to export component {} to {}", getName(),f);
        }
    }

    public void swapWith(Container c, int i) {
        if(c!=null) {
            c.swapChildren(getParent(), i, this);
        }
    }

//*************************************** SERIALIZATION *******************************************/

    protected Object readResolve() throws ObjectStreamException {
        if(lockedUnder == null)
            util.Util.setField(this, "lockedUnder", new LockedProperty());
        if(loadType == null)
            util.Util.setField(this, "loadType", new SimpleObjectProperty<Widget.LoadType>(AUTOMATIC));
        return this;
    }

//***************************************** LOCKING ***********************************************/

    /**
     * Whether the container is locked. The effect of lock is not specifically defined and
     * may vary. Generally, the container becomes immune against certain
     * layout changes.
     * <p>
     * @see #lockedUnder which may be better fit for use, as any of the parents may be locked
     */
    public final BooleanProperty locked = new SimpleBooleanProperty(false);
    /** True if this container is locked or any parent is locked or entire ui is locked. */
    @XStreamOmitField
    public final LockedProperty lockedUnder = new LockedProperty();

    public class LockedProperty extends SimpleBooleanProperty {

        public LockedProperty() {
            super(false);
            initLocked(null);
        }

        // cal when component parent changes
        public void initLocked(Component p) {
            unbind();
            if(p==null) bind(locked.or(GUI.locked_layout));
            else bind(p.lockedUnder.or(locked).or(GUI.locked_layout));
        }

        // call when cosing component
        public void close() {
            unbind();
        }
    }


}
