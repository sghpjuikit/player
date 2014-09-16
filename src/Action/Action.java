
package Action;

import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.PlaylistManager;
import Configuration.Config;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.ContextManager;
import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.IntellitypeListener;
import com.melloware.jintellitype.JIntellitype;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import static java.util.Collections.singletonList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.SHIFT;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import static javafx.scene.input.KeyCombination.NO_MATCH;
import javafx.util.Duration;
import main.App;
import org.atteo.classindex.ClassIndex;
import org.reactfx.EventSource;
import utilities.FxTimer;
import utilities.Log;
import utilities.access.Accessor;
import utilities.functional.functor.Procedure;

/**
 * Encapsulates application behavior.
 * <p>
 * An action can wrap any {@link Runnable}. The aim however is to allow a
 * framework make convenient externalization of application behavior possible.
 * With the help of {@link IsAction} annotation methods can be annotated and
 * invoked directly as actions anytime.
 * Custom actions can also be constructed and used.
 * <p>
 * One example of use case for action is generating shortcut for the
 * application. When the application starts up, all annotated methods are turned
 * into actions and provide shortcuts for execution of the underlying behavior.
 * <p>
 * Additionally, actions can be configured and their state serialized.
 * <p>
 * @author uranium
 */
@IsConfigurable
public final class Action extends Config<Action> implements Runnable {
    
    private final String name;
    private final Runnable action;
    private final String info;
    private final boolean continuous;
    private boolean global;
    private KeyCombination keys = KeyCombination.NO_MATCH;
    private final String defaultKeys;
    private final boolean defaultGlobal;
    
    private Action(IsAction a, Runnable action) {
        this(a.name(),action,a.description(),a.shortcut(),a.global(),a.continuous());
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
        return !keys.equals(NO_MATCH);
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
            System.out.println(System.currentTimeMillis());System.out.println("locking");
            locker.restart();
        }
        
