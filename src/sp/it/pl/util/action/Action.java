package sp.it.pl.util.action;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Pair;
import sp.it.pl.util.access.V;
import sp.it.pl.util.collections.mapset.MapSet;
import sp.it.pl.util.conf.Config;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.conf.IsConfig.EditMode;
import sp.it.pl.util.conf.IsConfigurable;
import sp.it.pl.util.hotkey.Hotkeys;
import sp.it.pl.util.system.Os;
import sp.it.pl.util.validation.Constraint;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.stream.Collectors.toCollection;
import static javafx.scene.input.KeyCode.ALT_GRAPH;
import static javafx.scene.input.KeyCombination.NO_MATCH;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.dev.Util.logger;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.functional.Util.setRO;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.reactive.Util.doOnceIfNonNull;
import static sp.it.pl.util.reactive.Util.listChangeHandlerEach;
import static sp.it.pl.util.system.EnvironmentKt.runCommand;
import static sp.it.pl.util.type.Util.getAnnotated;

/**
 * Behavior with a name and possible shortcut.
 * <p/>
 * An action can wrap any {@link Runnable}. The aim however is to allow a
 * framework make convenient externalization of application behavior possible, like using {@link IsAction} on methods
 * to obtain an action that invokes them. Example of use for action is generating shortcuts for the application.
 * <p/>
 * Action is also {@link Config} so it can be configured and serialized.
 */
@IsConfigurable(Action.CONFIG_GROUP)
public final class Action extends Config<Action> implements Runnable {

	/** Action that does nothing. Use where null inappropriate. */
	public static final Action EMPTY = new Action("None", () -> {}, "Action that does nothing", "", "", false, false);
	public static final String CONFIG_GROUP = "Shortcuts";

	private final String name;
	private final Runnable action;
	private final String info;
	private final String group;
	private final boolean continuous;
	private boolean global;
	private KeyCombination keys = KeyCombination.NO_MATCH;
	private final String defaultKeys;
	private final boolean defaultGlobal;

	private Action(IsAction a, String group, Runnable action) {
		this(a.name(), action, a.desc(), group, a.keys(), a.global(), a.repeat());
	}

	public Action(String name, Runnable action, String info, String group, String keys) {
		this(name, action, info, group, keys, false, false);
	}

	/**
	 * Creates new action.
	 *
	 * @param name action name. Must be be unique for each action. Also human readable.
	 * @param action Code that gets executed on {@link #run()}
	 * @param info value for the final info property
	 * @param keys Key combination for activating this action as a hotkey. See {@link #setKeys(java.lang.String)}
	 * @param global value for the global property
	 * @param continuous value for the final continuous property
	 */
	public Action(String name, Runnable action, String info, String group, String keys, boolean global, boolean continuous) {
		this.name = name;
		this.action = action;
		this.info = info==null || info.isEmpty() ? name : info;
		this.group = group.isEmpty() ? CONFIG_GROUP : group;
		this.continuous = continuous;
		this.global = global;
		this.defaultGlobal = global;
		this.defaultKeys = keys;
		changeKeys(keys);
	}

	/**
	 * Global action has broader activation limit. For example global shortcut
	 * doesn't require application to be focused. This value denotes the global
	 * attribute for the resulting action
	 * Default false.
	 *
	 * @return whether the action is global
	 */
	public boolean isGlobal() {
		return global;
	}

	/**
	 * @return whether the action should be run repeatedly while the hotkey is pressed or once.
	 */
	public boolean isContinuous() {
		return continuous;
	}

	/**
	 * Returns the key combination for activating this action as a hotkey.
	 * The output of this method is always valid parsable string for method
	 * {@link #setKeys(java.lang.String)}. Use to assign keys of this action to
	 * another action or to get the keys as human readable String.
	 *
	 * @return the key combination for shortcut of this action or "" if no valid value.
	 */
	public String getKeys() {
		String s = keys.getName();
		s = s.replaceAll("'", "");      // we need to replace ''' characters
		s = s.replaceAll(" ", "_");     // we need to replace ' ' characters
		return s;
	}

	/**
	 * Returns the key combination for activating this action as a hotkey.
	 * Alternative to {@link #getKeys()} with more friendly output. Use when
	 * the alternative is not satisfactory.
	 *
	 * @return the keys or KeyCombination.NO_MATCH if no valid value
	 */
	public KeyCombination getKeyCombination() {
		return keys;
	}

	/**
	 * When the parameter is not valid parsable hotkey string, the hotkey will
	 * not be able to be registered and used.
	 *
	 * @return true iff the keys for this action's hotkey have valid value and can be registered.
	 */
	public boolean hasKeysAssigned() {
		return keys!=NO_MATCH;
	}

