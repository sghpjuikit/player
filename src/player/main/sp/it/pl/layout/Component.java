package sp.it.pl.layout;

import java.util.Optional;
import java.util.UUID;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import sp.it.pl.layout.container.Container;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.Widget.LoadType;
import sp.it.util.access.V;
import sp.it.util.collections.map.PropertyMap;
import sp.it.util.conf.IsConfig;
import static sp.it.pl.layout.widget.Widget.LoadType.AUTOMATIC;
import static sp.it.pl.main.AppKt.APP;

/**
 * Defines wrapper of loadable graphical component.
 * Basis for wrappers - containers or wrapped widgets.
 */
public abstract class Component {

	/** Unique ID. Permanent. Persists application life cycle. */
	public final UUID id;
	/**
	 * Simple storage for component state. Persists application life cycle. All data will serialize
	 * and deserialize.
	 */
	public final PropertyMap<String> properties = new PropertyMap<>();
	/** Denotes weather component loading is delayed until user manually requests it.  */
	@IsConfig(name = "Load type", info = "Manual type delays component loading until user manually requests it")
	public final V<LoadType> loadType = new V<>(AUTOMATIC);

	public Component(ComponentDb state) {
		this.id = state.getId();
		this.properties.putAll(state.getProperties());
		locked.setValue(state.getLocked());
		loadType.setValue(state.getLoading());
	}

	/** @return name */
	abstract public String getName();

	/**
	 * Loads the graphical element this container wraps.
	 *
	 * @return root node of the loaded container
	 */
	abstract public Node load();

	abstract public void focus();

	abstract public void close();

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

	/** @return window containing this component or null if not loaded or not in any window. */
	public Optional<javafx.stage.Window> getWindow() {
		javafx.stage.Window w = null;
		if (this instanceof Container) {
			Node root = ((Container)this).getRoot();
			Scene scene = root==null ? null : root.getScene();
			w = scene==null ? null : scene.getWindow();
		}
		if (this instanceof Widget) {
			Node root = ((Widget)this).getGraphics();
			Scene scene = root==null ? null : root.getScene();
			w = scene==null ? null : scene.getWindow();
		}
		return Optional.ofNullable(w);
	}

	public String getExportName() {
		return this instanceof Widget ? ((Widget) this).custom_name.getValue() : getName();
	}

	public void swapWith(Container c, int i) {
		if (c!=null) {
			c.swapChildren(getParent(), i, this);
		}
	}

	abstract public ComponentDb toDb();

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
	public final LockedProperty lockedUnder = new LockedProperty();

	public class LockedProperty extends SimpleBooleanProperty {

		public LockedProperty() {
			super(false);
			initLocked(null);
		}

		// cal when component parent changes
		public void initLocked(Component p) {
			unbind();
			if (p==null) bind(locked.or(APP.ui.getLayoutLocked()));
			else bind(p.lockedUnder.or(locked).or(APP.ui.getLayoutLocked()));
		}

		// call when cosing component
		public void dispose() {
			unbind();
		}
	}

}