package sp.it.util.action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javafx.scene.Scene;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.Window;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import sp.it.util.conf.Config;
import sp.it.util.conf.Constraint;
import sp.it.util.conf.EditMode;
import sp.it.util.file.json.JsString;
import sp.it.util.file.json.JsValue;
import sp.it.util.functional.TryKt;
import sp.it.util.type.VType;
import static javafx.scene.input.KeyCombination.NO_MATCH;
import static javafx.scene.input.KeyCombination.keyCombination;
import static kotlin.text.StringsKt.contains;
import static kotlin.text.StringsKt.replace;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.dev.DebugKt.logger;
import static sp.it.util.functional.TryKt.getOr;
import static sp.it.util.functional.TryKt.runTry;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.UtilKt.runnable;

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
	public static final Action NONE = new Action("None", () -> {}, "Action that does nothing", "", "", false, false);
	public static final String CONFIG_GROUP = "Shortcuts";

	public final String name;
	public final Runnable action;
	public final String info;
	public final String group;
	public final boolean continuous;
	public boolean global;
	private KeyCombination keys = NO_MATCH;
	private final String defaultKeys;
	private final boolean defaultGlobal;
	private Set<Constraint<Action>> constraints = null;

	public Action(IsAction a, String group, Runnable action) {
		this(a.name(), action, a.info(), group, a.keys(), a.global(), a.repeat());
	}

	public Action(String name, Runnable action, String info, String group, String keys) {
		this(name, action, info, group, keys, false, false);
	}

	@SuppressWarnings("unchecked")
	public Action(String name, Runnable action, String info, String group, String keys, boolean global, boolean continuous) {
		this(name, action, info, group, keys, global, continuous, new ArrayList<Constraint<Action>>().toArray(new Constraint[]{}));
	}

	/**
	 * Creates new action.
	 *
	 * @param name action name. Must be unique for each action. Also, human-readable.
	 * @param action Code that gets executed on {@link #run()}
	 * @param info value for the final info property
	 * @param keys Key combination for activating this action as a hotkey. See {@link #setKeys(java.lang.String)}
	 * @param global value for the global property
	 * @param continuous value for the final continuous property
	 */
	@SafeVarargs
	public Action(String name, Runnable action, String info, String group, String keys, boolean global, boolean continuous, Constraint<? super Action>... constraints) {
		this.name = name;
		this.action = action;
		this.info = info==null || info.isEmpty() ? name : info;
		this.group = group==null || group.isEmpty() ? CONFIG_GROUP : group;
		this.continuous = continuous;
		this.global = global;
		this.defaultGlobal = global;
		this.defaultKeys = keys;
		changeKeys(keys);
		addConstraints(constraints);
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
	 * {@link #setKeys(java.lang.String)}. Use it to assign keys of this action to
	 * another action or to get the keys as human-readable String.
	 *
	 * @return the key combination for shortcut of this action or "" if no valid value.
	 */
	public String getKeys() {
		if (keys==NO_MATCH) return "";
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
			ActionManager.INSTANCE.getOnActionRunPre().invoke(this);
			action.run();
			ActionManager.INSTANCE.getOnActionRunPost().invoke(this);
		});
	}

	@Override
	public @NotNull Unit invoke() {
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
				this.keys = keyCombination(keys);
			} catch (Exception e) {
				logger(Action.class).warn(e, () -> "Illegal shortcut keys parameter. Shortcut " + name + " disabled. Keys: " + keys);
				this.keys = NO_MATCH;   // disable shortcut for wrong keys
			}
		}
	}

	private void registerLocal() {
		if (!ActionManager.INSTANCE.isActionListening()) return;
		var k = getKeysForLocalRegistering();
		if (k!=NO_MATCH)
			for (Window w : Stage.getWindows())
				if (w.getScene()!=null)
					w.getScene().getAccelerators().put(k, this);
	}

	private void unregisterLocal() {
		var k = getKeysForLocalRegistering();
		for (Window w : Stage.getWindows())
			if (w.getScene()!=null)
				w.getScene().getAccelerators().remove(k);
	}

	void registerInScene(Scene s) {
		if (!ActionManager.INSTANCE.isActionListening()) return;
		var k = getKeysForLocalRegistering();
		if (k!=NO_MATCH)
			s.getAccelerators().put(getKeysForLocalRegistering(), this);
	}

	void unregisterInScene(Scene s) {
		if (s==null) return;
		s.getAccelerators().remove(getKeysForLocalRegistering());
	}

	private void registerGlobal() {
		if (!ActionManager.INSTANCE.isActionListening()) return;
		ActionRegistrar.INSTANCE.getHotkeys().register(this, getKeys());
	}

	private void unregisterGlobal() {
		ActionRegistrar.INSTANCE.getHotkeys().unregister(this);
	}

	private KeyCombination getKeysForLocalRegistering() {
		// fix local shortcut problem - keyCodes not registering, needs raw characters instead
		// TODO resolve or include all characters' conversions
		String s = getKeys();
		if (contains(s, "BACK_SLASH", true))
			return keyCombination(replace(s, "BACK_SLASH", "\\", true));
		else if (contains(s, "BACK_QUOTE", true))
			return keyCombination(replace(s, "BACK_QUOTE", "`", true));
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

	@NotNull
	@Override
	public JsValue getValueAsJson() {
		return new JsString(ActionDb.Companion.toS(new ActionDb(global, getKeys())));
	}

	@Override
	public void setValueAsJson(@NotNull JsValue property) {
		runTry(() -> {
			var s = property.asJsStringValue();
			var a = s==null ? null : getOr(ActionDb.Companion.ofS(s).ifErrorUse(it -> logger(Action.class).warn(() -> "Unable to set config=" + name + " value from text='" + s + "' because: " + it)), null);
			if (a!=null) set(a.isGlobal(), a.getKeys());
			return a;
		}).ifErrorUse(e ->
			logger(Action.class).warn(() -> "Unable to set config=" + name + " value from json=" + property)
		);
	}

	@Override
	public @NotNull VType<Action> getType() {
		return new VType<>(Action.class, false);
	}

	@Override
	public @NotNull Action getDefaultValue() {
		return new Action(name, action, info, group, defaultKeys, defaultGlobal, continuous);
	}

	@Override
	public @NotNull String getName() {
		return name;
	}

	@Override
	public @NotNull String getNameUi() {
		return name;
	}

	@Override
	public @NotNull String getInfo() {
		return info;
	}

	@Override
	public @NotNull String getGroup() {
		return group;
	}

	@Override
	public @NotNull EditMode isEditable() {
		return EditMode.USER;
	}

	@Override
	public @NotNull Set<Constraint<Action>> getConstraints() {
		return constraints==null ? Set.of() : constraints;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@SafeVarargs
	@Override
	public final @NotNull Config<Action> addConstraints(Constraint<? super Action> @NotNull ... constraints) {
		if (this.constraints==null) this.constraints = new HashSet<>(constraints.length);
		this.constraints.addAll((List) list(constraints));
		return this;
	}

/* ---------- AS OBJECT --------------------------------------------------------------------------------------------- */

	@Override
	public boolean equals(Object o) {
		if (this==o) return true;

		if (!(o instanceof Action a)) return false;
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

	@Override
	public String toString() {
		return global + "," + getKeys();
	}

	public record Data(boolean isGlobal, String keys) {

		public KeyCombination getKeysAsKeyCombination() {
			return keys.isEmpty() ? NO_MATCH : (KeyCombination) TryKt.getOr(TryKt.runTry(runnable(() -> KeyCodeCombination.valueOf(keys))), NO_MATCH);
		}

		@Override
		public String toString() {
			return isGlobal + "," + keys;
		}

		public static Data fromString(String str) {
			int i = str.indexOf(",");
			if (i==-1) return null;
			var isGlobal = Boolean.parseBoolean(str.substring(0, i));
			var keys = str.substring(i + 1);
			return new Data(isGlobal, keys);
		}
	}
}