	/** Set whether this action is global. */
	public void setGlobal(boolean global) {
		unregister();
		this.global = global;
		register();
	}

	/**
	 * Change and apply key combination.
	 *
	 * @param key_combination Case does not matter. <pre>
	 * For example: "CTRL+A", "F6", "D", "ALT+SHIFT+\"
	 * </pre>
	 * Incorrect keys will be substituted with "", which is equivalent to deactivating the shortcut.
	 * <p/>
	 * To check the result of the assignment of the keys use {@link #getKeys()} or {@link #hasKeysAssigned()} method.
	 */
	public void setKeys(String key_combination) {
		unregister();
		changeKeys(key_combination);
		register();
	}

	/**
	 * Set keys and scope of this action atomically.
	 * See {@link #setGlobal(boolean)} and {@link #setKeys(java.lang.String)}.
	 */
	public void set(boolean global, String key_combination) {
		unregister();
		this.global = global;
		changeKeys(key_combination);
		register();
	}

	public void setDefault() {
		set(defaultGlobal, defaultKeys);
	}

	/**
	 * Execute the action. Always executes on application thread.
	 */
	@Override
	public void run() {
		runFX(() -> {
			logger(Action.class).info("Executing action {}", name);
			action.run();
			APP.actionStream.push(name);
		});
	}

	/**
	 * Activates shortcut. Only registered shortcuts can be invoked.
	 * <p/>
	 * If the {@link #hasKeysAssigned()} returns false, registration will not
	 * take place.
	 * <p/>
	 * For local action this method will succeed only after {@link Scene} is
	 * already initialized.
	 * For global, platform support is required. If it is not, shortcut will
	 * be registered locally, although the action will remain global.
	 * <p/>
	 * Note that shortcut can be registered globally multiple times even with
	 * the same keys and locally with different keys. Make sure the action is
	 * unregistered before registering it.
	 */
	public void register() {
		if (!hasKeysAssigned()) return;

		boolean can_be_global = global && globalShortcuts.getValue() && isGlobalShortcutsSupported();
		if (can_be_global) registerGlobal();
		else registerLocal();
	}

	public void unregister() {
		// unregister both local and global to prevent illegal states
		if (isGlobalShortcutsSupported()) unregisterGlobal();
		unregisterLocal();
	}

	/*********************** helper methods ***************************/

	private void changeKeys(String keys) {
		if (keys.isEmpty()) {
			this.keys = NO_MATCH;   // disable shortcut for empty keys
		} else {
			try {
				this.keys = KeyCombination.keyCombination(keys);
			} catch (Exception e) {
				logger(Action.class).warn("Illegal shortcut keys parameter. Shortcut {} disabled. Keys: {}", name, keys, e);
				this.keys = NO_MATCH;   // disable shortcut for wrong keys
			}
		}
	}

	private void registerLocal() {
		if (!isActionListening()) return; // make sure there is no illegal state

		KeyCombination k = getKeysForLocalRegistering();
//        Stage.getWindows().stream().map(Window::getScene).forEach(this::registerInScene);
		// register for each app window separately
		for (Window w : Stage.getWindows())
			if (w.getScene()!=null)
				w.getScene().getAccelerators().put(k, this);
	}

	private void unregisterLocal() {
		KeyCombination k = getKeysForLocalRegistering();
		// unregister for each app window separately
//        Stage.getWindows().stream().map(Window::getScene).forEach(this::registerInScene);
		for (Window w : Stage.getWindows())
			if (w.getScene()!=null)
				w.getScene().getAccelerators().remove(k);
	}

	private void registerInScene(Scene s) {
		if (!isActionListening()) return; // make sure there is no illegal state
		s.getAccelerators().put(getKeysForLocalRegistering(), this);
	}

	private void unregisterInScene(Scene s) {
		if (s==null) return;
		s.getAccelerators().remove(getKeysForLocalRegistering());
	}

	private void registerGlobal() {
		if (!isActionListening()) return; // make sure there is no illegal state
		hotkeys.register(this, getKeys());
	}

	private void unregisterGlobal() {
		hotkeys.unregister(this);
	}

	public int getID() {
		return idOf(name);
	}

	private KeyCombination getKeysForLocalRegistering() {
		// fix local shortcut problem - keyCodes not registering, needs raw characters instead
		// TODO resolve or include all characters' conversions
		String s = getKeys();
		if (s.contains("Back_Slash"))
			return KeyCombination.keyCombination(s.replace("Back_Slash", "\\"));
		else if (s.contains("Back_Quote"))
			return KeyCombination.keyCombination(s.replace("Back_Quote", "`"));
		else
			return keys;
	}

/* ---------- AS CONFIG --------------------------------------------------------------------------------------------- */

