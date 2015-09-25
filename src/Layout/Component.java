
package Layout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.UUID;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.Scene;

import org.atteo.classindex.IndexSubclasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import Layout.container.Container;
import Layout.container.layout.Layout;
import Layout.widget.Widget;
import Layout.widget.WidgetManager;
import gui.GUI;
import gui.objects.Window.stage.Window;
import main.App;
import util.collections.map.PropertyMap;

/**
 * @author uranium
 *
 * Defines wrapper of loadable graphical component.
 * Basis for wrappers - containers or wrapped widgets.
 */
@IndexSubclasses()
public abstract class Component {

    private static final Logger LOGGER = LoggerFactory.getLogger(Component.class);
    private static final XStream X = App.APP.serialization.x;

    /** Unique ID. Permanent. Persists application life cycle. */
    public final UUID id = UUID.randomUUID();
    /** Simple storage. Persists application life cycle. */
    public final PropertyMap properties = new PropertyMap();


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

    /** @return whether this component is currently open*/
    public boolean isOpen() {
        // check if this isnt standalone widget (not in a layout)
        if(WidgetManager.standaloneWidgets.contains(this)) return true;

        Component c = this;
        Component p = this;
        do {
            p = c instanceof Widget ? null : ((Container)c).getParent();
            if(p!=null) c = p;
        } while(p!=null);

        // top container is always layout
        return c instanceof Layout;
    }

    /**
     * Creates a launcher for this component in given directory. Launcher is a
     * file, opening which by this application opens this component with its
     * current settings.
     */
    public void exportFxwl(File dir) {
        File f = new File(dir,getName() + ".fxwl");
        try {
            X.toXML(this, new BufferedWriter(new FileWriter(f)));
        } catch (IOException ex) {
            LOGGER.error("Unable to export component launcher for {} into {}", getName(),f);
        }
    }

//*************************************** SERIALIZATION *******************************************/

    protected Object readResolve() throws ObjectStreamException {
        if(lockedUnder == null)
            util.Util.setField(this, "lockedUnder", new LockedProperty());
        return this;
    }

//***************************************** LOCKING ***********************************************/

    /**
     * Whether the container is locked. The effect of lock is not implicit and
     * may vary. Generally, the container becomes immune against certain
     * layout changes.
     * <p>
     * Note that the method {@link #isUnderLock()} may be better fit for use,
     * because unlocked container can still be under lock from any of its parents.
     *
     * @return true if this container is locked.
     */
    public final BooleanProperty locked = new SimpleBooleanProperty(false);
    /**
     * @return true if this container is locked or any parent is locked or entire ui is locked
     */
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
