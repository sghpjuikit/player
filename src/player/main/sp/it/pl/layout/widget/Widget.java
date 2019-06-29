package sp.it.pl.layout.widget;

import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.io.File;
import java.io.ObjectStreamException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.container.ComponentUi;
import sp.it.pl.layout.container.Container;
import sp.it.pl.layout.widget.controller.Controller;
import sp.it.pl.layout.widget.controller.LegacyController;
import sp.it.pl.layout.widget.controller.LoadErrorController;
import sp.it.pl.layout.widget.controller.io.IOLayer;
import sp.it.pl.layout.widget.controller.io.Input;
import sp.it.pl.layout.widget.controller.io.Output;
import sp.it.util.Locatable;
import sp.it.util.access.V;
import sp.it.util.conf.Config;
import sp.it.util.conf.Configurable;
import sp.it.util.conf.Configuration;
import sp.it.util.conf.EditMode;
import sp.it.util.conf.IsConfig;
import sp.it.util.dev.Dependency;
import sp.it.util.file.properties.PropVal;
import sp.it.util.functional.Functors.Ƒ1;
import sp.it.util.reactive.Disposer;
import sp.it.util.type.Util;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Objects.deepEquals;
import static kotlin.io.FilesKt.deleteRecursively;
import static sp.it.pl.layout.widget.WidgetSource.OPEN;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.file.properties.PropertiesKt.readProperties;
import static sp.it.util.file.UtilKt.writeTextTry;
import static sp.it.util.functional.Util.ISNTØ;
import static sp.it.util.functional.Util.filter;
import static sp.it.util.functional.Util.firstNotNull;
import static sp.it.util.functional.Util.map;
import static sp.it.util.functional.Util.set;
import static sp.it.util.functional.Util.split;
import static sp.it.util.functional.Util.toS;
import static sp.it.util.ui.UtilKt.findParent;
import static sp.it.util.ui.UtilKt.onNodeDispose;
import static sp.it.util.ui.UtilKt.pseudoclass;
import static sp.it.util.ui.UtilKt.removeFromParent;

/**
 * Widget graphical component with a functionality.
 * <p/>
 * The functionality is handled by widget's {@link Controller}. The controller
 * is instantiated when widget loads. The widget-controller relationship is 1:1
 * and permanent.
 * <p/>
 * Widget can be thought of as a wrapper for controller (which may be used as
 * standalone object if implementation allows). The type of widget influences
 * the lifecycle.
 */
public final class Widget extends Component implements Configurable<Object>, Locatable {

	private static final Logger LOGGER = LoggerFactory.getLogger(Widget.class);
	private static final Set<String> ignoredConfigs = set("Is preferred", "Is ignored", "Custom name"); // avoids data duplication

	// Name of the widget. Permanent. Same as factory name. Used solely for deserialization (to find
	// appropriate factory)
	private final String name;

	/**
	 * Factory that produced this widget.
	 * <p/>
	 * Note that in case the application creates another version of the factory (e.g., when the
	 * widget source code has been modified and recompiled in runtime), even though the old factory
	 * will be removed from the list of factories (substituted by the new factory), this field will
	 * still point to the old factory, holding true to the description of this field.
	 */
	@Dependency("name - accessed using reflection by name")
	@XStreamOmitField public final WidgetFactory<?> factory;
	@XStreamOmitField protected Pane root;
	@XStreamOmitField protected Controller controller;
	@XStreamOmitField private HashMap<String,Config<Object>> configs = new HashMap<>();
	@XStreamOmitField Disposer onClose = new Disposer();

	// Temporary workaround for bad design. Widget-Container-Controller-Area relationship is badly
	// designed. This particular problem: Area can contain not yet loaded widget. Thus, we cant
	// use controller (null) to obtain area.
	//
	// I think this is the best and most painless way to wire widget with area & container (parent)
	// All the pseudo wiring through Controller is pure chaos.
	@XStreamOmitField public Container parentTemp;

	/**
	 * Graphics this widget is loaded in. It is responsibility of the caller of the {@link #load()} to set this
	 * field properly. There is no restriction where widget is loaded, so this field may be null.
	 * <p/>
	 * This field allows widget to control its lifecycle and context from its controller.
	 */
	@XStreamOmitField public ComponentUi uiTemp;