	@Override
	public Action getValue() {
		return this;
	}

	@Override
	public void setValue(Action val) {
		set(val.isGlobal(), val.getKeys());
	}

	@Override
	public void applyValue(Action val) {
		register();
	}

	@Override
	public Class<Action> getType() {
		return Action.class;
	}

	@Override
	public Action getDefaultValue() {
		return new Action(name, action, info, group, defaultKeys, defaultGlobal, continuous);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getGuiName() {
		return name;
	}

	@Override
	public String getInfo() {
		return info;
	}

	@Override
	public String getGroup() {
		return group;
	}

	@Override
	public EditMode isEditable() {
		return EditMode.USER;
	}

	@Override
	public Set<Constraint<? super Action>> getConstraints() {
		return setRO();
	}

	@SafeVarargs
	@Override
	public final Config<Action> constraints(Constraint<? super Action>... constraints) {
		return this;
	}

/* ---------- AS OBJECT --------------------------------------------------------------------------------------------- */

	@Override
	public boolean equals(Object o) {
		if (this==o) return true;

		if (!(o instanceof Action)) return false;
		Action a = (Action) o;
		// we will compare all fields that can change (global & keys)
		// for all the rest only one (name) is necessary because they go
		// with each other
		// name is basically a unique identifier so this should be enough
		return a.name.equals(name) && a.global==global && a.keys.equals(keys);
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 41*hash + Objects.hashCode(this.name);
		hash = 41*hash + (this.global ? 1 : 0);
		hash = 41*hash + Objects.hashCode(this.keys);
		return hash;
	}

	@Deprecated // internal use only
	private Action(boolean isGlobal, KeyCombination keys) {
		this.name = null;
		this.action = null;
		this.info = null;
		this.group = null;
		this.continuous = false;
		this.global = isGlobal;
		this.keys = keys;
		this.defaultGlobal = isGlobal;
		this.defaultKeys = getKeys();
	}

	// TODO: remove
	@Deprecated(forRemoval = true)
	public static Action fromString(String str) {
		int i = str.lastIndexOf(",");
		if (i==-1) return null;
		String s1 = str.substring(0, i);
		String s2 = str.substring(i + 1, str.length());
		boolean isGlobal = Boolean.parseBoolean(s1);
		KeyCombination keys = s2.isEmpty() ? KeyCombination.NO_MATCH : KeyCodeCombination.valueOf(s2);
		return new Action(isGlobal, keys);
	}

	// internal use
	@Deprecated
	static Action from(Action a, String str) {
		Action tmp = fromString(str);
		if (tmp!=null) {
			a.global = tmp.global;
			a.keys = tmp.keys;
		}
		return a;
	}

	@Override
	public String toString() {
		return global + "," + getKeys();
	}

/* ---------- SHORTCUT HANDLING ON APP LEVEL ------------------------------------------------------------------------ */

	@SuppressWarnings("FieldCanBeLocal")
	@IsConfig(name = "Global shortcuts supported", editable = EditMode.NONE, info = "Whether global shortcuts are supported on this system")
	private static final boolean isGlobalShortcutsSupported = true;
	@IsConfig(name = "Media shortcuts supported", editable = EditMode.NONE, info = "Whether media shortcuts are supported on this system")
	private static final boolean isMedialShortcutsSupported = true;
	private static boolean isRunning = false;
	private static Hotkeys hotkeys = isGlobalShortcutsSupported() ? new Hotkeys(Platform::runLater) : null;

	/**
	 * Activates listening process for hotkeys. Not running this method will cause hotkeys to not
	 * get invoked.
	 * Must not be ran more than once.
	 * Does nothing if not supported.
	 *
	 * @throws IllegalStateException if ran more than once without calling {@link #stopActionListening()}
	 */
	public static void startActionListening() {
		if (isRunning) throw new IllegalStateException("Action listening already running");
		startLocalListening();
		if (isGlobalShortcutsSupported() && globalShortcuts.get())
			startGlobalListening();
		isRunning = true;
	}

	/**
	 * Deactivates listening process for hotkeys (global and local), causing them to stop working.
	 * Frees resources. This method should should always be ran when {@link #startActionListening()}
	 * was invoked. Not doing so may prevent the application from closing successfully, due to non
	 * daemon thread involved here.
	 */
	public static void stopActionListening() {
		stopLocalListening();
		if(isGlobalShortcutsSupported())
			stopGlobalListening();
		isRunning = false;
	}

	/** @return whether the action listening is running */
	public static boolean isActionListening() {
		return isRunning;
	}

/* ---------- HELPER METHODS ---------------------------------------------------------------------------------------- */

	private static final ListChangeListener<Window> localActionRegisterer = listChangeHandlerEach(
			window -> doOnceIfNonNull(window.sceneProperty(),
					scene -> getActions().stream().filter(a -> !a.isGlobal()).forEach(a -> a.registerInScene(scene))),
			window -> {
				Scene scene = window.getScene();
				if (scene!=null)
					getActions().stream().filter(a -> !a.isGlobal()).forEach(a -> a.unregisterInScene(scene));
			}
	);

	/** Activates listening process for local hotkeys. */
	private static void startLocalListening() {
		// Normally we first register for all visible windows.
		// But its handled when applying the action as a Config.
		// Stage.getWindows().forEach(window -> executeWhenNonNull(window.sceneProperty(), scene -> getActions().forEach(a -> a.registerInScene(scene))));

		// keep registering when new windows are showed
		Stage.getWindows().addListener(localActionRegisterer);

		// Normally, we should also observe Actions and register/unregister on add/remove or we effectively
		// support only pre-created actions.
		// But its handled when applying the action as a Config.
	}

	/**
	 * Deactivates listening process for local hotkeys.
	 */
	private static void stopLocalListening() {
		Stage.getWindows().removeListener(localActionRegisterer);
		Stage.getWindows().forEach(window -> {
			Scene scene = window.getScene();
			if (scene!=null)
				Action.getActions().forEach(a -> a.unregisterInScene(scene));
		});
	}

	/**
	 * Activates listening process for global hotkeys. Not running this method
	 * will cause registered global hotkeys to not get invoked. Use once when
	 * application initializes.
	 * Does nothing if not supported.
	 */
	private static void startGlobalListening() {
		hotkeys.start();
	}

	/**
	 * Deactivates listening process for global hotkeys. Frees resources. This
	 * method should should always be ran at the end of application's life cycle
	 * if {@link #startGlobalListening()} ()} was invoked at least once.
	 * Not doing so might prevent from the application to close successfully,
	 * because bgr listening thread will not close.
	 */
	private static void stopGlobalListening() {
		hotkeys.stop();
	}

	/**
	 * Returns true if global shortcuts are supported at running platform.
	 * Otherwise false. In such case, global shortcuts will run as local and
	 * {@link #startGlobalListening()} and {@link #stopGlobalListening()} will
	 * have no effect.
	 */
	public static boolean isGlobalShortcutsSupported() {
		return isGlobalShortcutsSupported;
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/**
	 * Returns modifiable collection of all actions mapped by their name. Actions
	 * can be added and removed, which modifies the underlying collection.
	 *
	 * @return all actions.
	 */
	public static Collection<Action> getActions() {
		return actions;
	}

	public static Action get(int id) {
		Action a = actions.get(id);
		if (a==null) throw new IllegalArgumentException("No such action: '" + id + "'. Make sure the action is " +
				"declared and annotation processing is enabled and functioning properly.");
		return a;
	}

	/**
	 * Returns the action with specified name or throws an exception. It is a programmatic error if an action does
	 * not exist.
	 *
	 * @param name name of the action
	 * @return action. Never null.
	 * @throws IllegalArgumentException if no such action
	 */
	public static Action get(String name) {
		Action a = actions.get(idOf(name));
		if (a==null) throw new IllegalArgumentException("No such action: '" + name + "'. Make sure the action is " +
				"declared and annotation processing is enabled and functioning properly.");
		return a;
	}

	// Guarantees consistency
	private static int idOf(String actionName) {
		return actionName.hashCode();
	}

	private static final MapSet<Integer,Action> actions = new MapSet<>(new ConcurrentHashMap<>(), Action::getID) {{
		getAnnotated(IsActionable.class).flatMap(type -> gatherActions(type, null)).forEach(this::add);
		add(EMPTY);
	}};

	public static void installActions(Object... os) {
		stream(os)
				.flatMap(o -> {
					if (o instanceof Stream) return (Stream<?>) o;
					if (o instanceof Optional) return ((Optional<?>) o).stream();
					if (o instanceof Object[]) return stream(((Object[]) o));
					if (o instanceof Collection) return Stream.concat(Stream.of(o), ((Collection<?>)o).stream());
					return stream(o);
				})
				.forEach(o -> gatherActions(o).forEach(actions::add));
	}

	public static Stream<Action> gatherActions(Object object) {
		return gatherActions(object.getClass(), object);
	}

	private static <T> Stream<Action> gatherActions(Class<? extends T> type, T instance) {
		boolean findInstance = instance!=null;
		Lookup method_lookup = MethodHandles.lookup();
		return stream(type.getDeclaredMethods())
				.map(m -> new Pair<>(m, isStatic(m.getModifiers())))
				.filter(m -> findInstance^m.getValue())
				.flatMap(m -> stream(m.getKey().getAnnotationsByType(IsAction.class))
						.map(a -> {
							if (m.getKey().getParameters().length>0)
								throw new RuntimeException("Action Method must have 0 parameters!");

							String group = getActionGroup(type);
							MethodHandle mh;
							try {
								m.getKey().setAccessible(true);
								mh = method_lookup.unreflect(m.getKey());
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}

							Runnable r = () -> {
								try {
									if (m.getValue()) mh.invokeExact();
									else mh.invoke(instance);
								} catch (Throwable e) {
									throw new RuntimeException("Error during running action", e);
								}
							};
							return new Action(a, group, r);
						})
				);
	}

	public static void loadCommandActions() {
		// discover all command actions defined in file
		File file = new File(APP.DIR_USERDATA, "command-actions.cfg");
		// TODO: improve to side effect free code
		long count = !file.exists() ? 0 : APP.serializerXml.fromXML(Commands.class, file)
			.getOrSupply(Commands::new)
			.stream()
			.filter(a -> a.isEnabled)
			.map(Command::toAction)
			.peek(Action::register)
			.peek(actions::add)
			.count();
		// Generate default template for the user if necessary (shows how to define commands).
		// Note we must not overwrite existing file, possibly containing already defined commands, hence the
		// file.exists() check. The number of deserialized commands can be 0 if deserialization fails for some reason
		boolean generateTemplate = count<1 && !file.exists();
		if (generateTemplate)
			APP.serializerXml.toXML(stream(new Command()).collect(toCollection(Commands::new)), file);
	}

	private static class Commands extends HashSet<Command> {}

	private static class Command {
		public String name = "";
		public String keys = "";
		public String command = "";
		public boolean isGlobal = false;
		public boolean isAppCommand = true;
		public boolean isEnabled = true;

		public Action toAction() {
			return new Action(
					name,
					isAppCommand ? () -> APP.parameterProcessor.process(list(command)) : () -> runCommand(command),
					isAppCommand ? "Runs app command '" + command + "'" : "Runs system command '" + command + "'",
					isAppCommand ? "Shortcuts.command.app" : "Shortcuts.command.system",
					keys, isGlobal, false
			);
		}
	}

	private static String getActionGroup(Class<?> c) {
		IsConfigurable ac = c.getAnnotation(IsConfigurable.class);
		if (ac!=null && !ac.value().isEmpty())
			return ac.value();

		IsActionable aa = c.getAnnotation(IsActionable.class);
		return aa==null || aa.value().isEmpty() ? c.getSimpleName() : aa.value();
	}

/* ---------- CONFIGURATION ----------------------------------------------------------------------------------------- */

	@IsConfig(name = "Global shortcuts enabled", info = "Allows using the shortcuts even if application is not focused.")
	public static final V<Boolean> globalShortcuts = new V<>(true, v -> {
		if (isGlobalShortcutsSupported()) {
			if (v) {
				startGlobalListening();
				// re-register shortcuts to switch from local
				getActions().forEach(a -> {
					a.unregister();
					a.register();
				});
			} else {
				stopGlobalListening();
				// re-register shortcuts to switch to local
				getActions().forEach(a -> {
					a.unregister();
					a.register();
				});
			}
		}
	});

	@IsConfig(name = "Media shortcuts enabled", info = "Allows using shortcuts for media keys on the keyboard.")
	public static final V<Boolean> globalMediaShortcuts = new V<>(true);

//    @IsConfig(name = "Allow in-app shortcuts", info = "Allows using standard shortcuts.", group = "Shortcuts")
//    public static final V<Boolean> local_shortcuts = new V<>(true, v -> {
//        if (isLocalShortcutsSupported()) {
//            if (v){
//            } else {
//
//            }
//        }
//    });

	@IsConfig(name = "Manage Layout (fast) Shortcut", info = "Enables layout management mode.")
	public static KeyCode Shortcut_ALTERNATE = ALT_GRAPH;

	@IsConfig(name = "Collapse layout", info = "Collapses focused container within layout.", editable = EditMode.NONE)
	public static final String Shortcut_COLAPSE = "Shift+C";

}