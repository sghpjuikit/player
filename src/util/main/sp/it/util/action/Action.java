package sp.it.util.action;

import java.util.Objects;
import java.util.Set;
import javafx.scene.Scene;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.Window;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import sp.it.util.conf.Config;
import sp.it.util.conf.EditMode;
import sp.it.util.validation.Constraint;
import static javafx.scene.input.KeyCombination.NO_MATCH;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.dev.DebugKt.logger;
import static sp.it.util.functional.Util.setRO;

/**
 * Behavior with a name and possible shortcut.
 * <p/>
 * An action can wrap any {@link Runnable}. The aim however is to allow a
 * framework make convenient externalization of application behavior possible, like using {@link IsAction} on methods
 * to obtain an action that invokes them. Example of use for action is generating shortcuts for the application.
 * <p/>
 * Action is also {@link Config} so it can be configured and serialized.
 */
public class Action extends Config<Action> implements Runnable, Function0<Unit> {

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

	public Action(IsAction a, String group, Runnable action) {
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
			ActionManager.INSTANCE.getOnActionRunPre().invoke(this);
			action.run();
			ActionManager.INSTANCE.getOnActionRunPost().invoke(this);
		});
	}

	@Override
	public Unit invoke() {
		run();
		return Unit.INSTANCE;
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

		boolean isGlobal = global && ActionManager.INSTANCE.getGlobalShortcutsEnabled().getValue();
		if (isGlobal) registerGlobal();
		else registerLocal();
	}

	public void unregister() {
		// unregister both local and global to prevent illegal states
		if (ActionManager.INSTANCE.isGlobalShortcutsSupported()) unregisterGlobal();
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
		if (!ActionManager.INSTANCE.isActionListening()) return; // make sure there is no illegal state

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

	void registerInScene(Scene s) {
		if (!ActionManager.INSTANCE.isActionListening()) return; // make sure there is no illegal state
		s.getAccelerators().put(getKeysForLocalRegistering(), this);
	}

	void unregisterInScene(Scene s) {
		if (s==null) return;
		s.getAccelerators().remove(getKeysForLocalRegistering());
	}

	private void registerGlobal() {
		if (!ActionManager.INSTANCE.isActionListening()) return; // make sure there is no illegal state
		ActionRegistrar.INSTANCE.getHotkeys().register(this, getKeys());
	}

	private void unregisterGlobal() {
		ActionRegistrar.INSTANCE.getHotkeys().unregister(this);
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

	@Deprecated // internal use
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

}