	/** Whether this widget will be preferred over other widgets in widget lookup. */
	@IsConfig(name = "Is preferred", info = "Prefer this widget among all widgets of its type. If there is a request "
			+ "for widget, preferred one will be selected. ")
	public final V<Boolean> preferred = new V<>(false);

	/** Whether whether this widget will not be included in widget lookup. */
	@IsConfig(name = "Is ignored", info = "Ignore this widget if there is a request.")
	public final V<Boolean> forbid_use = new V<>(false);

	/** Name displayed in gui. Customizable. Default is component type name. */
	@IsConfig(name = "Custom name", info = "Name displayed in gui. User can set his own. By default component type name.")
	public final V<String> custom_name = new V<>("");

	/** Whether this widget is active/focused. Each window has 0 or 1 active widgets. Default false. */
	@Dependency("name - accessed using reflection by name")
	@IsConfig(name = "Focused", info = "Whether widget is active/focused.", editable = EditMode.APP)
	@XStreamOmitField public final V<Boolean> focused = new V<>(false); // TODO: make read-only

	/**
	 * Whether this widget has been created from persisted state or normally by application/user.
	 * May be used for sensitive initialization, like setting default values that must not override user settings.
	 */
	@Dependency("name - accessed using reflection by name")
	@XStreamOmitField public final boolean isDeserialized = false;

	public Widget(String name, WidgetFactory<?> factory) {
		this.name = name;
		this.factory = factory;
		custom_name.setValue(name);
		focused.addListener(computeFocusChangeHandler());
	}

	@Override
	public String getName() {
		return name;
	}

	public Pane getGraphics() {
		return root;
	}

	/**
	 * Null if not loaded, otherwise never null - widgets can only reside within a {@link sp.it.pl.layout.container.Container}
	 * parent.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public Container<?> getParent() {
		return parentTemp;
	}

	/**
	 * Returns this widget's content.
	 * <p/>
	 * If called the 1st time, loads this widget's content and instantiates its
	 * controller. The controller will be null until the first time this method
	 * is called.
	 * Any subsequent call of this method will simply return the content.
	 * <p/>
	 * Details:
	 * <ul>
	 * <li> graphics and controller is created only once when needed and reused
	 * <li> attaching the graphics to the scenegraph will remove it from its old location
	 * <li> reattaching to scenegraph has no effect on widget state
	 * <li> serialisation is the only time when widget state (configs) need to be taken care of
	 * manually
	 * </ul>
	 * {@inheritDoc}
	 *
	 * @return graphical content of this widget
	 */
	@Override
	public Node load() {
		if (root==null) {
			controller = controller!=null ? controller : instantiateController();

			if (controller==null) {
				LoadErrorController c = new LoadErrorController(this);
				root = c.loadFirstTime();
				controller = c;
			} else {
				try {
					root = controller.loadFirstTime();
					// we cant call this as parent is injected after this method returns
					// in WidgetUi
					// lockedUnder.init();

					Class<?> cc = controller.getClass();
					boolean isLegacy = cc.isAnnotationPresent(LegacyController.class);
					if (isLegacy) restoreConfigs();

					updateIO();
				} catch (Throwable e) {
					LoadErrorController c = new LoadErrorController(this);
					root = c.loadFirstTime();
					controller = c;
					LOGGER.error("Widget={} graphics creation failed.", name, e);
				}
			}
		}

		root.getProperties().put("widget", this);
		return root;
	}

	public boolean isLoaded() {
		return root!=null;
	}

	@SuppressWarnings("unchecked")
	private Controller instantiateController() {
		restoreDefaultConfigs();

		// instantiate controller
		Class<Controller> cc = (Class) factory.getControllerType();
		LOGGER.info("Instantiating widget controller " + cc);
		Controller c = firstNotNull(
			() -> {
				try {
					Constructor<Controller> ccc = cc.getDeclaredConstructor(Widget.class);
					LOGGER.debug("Instantiating widget controller using 1 arg constructor");
					return ccc.newInstance(this);
				} catch (NoSuchMethodException e) {
					return null;
				} catch (IllegalAccessException|InstantiationException|InvocationTargetException e) {
					LOGGER.error("Instantiating widget controller failed {}", cc, e);
					return null;
				}
			}
		);

		return c;
	}