        if(canRun) {
            // run on appFX thread
            if (Platform.isFxApplicationThread()) runUnsafe();
            else Platform.runLater(this::runUnsafe);
        }
        

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
            actionStream.push(name);
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
    public void register() {                                                    // Log.deb("Attempting to register shortcut " + name);
        if(!hasKeysAssigned()) return;
        
        if (global && global_shortcuts.getValue() && isGlobalShortcutsSupported())
            registerGlobal();
        else
            registerInApp();
    }
    public void unregister() {                                                  // Log.deb("Attempting to unregister shortcut " + name);
        // unregister both local and global to prevent illegal states
        unregisterGlobal();
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
            Log.info("Illegal shortcut keys parameter. Shortcut keys disabled for: "
                    + name + " Keys: '" + keys + "'");    
            this.keys = NO_MATCH;   // disable shortcut for wrong keys
        }
    }   
    
    private void registerInApp() {                                              // Log.deb("Registering in-app shortcut "+name);
        // fix local shortcut problem - keyCodes not registering, needs raw characters instead
        // TODO resolve or include all characters' conversions
        final KeyCombination k;
        String s = getKeys();
        if(s.contains("Back_Slash")) k = KeyCombination.keyCombination(s.replace("Back_Slash","\\"));
        else if(s.contains("Back_Quote"))k = KeyCombination.keyCombination(s.replace("Back_Quote","`"));
        else k = keys;
        
        if (App.getWindow()==null || !App.getWindow().isInitialized()) return;
        // register for each window separately
        ContextManager.windows.forEach( w -> 
            w.getStage().getScene().getAccelerators().put(k,this));
    }

    private void unregisterInApp() {                                            // Log.deb("Unregistering in-app shortcut "+name);
        // fix local shortcut problem - keyCodes not registering, needs raw characters instead
        // TODO resolve or include all characters' conversions
        final KeyCombination k = getKeysForLocalRegistering(this);
        
        if (App.getWindow()==null || !App.getWindow().isInitialized()) return;
        // unregister for each window separately
        ContextManager.windows.forEach( w -> 
            w.getStage().getScene().getAccelerators().remove(k));
    }
    public void unregisterInScene(Scene s) {
        s.getAccelerators().remove(getKeysForLocalRegistering(this));
    }
    
    private void registerGlobal() {                                             //  Log.deb("Registering global shortcut "+name);
        JIntellitype.getInstance().registerHotKey(getID(), getKeys());
    }
    
    public void registerInScene(Scene s) {
        s.getAccelerators().put(getKeysForLocalRegistering(this),this);
    }
    private void unregisterGlobal() {                                           //  Log.deb("Unregistering global shortcut "+name);
        JIntellitype.getInstance().unregisterHotKey(getID());
    }
    
    private int getID() {
        return name.hashCode();
    }
    
    private static KeyCombination getKeysForLocalRegistering(Action a) {
        // fix local shortcut problem - keyCodes not registering, needs raw characters instead
        // TODO resolve or include all characters' conversions
        String s = a.getKeys();
        if(s.contains("Back_Slash")) 
            return KeyCombination.keyCombination(s.replace("Back_Slash","\\"));
        else if(s.contains("Back_Quote"))
            return KeyCombination.keyCombination(s.replace("Back_Quote","`"));
        else 
            return a.keys;
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
    public void applyValue(Action val) { }

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

    /** {@inheritDoc} */
    @Override
    public List<Class> getSupportedClasses() {
        return singletonList(Action.class);
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
        JIntellitype.getInstance().cleanUp();
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
     * Returns modifiable collection of actions mapped by their name. Actions
     * can be added and removed.
     * <p>
     * Note that the name must be unique.
     * 
     * @return map of all actions.
     */
    public static Collection<Action> getActions() {
        return actions.values();
    }
    
    /**
     * Returns the action to which the specified name is mapped, or null if 
     * action does not exist.
     * @param name
     * 
     * @return action or null
     */
    public static Action getAction(String name) {
        return actions.get(name.hashCode());
    }
    
/************************ action helper methods *******************************/
    
    private static boolean isGlobalShortcutsSupported = JIntellitype.isJIntellitypeSupported();
    private static final Map<Integer,Action> actions = gatherActions();
    
    /** @return all actions of this application */
    private static Map<Integer,Action> gatherActions() {
        List<Class<?>> cs = new ArrayList();
        
        // autodiscover all classes that can contain actions
        ClassIndex.getAnnotated(IsActionable.class).forEach(cs::add);
        
        // discover all actions
        Map<Integer,Action> acts = new HashMap();
        Lookup method_lookup = MethodHandles.lookup();
        for (Class<?> man : cs) {
            for (Method m : man.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) {
                    for(IsAction a : m.getAnnotationsByType(IsAction.class)) {
                        if (a != null) {
                            if (m.getParameters().length > 0)
                                throw new RuntimeException("Action Method must have 0 parameters!");
                            
                            // grab method
                            MethodHandle mh;
                            try {
                                m.setAccessible(true);
                                mh = method_lookup.unreflect(m);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                            
                            String name = a.name();
                            Procedure p = () -> {
                                try {
                                    mh.invokeExact();
                                } catch (Throwable e) {
                                    throw new RuntimeException("Error during running action.",e);
                                }
                            };
                            acts.put(name.hashCode(),new Action(a,p));
                        }
                    }
                }
            }
        }
        return acts;
    }
    
/************************ shortcut helper methods *****************************/

    // lock
     private static FxTimer locker = FxTimer.create(Duration.millis(80), () -> lock = -1);
//    private static final FxTimer locker = FxTimer.create(Duration.millis(500), Action::unlock);
    private static int lock = -1;
//    private static void unlock() {System.out.println(System.currentTimeMillis() +" unlocking");
//        lock = -1;
//    }

    //shortcut running
    // this listener is running ever 33ms when any registered shortcut is pressed
    private static final HotkeyListener global_listener = id -> {System.out.println(System.currentTimeMillis() + " s");
        Log.deb("Global shortcut " + actions.get(id).getName() + " captured.");
        actions.get(id).run();
        locker.restart();
    };
    private static final IntellitypeListener media_listener = i -> {
        // run on appFX thread
        Platform.runLater(() -> {
            if     (i==JIntellitype.APPCOMMAND_MEDIA_PREVIOUSTRACK) PlaylistManager.playPreviousItem();
            else if(i==JIntellitype.APPCOMMAND_MEDIA_NEXTTRACK) PlaylistManager.playNextItem();
            else if(i==JIntellitype.APPCOMMAND_MEDIA_PLAY_PAUSE) PLAYBACK.pause_resume();
            else if(i==JIntellitype.APPCOMMAND_MEDIA_STOP) PLAYBACK.stop();
            else if(i==JIntellitype.APPCOMMAND_VOLUME_DOWN) PLAYBACK.decVolume();
            else if(i==JIntellitype.APPCOMMAND_VOLUME_UP) PLAYBACK.incVolume();
            else if(i==JIntellitype.APPCOMMAND_VOLUME_MUTE) PLAYBACK.toggleMute();
            else if(i==JIntellitype.APPCOMMAND_CLOSE) App.close();
        });
    };
    
/******************************************************************************/

    /**
     * Event source and stream for executed actions, providing their name. Use
     * for notifications of running the action or executing additional behavior.
     * <p>
     * A use case could be an application wizard asking user to do something.
     * The code in question simply notifies this stream of the name of action
     * or uses custom string as id. The wizard would then monitor this stream
     * and get notified if the expected action was executed.
     * <p>
     * Running an {@link Action} fires an event.
     * Supports custom actions. Simply push a String value into the stream.
     */
    public static final EventSource<String> actionStream = new EventSource();
    
    
/****************************** CONFIGURATION *********************************/
    
    @IsConfig(name = "Allow global shortcuts", info = "Allows using the shortcuts even if"
            + " application is not focused. Not all platforms supported.", group = "Shortcuts")
    public static final Accessor<Boolean> global_shortcuts = new Accessor<>(true, v -> {
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
    public static final Accessor<Boolean> global_media_shortcuts = new Accessor<>(true, v -> {
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
    public static KeyCode Shortcut_ALTERNATE = SHIFT;
    
    @IsConfig(name = "Collapse layout", info = "Colapses focused container within layout.", group = "Shortcuts", editable = false)
    public static String Shortcut_COLAPSE = "Shift+C";

}