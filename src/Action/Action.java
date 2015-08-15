
package action;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import org.atteo.classindex.ClassIndex;

import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.IntellitypeListener;
import com.melloware.jintellitype.JIntellitype;

import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.PlaylistManager;
import Configuration.Config;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import gui.objects.Window.stage.Window;
import main.App;
import unused.Log;
import util.access.Var;
import util.async.Async;
import util.async.executor.FxTimer;
import util.collections.map.MapSet;
import util.dev.Dependency;

import static javafx.scene.input.KeyCode.ALT_GRAPH;
import static javafx.scene.input.KeyCombination.NO_MATCH;
import static util.async.Async.runLater;
import static util.functional.Util.do_NOTHING;

/**
 * Encapsulates application behavior.
 * <p>
 * An action can wrap any {@link Runnable}. The aim however is to allow a
 * framework make convenient externalization of application behavior possible.
 * With the help of {@link IsAction} annotation methods can be annotated and
 * invoked directly as actions anytime. Example of use for action is generating
 * shortcuts for the application.
 * <p>
 * Action is also {@link Config} so it can be configured and serialized.
 */
@IsConfigurable
public final class Action extends Config<Action> implements Runnable {
    
    /** Action that does nothing. Use where null inappropriate. */
    public static final Action EMPTY = new Action("None", do_NOTHING, "Does nothing", "", false, false);
    
    private final String name;
    private final Runnable action;
    private final String info;
    private final boolean continuous;
    private boolean global;
    private KeyCombination keys = KeyCombination.NO_MATCH;
    private final String defaultKeys;
    private final boolean defaultGlobal;
    
    private Action(IsAction a, Runnable action) {
        this(a.name(),action,a.desc(),a.keys(),a.global(),a.repeat());
    }
    