	/**
	 * Returns controller of the widget. It provides access to public behavior
	 * of the widget.
	 * <p/>
	 * The controller is instantiated when widget loads. The controller is null
	 * before that and this method should not be invoked.
	 * <p/>
	 * Do not check the output of this method for null! Receiving null implies
	 * wrong use of this method.
	 *
	 * @return controller of the widget or null if widget has not been loaded yet.
	 */
	public Controller getController() {
		return controller;
	}

	@Override
	public void close() {
		LOGGER.info("Widget=" + name + " closing");

		if (controller!=null) {
			Controller c = controller;
			controller = null;

			var is = c.io.i.getInputs();
			var os = c.io.o.getOutputs();
			IOLayer.allInputs.removeAll(is);
			IOLayer.allOutputs.removeAll(os);

			c.close();

			is.forEach(i -> i.setValue(null));
			os.forEach(o -> o.setValue(null));
		}

		if (root!=null) {
			removeFromParent(root);
			onNodeDispose(root);
			root = null;
		}

		ios.removeIf(io -> io.widget==this);

		onClose.invoke();
	}

	/** @return factory information about this widget */
	public WidgetInfo getInfo() {
		return factory;
	}

	@NotNull
	@Override
	public File getLocation() {
		return factory.getLocation();
	}

	@NotNull
	@Override
	public File getUserLocation() {
		return factory.getLocationUser();
	}

	/** Creates a launcher for this widget with default (no predefined) settings. */
	public void exportFxwlDefault(File f) {
		writeTextTry(f, name)
			.ifErrorUse(e -> LOGGER.error("Unable to export widget launcher for {} into {}", name, f, e));
	}

	/**
	 * Sets state of this widget to that of a target widget's.
	 *
	 * @param w widget to copy state from
	 */
	public void setStateFrom(Widget w) {
		// if widget was loaded we store its latest state, otherwise it contains serialized pme
		if (w.controller!=null)
			w.storeConfigs();

		properties.clear();
		properties.putAll(w.properties);

		Util.setField(this, "id", w.id); // (nasty cheat) not sure if this 100% required
		preferred.setValue(w.preferred.getValue());
		forbid_use.setValue(w.forbid_use.getValue());
		custom_name.setValue(w.custom_name.getValue());
		loadType.set(w.loadType.get());
		locked.set(w.locked.get());

		// if this widget is loaded we apply state, otherwise its done when it loads
		if (controller!=null)
			restoreConfigs();

		properties.entrySet().stream()
			.filter(e -> e.getKey().startsWith("io"))
			.map(e -> new IO(this, e.getKey().substring(2), (String) e.getValue()))
			.forEach(ios::add);

		if (controller!=null)
			updateIO();
	}

	@Override
	public void focus() {
		if (isLoaded()) {
			controller.focus();
		}
	}

	private ChangeListener<Boolean> computeFocusChangeHandler() {
		return (o, ov, nv) -> {
			if (isLoaded()) {
				Pane p = (Pane) findParent(root, n -> n.getStyleClass().contains(WidgetUi.STYLECLASS));
				if (p!=null) p.pseudoClassStateChanged(pseudoclass("active"), nv);
			}
		};
	}

/******************************************************************************/

	@Override
	public Config<Object> getField(String n) {
		return configs.computeIfAbsent(n, this::getFieldImpl);
	}

	@Override
	public Collection<Config<Object>> getFields() {
		getFieldsImpl().forEach(c -> configs.putIfAbsent(c.getName(), c));
		return configs.values();
	}

	private Collection<Config<Object>> getFieldsImpl() {
		var c = new ArrayList<>(Configurable.super.getFields());
		if (controller!=null) c.addAll(controller.getFields());
		return c;
	}

	private Config<Object> getFieldImpl(String name) {
		return firstNotNull(
			() -> Configurable.super.getField(name),
			() -> controller==null ? null : controller.getField(name)
		);
	}

	@SuppressWarnings("unchecked")
	public Map<String,PropVal> getFieldsRaw() {
		return (Map) properties.computeIfAbsent("configs", key -> new HashMap<>());
	}

/******************************************************************************/

