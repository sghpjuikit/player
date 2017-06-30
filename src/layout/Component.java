package layout;

import java.io.File;
import java.io.ObjectStreamException;
import java.util.UUID;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.Scene;

import org.atteo.classindex.IndexSubclasses;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import gui.Gui;
import gui.objects.window.stage.Window;
import layout.container.Container;
import layout.widget.Widget;
import layout.widget.Widget.LoadType;
import main.App;
import util.access.V;
import util.collections.map.PropertyMap;
import util.conf.IsConfig;
import util.type.Util;

import static layout.widget.Widget.LoadType.AUTOMATIC;
import static main.App.APP;
import static util.dev.Util.log;

/**
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
	@IsConfig(name = "Load type", info = "Manual type delays component loading until user manually requests it")
	public final V<LoadType> loadType = new V<>(AUTOMATIC);


	/** @return name */
	abstract public String getName();

	/**
	 * Loads the graphical element this container wraps.
	 *
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
	public final boolean hasParent() {
		return getParent() != null;
	}

	/**
	 * Parent component. Root component (at the top of the hierarchy) has no parent. Every component aside from root
	 * must have a parent.
	 * <p/>
	 * Use to traverse hierarchy.
	 *
	 * @return parent container of this container
	 */
	abstract public Container<?> getParent();

	/**
	 * Equivalent to nullsafe version of: getParent().indexOf(this)
	 *
	 * @return parent.indexOf(this) or null if no parent
	 */
	public final Integer indexInParent() {
		Container p = getParent();
		return p==null ? null : p.indexOf(this);
	}

	/**
	 * Top level parent - root of the hierarchy.
	 * <p/>
	 * If this component has no parent:
	 * <ul>
	 * <li> {@link Widget} returns null
	 * <li> {@link Container} returns itself
	 * </ul>
	 */
	public Container<?> getRootParent() {
		Container parent = getParent();
		return parent!=null
			? parent.getRootParent()
			: this instanceof Container
				  ? (Container) this
				  : null;
	}

	// TODO: use Optional<>
	/** Window containing this component. Null if not loaded or not in any window. */
	public Window getWindow() {
		Window w = null;
		if (this instanceof Container) {
			Node root = ((Container)this).getRoot();
			Scene scene = root==null ? null : root.getScene();
			javafx.stage.Window stage = scene==null ? null : scene.getWindow();
			w = stage==null ? null : (Window)stage.getProperties().get("window");
		}
		if (this instanceof Widget) {
			Node root = ((Widget)this).getGraphics();
			Scene scene = root==null ? null : root.getScene();
			javafx.stage.Window stage = scene==null ? null : scene.getWindow();
			w = stage==null ? null : (Window)stage.getProperties().get("window");
		}
		return w==null ? APP.windowManager.getActive().orElse(null) : w;
	}

//    /** @return whether this component is currently open*/
//    public boolean isOpen() {
//        // check if this is not standalone widget (not in a layout)
//        if (APP.widgetManager.standaloneWidgets.contains(this)) return true;
//
//        Component c = this;
//        Component p = this;
//        do {
//            p = c instanceof Widget ? null : ((Container)c).getParent();
//            if (p!=null) c = p;
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
		String name = this instanceof Widget ? ((Widget<?>)this).custom_name.getValue() : getName();
		File f = new File(dir,name + ".fxwl");
		App.APP.serializators.toXML(this, f)
			.ifError(e -> log(Component.class).error("Failed to export component {}", getName(), e));
	}

	public void swapWith(Container c, int i) {
		if (c!=null) {
			c.swapChildren(getParent(), i, this);
		}
	}

//*************************************** SERIALIZATION *******************************************/

	protected Object readResolve() throws ObjectStreamException {
		// Special case. The class at hand (LockedProperty) is inner class (due to unavoidable
		// dependency on this one) and can not be deserialized since we can not create an
		// XStream converter for it.
		//
		// We must always initialize it manually (we use @XStreamOmit for that) and because
		// it really should be final, but the initialization is here, we use reflection
		if (lockedUnder == null) Util.setField(this, "lockedUnder", new LockedProperty());
		return this;
	}

//***************************************** LOCKING ***********************************************/

	/**
	 * Whether the container is locked. The effect of lock is not specifically defined and
	 * may vary. Generally, the container becomes immune against certain
	 * layout changes.
	 * <p/>
	 * @see #lockedUnder which may be better fit for use, as any of the parents may be locked
	 */
	public final BooleanProperty locked = new SimpleBooleanProperty(false);
	/** True if this container is locked or any parent is locked or entire ui is locked. */
	@XStreamOmitField // see readResolve() method
	public final LockedProperty lockedUnder = new LockedProperty();

	public class LockedProperty extends SimpleBooleanProperty {

		public LockedProperty() {
			super(false);
			initLocked(null);
		}

		// cal when component parent changes
		public void initLocked(Component p) {
			unbind();
			if (p==null) bind(locked.or(Gui.locked_layout));
			else bind(p.lockedUnder.or(locked).or(Gui.locked_layout));
		}

		// call when cosing component
		public void dispose() {
			unbind();
		}
	}

}