    /**
     * Creates new action.
     * @param name action name. Must be be unique for each action. Also human readable.
     * @param action Code that gets executed on {@link #run()}
     * @param info value for the final info property
     * @param keys Key combination for activating this action as a hotkey. See
     * {@link #setKeys(java.lang.String)}
     * @param global value for the global property
     * @param continuous value for the final continuous property
     */
    public Action(String name, Runnable action, String info, String keys, boolean global, boolean continuous) {
        this.name = name;
        this.action = action;
        this.info = info;
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
     * Default false;
     * @return whether the action is global
     */
    public boolean isGlobal() {
        return global;
    }

    /**
     * Whether the action should be run constantly while the hotkey is pressed
     * or once.
     * @return 
     */
    public boolean isContinuous() {
        return continuous;
    }
    
    /** 
     * Returns the key combination for activating this action as a hotkey.
     * The output of this method is always valid parsable string for method
     * {@link #setKeys(java.lang.String)}. Use to assign keys of this action to
     * another action or to get the keys as human readable String.
     * @return the key combination for shortcut of this action or "" if no
     * valid value.
     */
    public String getKeys() {
        String s = keys.getName();
        s = s.replaceAll("'", "");      // we need to replace ''' characters
        s = s.replaceAll(" ", "_");     // we need to replace ' ' characters
        s = s.replaceAll(Matcher.quoteReplacement("\\"), "Back_Slash");
        return s;
    }
    /**
     * Returns the key combination for activating this action as a hotkey.
     * Alternative to {@link #getKeys()} with more friendly output. Use when
     * the alternative is not satisfactory.
     * @return the keys or KeyCombination.NO_MATCH if no valid value
     */
    public KeyCombination getKeyCombination() {
        return keys;
    }
    
    /**
     * When the parameter is not valid parsable hotkey string, the hotkey will
     * not be able to be registered and used.
     * @return true if and only if the keys for this action's hotkey have valid
     * value and can be registered.
     */
    public boolean hasKeysAssigned() {
        return keys!=NO_MATCH;
    }
    
    
    /** Set globality of this action. */
    public void setGlobal(boolean global) {
        unregister();
        this.global = global;
        register();
    }
    
    /** 
     * Change and apply key combination. 
     * @param key_combination Case doesnt matter. <pre>
     * For example: "CTRL+A", "F6", "D", "ALT+SHIFT+\"
     * </pre>
     * Incorrect keys will be substituted with "", which is equivalent to 
     * deactivating the shortcut.
     * <p>
     * To check the result of the assignment of the keys use {@link #getKeys()}
     * or {@link #hasKeysAssigned()} method.
     */
    public void setKeys(String key_combination) {
        unregister();
        changeKeys(key_combination);
        register();
    }
    /**
     * Set keys and scope of this action. See {@link #setGlobal(boolean)} and
     * {@link #setKeys(java.lang.String)}
     * @param global
     * @param keys 
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
    
    /** Execute the action. Always executes on application thread. */
    @Override
    public void run() {
        
        int id = getID();
        boolean canRun = id!=lock;
        
        if(!continuous) {
            lock = id;
//            System.out.println(System.currentTimeMillis());System.out.println("locking");
            locker.start();
        }
        
        // run on appFX thread
        if(canRun) Async.runFX(this::runUnsafe);
    }
    
    private void runUnsafe() {
        if(global) Log.deb("Global shortcut " + name + " execuing.");
        else Log.deb("Local shortcut " + name + " execuing.");
        
//        int id = getID();
//        boolean canRun = id!=lock;
//        
//        if(!continuous) {
//            lock = id;System.out.println(System.currentTimeMillis());
//            locker.restart();
////            unlocker.push(null);
//        }
//        
//        if(canRun) {
            action.run();
            App.actionStream.push(name);
//        }
    }
    
    /**
     * Activates shortcut. Only registered shortcuts can be invoked.
     * <p>
     * If the {@link #hasKeysAssigned()} returns false, registration will not 
     * take place.
     * <p>
     * For local action this method will succeed only after {@link Scene} is 
     * already initialized.
     * For global, platform support is required. If it isnt, shortcut will
     * be registered locally, although the action will remain global.
     * <p>
     * Note that shortcut can be registered globally multiple times even with
     * the same keys and locally with different keys. Make sure the action is
     * unregistered before registering it.
     */
    public void register() {
        if(!hasKeysAssigned()) return;
        
        // notice the else and how even global shortcuts can register locally
        // this is so if global registration is not possible, we fall back to
        // local, not leaving user confused about why the shortcut doesnt work
        if (global && global_shortcuts.getValue() && isGlobalShortcutsSupported())
            registerGlobal();
        else 
            // runlater is bugfix, we delay local shortcut registering 
            // probably a javafx bug, as this was not always a problem
            //
            // for some unknown reason some shortcuts (F3,F4,
            // F5,F6,F8,F12 confirmed) not getting registered when app starts, but 
            // other shortcuts register fine, even F9, F10 or F11...
            //
            // (1) The order in which shortcuts register doesnt seem to play a 
            // role. (2) The problem is certainly not shortcuts being consumed
            // by gui. (3) This method always executes on fx thread, threading 
            // is not the problem, you can make sure by uncommenting:
            // System.out.println("Registering shortcut: " + keys.getDisplayText() + ", is FX thread: " + Platform.isFxApplicationThread());
            runLater(this::registerInApp);
    }
    public void unregister() {
        // unregister both local and global to prevent illegal states
        if (isGlobalShortcutsSupported()) unregisterGlobal();
        unregisterInApp();
    }
    
/*********************** registering helper methods ***************************/
    
    private void changeKeys(String keys) {
        if(keys.isEmpty()) {   
            this.keys = NO_MATCH;   // disable shortcut for empty keys
            return;
        }
        try {
            this.keys = KeyCombination.keyCombination(keys);
        } catch (Exception e) {
            Log.warn("Illegal shortcut keys parameter. Shortcut keys disabled for: "
                    + name + " Keys: '" + keys + "'");    
            this.keys = NO_MATCH;   // disable shortcut for wrong keys
        }
    }   
    
    private void registerInApp() {
        if (!App.isInitialized()) return;

        KeyCombination k = getKeysForLocalRegistering();
        // register for each window separately
        Window.windows.forEach(w -> w.getStage().getScene().getAccelerators().put(k,this));
    }
    private void unregisterInApp() {
        if (!App.isInitialized()) return;

        KeyCombination k = getKeysForLocalRegistering();
        // unregister for each window separately
        Window.windows.forEach(w -> w.getStage().getScene().getAccelerators().remove(k));
    }
    
    
    public void unregisterInScene(Scene s) {
        s.getAccelerators().remove(getKeysForLocalRegistering());
    }
    public void registerInScene(Scene s) {
        if (!App.isInitialized()) return;
        s.getAccelerators().put(getKeysForLocalRegistering(),this);
    }
    
    
    private void registerGlobal() {
        JIntellitype.getInstance().registerHotKey(getID(), getKeys());
    }
    private void unregisterGlobal() {
        JIntellitype.getInstance().unregisterHotKey(getID());
    }
    
    
    private int getID() {
        return name.hashCode();
    }
    
    private KeyCombination getKeysForLocalRegistering() {
        // fix local shortcut problem - keyCodes not registering, needs raw characters instead
        // TODO resolve or include all characters' conversions
        String s = getKeys();
        if(s.contains("Back_Slash")) 
            return KeyCombination.keyCombination(s.replace("Back_Slash","\\"));
        else if(s.contains("Back_Quote"))
            return KeyCombination.keyCombination(s.replace("Back_Quote","`"));
        else 
            return keys;
    }
    
/********************************** AS CONFIG *********************************/
    
    /** {@inheritDoc} */
    @Override
    public Action getValue() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(Action val) {
        set(val.isGlobal(), val.getKeys());
    }

    /** {@inheritDoc} */
    @Override
    public void applyValue(Action val) {
        register();
    }

    /** {@inheritDoc} */
    @Override
    public Class<Action> getType() {
        return Action.class;
    }

    /** {@inheritDoc} */
    @Override
    public Action getDefaultValue() {
        return new Action(name,action,info,defaultKeys,defaultGlobal,continuous);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public String getGuiName() {
        return name;
    }    
    
    /** {@inheritDoc} */
    @Override
    public String getInfo() {
        return info;
    }

    /** {@inheritDoc} */
    @Override
    public String getGroup() {
        return "Shortcuts";
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isEditable() {
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isMinMax() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public double getMin() {
        return Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public double getMax() {
        return Double.NaN;
    }
    
/********************************** AS OBJECT *********************************/
    
    @Override
    public boolean equals(Object o) {
        if(this==o) return true; // this line can make a difference
        
        if(!(o instanceof Action)) return false;
        Action a = (Action) o;
        // we will compare all fields that can change (global & keys)
        // for all the rest only one (name) is necesary because they go
        // with each other
        // name is basically a unique identifier so this should be enough
        return a.name.equals(name) && a.global==global && a.keys.equals(keys);
    }

    @Override
    public int hashCode() {
        int hash = 7;
            hash = 41 * hash + Objects.hashCode(this.name);
            hash = 41 * hash + (this.global ? 1 : 0);
            hash = 41 * hash + Objects.hashCode(this.keys);
        return hash;
    }
    
    

    
    
    private Action(boolean isGlobal, KeyCombination keys) {
        this.name = null;
        this.action = null;
        this.info = null;
        this.continuous = false;
        this.global = isGlobal;
        this.keys = keys;
        this.defaultGlobal = isGlobal;
        this.defaultKeys = getKeys();
    }
    public static Action fromString(String str) {
        int i = str.lastIndexOf(",");
        if(i==-1)return null;
        String s1 = str.substring(0,i);
        String s2 = str.substring(i+1, str.length());
        boolean g = Boolean.parseBoolean(s1);
        KeyCombination k = s2.isEmpty() ? KeyCombination.NO_MATCH : KeyCodeCombination.valueOf(s2);
        return new Action(g, k);
    }
    public static Action from(Action a, String str) {
        Action tmp = fromString(str);
        if(tmp!=null) {
            a.global = tmp.global;
            a.keys = tmp.keys;
        }
        return a;
    }
    @Override
    public String toString() {
        return global + "," + getKeys();
    }
    
    

    
/*********************** SHORTCUT HANDLING ON APP LEVEL ***********************/
    
    /** 
     * Activates listening process for global hotkeys. Not running this method
     * will cause registered global hotkeys to not get invoked. Use once when 
     * application initializes.
     * Does nothing if not supported.
     */
    public static void startGlobalListening() {
        if(isGlobalShortcutsSupported()) {
            JIntellitype.getInstance().addHotKeyListener(global_listener);
            JIntellitype.getInstance().addIntellitypeListener(media_listener);
        }
    }
    
    /** 
     * Deactivates listening process for global hotkeys. Frees resources. This
     * method should should always be ran at the end of application's life cycle
     * if {@link #stopGlobalListening()} was invoked at least once.
     * Not doing so might prevent from the application to close successfully,
     * because bgr listening thread will not close.
     */
    public static void stopGlobalListening() {
        if(isGlobalShortcutsSupported()) {
            JIntellitype.getInstance().cleanUp();
        }
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
    
    /** 
     * Returns modifiable collection of all actions mapped by their name. Actions
     * can be added and removed, which modifiea the underlying collection.
     * @return all actions.
     */
    public static Collection<Action> getActions() {
        return actions;
    }
    
    /**
     * Returns the action with specified name.
     * 
     * @param name
     * @return action. Never null.
     * @throws IllegalArgumentException if no such action 
     */
    @Dependency("must use the same implementation as Action.getId()")
    public static Action get(String name) {
        Action a = actions.get(name.hashCode());
        if(a==null) throw new IllegalArgumentException("No such action: " + name);
        return a;
    }
    
    /** Do not use. Private API. Subject to change. */
    @Deprecated
    public static Action getOrNull(String name) {
        return actions.get(name.hashCode());
    }
    
/************************ action helper methods *******************************/
    
    private static boolean isGlobalShortcutsSupported = JIntellitype.isJIntellitypeSupported();
    private static final MapSet<Integer,Action> actions = gatherActions();
    
    /** @return all actions of this application */
    private static MapSet<Integer,Action> gatherActions() {
        List<Class<?>> cs = new ArrayList<>();
        
        // autodiscover all classes that can contain actions
        ClassIndex.getAnnotated(IsActionable.class).forEach(cs::add);
        
        // discover all actions
        MapSet<Integer,Action> out = new MapSet<>(Action::getID);
                               out.add(EMPTY);
        Lookup method_lookup = MethodHandles.lookup();
        for (Class<?> man : cs) {
            for (Method m : man.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) {
                    for(IsAction a : m.getAnnotationsByType(IsAction.class)) {
                        if (m.getParameters().length > 0)
                            throw new RuntimeException("Action Method must have 0 parameters!");

                        // grab method
                        MethodHandle mh;
                        try {
                            m.setAccessible(true);
                            mh = method_lookup.unreflect(m);
                            m.setAccessible(false);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }

                        Runnable r = () -> {
                            try {
                                mh.invokeExact();
                            } catch (Throwable e) {
                                throw new RuntimeException("Error during running action.",e);
                            }
                        };
                        Action ac = new Action(a, r);
                        out.add(ac);
                    }
                }
            }
        }
        return out;
    }
    
/************************ shortcut helper methods *****************************/

    // lock
     private static final FxTimer locker = new FxTimer(80, 1, () -> lock = -1);
//    private static final FxTimer locker = FxTimer.create(Duration.millis(500), Action::unlock);
    private static int lock = -1;
//    private static void unlock() {System.out.println(System.currentTimeMillis() +" unlocking");
//        lock = -1;
//    }

    //shortcut running
    // this listener is running ever 33ms when any registered shortcut is pressed
    private static final HotkeyListener global_listener = id -> {
        Log.deb("Global shortcut " + actions.get(id).getName() + " captured.");
        actions.get(id).run();
        locker.start();
    };
    private static final IntellitypeListener media_listener = i -> {
        // run on appFX thread
        Platform.runLater(() -> {
            if     (i==JIntellitype.APPCOMMAND_MEDIA_PREVIOUSTRACK) PlaylistManager.playPreviousItem();
            else if(i==JIntellitype.APPCOMMAND_MEDIA_NEXTTRACK) PlaylistManager.playNextItem();
            else if(i==JIntellitype.APPCOMMAND_MEDIA_PLAY_PAUSE) PLAYBACK.pause_resume();
            else if(i==JIntellitype.APPCOMMAND_MEDIA_STOP) PLAYBACK.stop();
            else if(i==JIntellitype.APPCOMMAND_VOLUME_DOWN) PLAYBACK.volumeDec();
            else if(i==JIntellitype.APPCOMMAND_VOLUME_UP) PLAYBACK.volumeInc();
            else if(i==JIntellitype.APPCOMMAND_VOLUME_MUTE) PLAYBACK.toggleMute();
            else if(i==JIntellitype.APPCOMMAND_CLOSE) App.close();
        });
    };
    
/****************************** CONFIGURATION *********************************/
    
    @IsConfig(name = "Allow global shortcuts", info = "Allows using the shortcuts even if"
            + " application is not focused. Not all platforms supported.", group = "Shortcuts")
    public static final Var<Boolean> global_shortcuts = new Var<>(true, v -> {
        if(isGlobalShortcutsSupported()) {
            if(v){
                // make sure we dont add the listener twice
                JIntellitype.getInstance().removeHotKeyListener(global_listener);
                JIntellitype.getInstance().addHotKeyListener(global_listener);
                // reregister shortcuts to switch from local
                getActions().forEach( a -> {
                    a.unregister();
                    a.register();
                });
            } else {
                JIntellitype.getInstance().removeHotKeyListener(global_listener);
                // reregister shortcuts to switch to local
                getActions().forEach( a -> {
                    a.unregister();
                    a.register();
                });
            }
        }
    });
    
    @IsConfig(name = "Allow media shortcuts", info = "Allows using shortcuts for media keys on the keyboard.", group = "Shortcuts")
    public static final Var<Boolean> global_media_shortcuts = new Var<>(true, v -> {
        if(isGlobalShortcutsSupported()) {
            if(v) {
                // make sure we dont add the listener twice
                JIntellitype.getInstance().removeIntellitypeListener(media_listener);
                JIntellitype.getInstance().addIntellitypeListener(media_listener);
            } else {
                JIntellitype.getInstance().removeIntellitypeListener(media_listener);
            }
        }
    });
    
    @IsConfig(name = "Manage Layout (fast) Shortcut", info = "Enables layout managment mode.", group = "Shortcuts")
    public static KeyCode Shortcut_ALTERNATE = ALT_GRAPH;
    
    @IsConfig(name = "Collapse layout", info = "Colapses focused container within layout.", group = "Shortcuts", editable = false)
    public static String Shortcut_COLAPSE = "Shift+C";

}