	@Override
	public String toString() {
		return getClass() + " " + name;
	}

/****************************** SERIALIZATION *********************************/

	/** Invoked just before the serialization. */
	@SuppressWarnings("StatementWithEmptyBody")
	protected Object writeReplace() throws ObjectStreamException {
		boolean isLoaded = controller!=null;

		// Prepare input-outputs
		// If widget is loaded, we serialize inputs & outputs
		if (isLoaded) {
			getController().io.i.getInputs().forEach(i ->
					properties.put("io" + i.name, toS(i.getSources(), o -> o.id.toString(), ":"))
			);
			// Otherwise we still have the deserialized inputs/outputs leave them as they are
		} else {}

		// Prepare configs
		if (isLoaded) {
			storeConfigs(); // If widget is loaded, we serialize name:value pairs
		} else {
			// Otherwise we still have the deserialized name:value pairs and leave them as they are
		}

		return this;
	}

	/**
	 * Invoked just after deserialization.
	 *
	 * @implSpec Resolve object by initializing non-deserializable fields or providing an alternative instance (e.g. to
	 * adhere to singleton pattern).
	 */
	@SuppressWarnings("unchecked")
	protected Object readResolve() throws ObjectStreamException {
		super.readResolve();

		Util.setField(this, "isDeserialized", true);

		if (factory==null) {
			var f = firstNotNull(
				() -> APP.widgetManager.factories.getFactory(name),
				() -> new NoFactoryFactory(name)
			);
			Util.setField(this, "factory", f);
			if (f instanceof NoFactoryFactory) controller = ((NoFactoryFactory) f).createController(this);
		}

		if (configs==null) configs = new HashMap<>();
		if (focused==null) {
			Util.setField(this, "focused", new V<>(false));
			focused.addListener(computeFocusChangeHandler());
		}

		if (onClose==null) Util.setField(this, "onClose", new Disposer());

		// accumulate serialized inputs for later deserialization when all widgets are ready
		properties.entrySet().stream()
				.filter(e -> e.getKey().startsWith("io"))
				.map(e -> new IO(this, e.getKey().substring(2), (String) e.getValue()))
				.forEach(ios::add);

		return this;
	}

	public static Ƒ1<Config<?>,String> configToRawKeyMapper = it -> it.getName().replace(' ', '_').toLowerCase();

	private void storeConfigs() {
		// We store only Controller configs as configs of this widget should be defined in this
		// class as fields and serialized/deserialized automatically. Only Controller is created
		// manually, Widget configs should be independent and full auto...

		// 1) nothing to serialize
		// 2) serializing empty list could actually rewrite previously serialized properties,
		//    not yet restored due to widget not yet loaded
		// 3) easy optimization
		if (controller==null) return;

		var configsRaw = getFieldsRaw();
		controller.getFields().forEach(c -> configsRaw.put(configToRawKeyMapper.apply(c), c.getValueAsProperty()));
	}

	private void restoreConfigs() {
		var configsRaw = getFieldsRaw();
		if (!configsRaw.isEmpty()) {
			getFields().forEach(c -> {
				String key = configToRawKeyMapper.apply(c);
				if (configsRaw.containsKey(key)) {
					c.setValueAsProperty(configsRaw.get(key));
				}
			});

			configsRaw.clear(); // restoration can only ever happen once
		}
	}

	public void storeDefaultConfigs() {
		if (!isLoaded()) throw new AssertionError("Must be loaded to export default configs");

		@SuppressWarnings("RedundantCast")
		Predicate<Config<Object>> nonDefault = f -> ((Class) f.getType())==ObservableList.class || !deepEquals(f.getValue(), f.getDefaultValue());
		File configFile = new File(getUserLocation(), "default.properties");
		Configuration configuration = new Configuration(configToRawKeyMapper);
		configuration.collect(filter(getFields(), nonDefault));
		configuration.save("Custom default widget settings", configFile);
	}

	private void restoreDefaultConfigs() {
		File configFile = new File(getUserLocation(), "default.properties");
		if (configFile.exists()) {
			var configsRaw = getFieldsRaw();
			readProperties(configFile).ifOkUse(it -> it.forEach(configsRaw::putIfAbsent));
		}
	}

	public void clearDefaultConfigs() {
		File configFile = new File(getUserLocation(), "default.properties");
		deleteRecursively(configFile);
	}

	/******************************************************************************/

	static class IO {
		public final Widget widget;
		public final String input_name;
		public final List<Output.Id> outputs_ids = new ArrayList<>();

		IO(Widget widget, String name, String outputs) {
			this.widget = widget;
			this.input_name = name;
			this.outputs_ids.addAll(map(split(outputs, ":", x -> x), Output.Id::fromString));
		}
	}

	static final ArrayList<IO> ios = new ArrayList<>();

	@SuppressWarnings({"unchecked", "UseBulkOperation", "deprecation"})
	public static void deserializeWidgetIO() {
		Set<Input<?>> is = new HashSet<>();
		Map<Output.Id,Output<?>> os = new HashMap<>();
		APP.widgetManager.widgets.findAll(OPEN)
			.filter(w -> w.controller!=null)
			.forEach(w -> {
				w.controller.io.i.getInputs().forEach(is::add);
				w.controller.io.o.getOutputs().forEach(o -> os.put(o.id, o));
			});
		IOLayer.allInoutputs.forEach(io -> os.put(io.o.id, io.o));

		ios.forEach(io -> {
			if (io.widget.controller==null) return;
			Input i = io.widget.controller.io.i.getInputRaw(io.input_name);
			if (i==null) return;
			io.outputs_ids.stream().map(os::get).filter(ISNTØ).forEach(i::bind);
		});

		IOLayer.allInoutputs.forEach(io -> is.remove(io.i));
		IOLayer.allInoutputs.forEach(io -> os.remove(io.o.id));
		IOLayer.allInputs.addAll(is);
		IOLayer.allOutputs.addAll(os.values());
	}

	// called when widget is loaded/closed (or rather, when inputs or outputs are created/removed)
	// we need to create i/o nodes and i/o connections
	private void updateIO() {
		// because widget inputs can be bound to other widget outputs, and because widgets can be
		// loaded passively (then its i/o does not exists yet), we need to update all widget i/os
		// because we do not know which bind to this widget
		IOLayer.allInputs.addAll(controller.io.i.getInputs());
		IOLayer.allOutputs.addAll(controller.io.o.getOutputs());
		deserializeWidgetIO();
	}

	/** Widget metadata. Passed from code to program. Use on controller class. */
	@Retention(value = RUNTIME)
	@Target(value = TYPE)
	public @interface Info {

		/**
		 * Name of the widget. "" by default.
		 */
		String name() default "";

		/**
		 * Description of the widget.
		 */
		String description() default "";

		/**
		 * Version of the widget
		 */
		String version() default "";

		/**
		 * Author of the widget
		 */
		String author() default "";

		/**
		 * Co-developer of the widget.
		 */
		String contributor() default "";

		/**
		 * Last time of change.
		 */
		String year() default "";

		/**
		 * How to use text.
		 * <pre>
		 * For example:
		 * "Available actions:\n" +
		 * "    Drag cover away : Removes cover\n" +
		 * "    Drop image file : Adds cover\n" +
		 * "    Drop audio files : Adds files to tagger\n" +
		 * "    Write : Saves the tags\n" +
		 * "    Open list of tagged items"
		 * </pre>
		 */
		String howto() default "";

		/**
		 * Any words from the author, generally about the intention behind or bugs
		 * or plans for the widget or simply unrelated to anything else information.
		 * <p/>
		 * For example: "To do: simplify user interface." or: "Discontinued."
		 */
		String notes() default "";

		/**
		 * Group the widget should categorize under as. Default {@link Widget.Group#UNKNOWN}
		 */
		Widget.Group group() default Widget.Group.UNKNOWN;
	}

	/** Widget's intended functionality. */
	public enum Group {
		APP,
		PLAYBACK,
		PLAYLIST,
		TAGGER,
		LIBRARY,
		VISUALISATION,
		OTHER,
		DEVELOPMENT,
		UNKNOWN
	}

	public enum LoadType {
		AUTOMATIC, MANUAL
	}

}