
package Configuration;

import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.PlaylistManager;
import GUI.ContextManager;
import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.IntellitypeListener;
import com.melloware.jintellitype.JIntellitype;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import static javafx.scene.input.KeyCombination.NO_MATCH;
import main.App;
import org.atteo.classindex.ClassIndex;
import utilities.Log;
import utilities.functional.functor.Procedure;

/**
 * Encapsulates application behavior.
 * <p>
 * An action can wrap any {@link Runnable}. The aim however is to allow a
 * framework make convenient externalization of application behavior possible.
 * With the help of {@link IsAction} annotation methods can be annotated and
 * invoked directly as actions anytime.
 * <p>
 * One example of use case for this is generating shortcut actions for the
 * application. When the application starts up, all annotated methods are turned
 * into actions and provide shortcuts for execution of the underlying behavior.
 * <p>
 * Additionally, actions can be configured and their state serialized.
 * <p>
 * @author uranium
 */
@IsConfigurable
public final class Action {
    
    public final String name;
    private final Runnable action;
    public final String info;
    public final boolean continuous;
    private boolean global;
    private KeyCombination keys = KeyCombination.NO_MATCH;
    
    public Action(IsAction a, Runnable action) {
        this(a.name(),action,a.description(),a.shortcut(),a.global(),a.continuous());
    }
    public Action(String name, Runnable action, String info, String keys, boolean global, boolean continuous) {
        this.name = name;
        this.action = action;
        this.info = info;
        this.continuous = continuous;
        this.global = global;
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
    
    /** @return the key combination for shortcut of this action */
    public String getKeys() {
        String s = keys.getName();
        s = s.replaceAll("'", "");      // we need to replace ''' characters
        s = s.replaceAll(" ", "_");     // we need to replace ' ' characters
        return s;
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
    /** Execute the action. Always executes on application thread. */
    public void run() {
        if(global) Log.deb("Global shortcut event fired. Shortcut: " + name);
        else Log.deb("Local shortcut event fired. Shortcut: " + name);
        // run on appFX thread
        if(Platform.isFxApplicationThread()) action.run();
        else Platform.runLater(()-> action.run());
    }
    
    /**
     * Activates shortcut. Only registered shortcuts can be invoked.
     * <p>
     * If the shortcut's keys are empty, registration will not take place.
     * <p>
     * For local shortcut method will succeed only if GUI is already initialized.
     * For global, platform support must is required. If it isnt, shortcut will
     * be registered as local.
     */
    public void register() {                                                    // Log.deb("Attempting to register shortcut " + name);
        if(keys.equals(NO_MATCH)) return;  // avoid registering empty shortcut
        if (global && global_shortcuts && isGlobalShortcutsSupported())
            registerGlobal();
        else
            registerInApp();
    }
    public void unregister() {                                                  // Log.deb("Attempting to unregister shortcut " + name);
        // unregister both local and global to prevent illegal states
        unregisterGlobal();
        unregisterInApp();
    }
    
/****************************** helper methods ********************************/
    
    private void changeKeys(String keys) { System.out.println("registering " + keys+" "+name);
        if(keys.isEmpty()) { System.out.println("EMPTY");       
            this.keys = NO_MATCH;   // disable shortcut for empty keys
            return;
        }
        try {
            this.keys = KeyCombination.keyCombination(keys);
        } catch (Exception e) {
            Log.mess("Illegal shortcut keys parameter. Shortcut keys disabled for: "
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
            w.getStage().getScene().getAccelerators().put(k,action)
        );
    }
    private void unregisterInApp() {                                            // Log.deb("Unregistering in-app shortcut "+name);
        
        // fix local shortcut problem - keyCodes not registering, needs raw characters instead
        // TODO resolve or include all characters' conversions
        final KeyCombination k;
        String s = getKeys();
        if(s.contains("Back_Slash")) k = KeyCombination.keyCombination(s.replace("Back_Slash","\\"));
        else if(s.contains("Back_Quote"))k = KeyCombination.keyCombination(s.replace("Back_Quote","`"));
        else k = keys;
        
        if (App.getWindow()==null || !App.getWindow().isInitialized()) return;
        // unregister for each window separately
        ContextManager.windows.forEach( w -> 
            w.getStage().getScene().getAccelerators().remove(k)
        );
    }
    private void registerGlobal() {                                             //  Log.deb("Registering global shortcut "+name);
        JIntellitype.getInstance().registerHotKey(getID(), getKeys());
    }
    private void unregisterGlobal() {                                           //  Log.deb("Unregistering global shortcut "+name);
        JIntellitype.getInstance().unregisterHotKey(getID());
    }
    private int getID() {
        Action[] as = shortcuts();
        for(int i=0; i<as.length; i++)
            if (as[i].equals(this)) return i;
        return -1;
    }
    
    @Override
    public boolean equals(Object o) {
        if(o==null) return false;
        if(o==this) return true;
        if(!o.getClass().equals(Action.class)) return false;
        Action a = ((Action)o);
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
    
    
    
    
    
    
    
    
    
    private Action(boolean b, KeyCombination k) {
        this.name = null;
        this.action = null;
        this.info = null;
        this.continuous = false;
        this.global = b;
        this.keys = k;
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
    
    @IsConfig(name = "Allow global shortcuts", info = "Allows using the shortcuts even if application is not focused. Not all platforms supported."
    ,group = "Shortcuts")
    public static boolean global_shortcuts = true;
    @IsConfig(name = "Allow media shortcuts", info = "Allows using shortcuts for media keys on the keyboard.", group = "Shortcuts")
    public static boolean global_media_shortcuts = true;
    @IsConfig(name = "Manage Layout (fast) Shortcut", info = "Enables layout managment mode.", group = "Shortcuts", editable = false)
    public static String Shortcut_ALTERNATE = "Alt";
    @IsConfig(name = "Collapse layout", info = "Colapses focused container within layout.", group = "Shortcuts", editable = false)
    public static String Shortcut_COLAPSE = "Ctrl+C";
    
    /** Activates listening process for global hotkeys */
    public static void startGlobalListening() {
        if(isGlobalShortcutsSupported()) {
            JIntellitype.getInstance().addHotKeyListener(listener);
            JIntellitype.getInstance().addIntellitypeListener(Ilistener);
        }
    }
    /** 
     * Deactivates listening process for global hotkeys. Frees resources. This
     * method should should always be ran at the end of application's life cycle
     * if {@link #stopGlobalListening()} was invoked at least once.
     * Not doing so might prevent from the application to close successfully.
     */
    public static void stopGlobalListening() {
        JIntellitype.getInstance().cleanUp();
    }
    /** 
     * Returns true if global shortcuts are supported at running platform.
     * Otherwise false. In such case, global shortcuts will run as local.
     */
    public static boolean isGlobalShortcutsSupported() {
        return JIntellitype.isJIntellitypeSupported();
    }
    
    /** @return all actions. */
    public static Map<String,Action> getShortcuts(){
        return actions;
    }
    
/************************ action helper methods *******************************/
    
    
    
    private static final Map<String,Action> actions = gatherActions();
    
    private static Action[] shortcuts(){
        return actions.values().toArray(new Action[0]);
    }
    /** @return all actions of this application */
    private static Map<String,Action> gatherActions() {
        List<Class<?>> cs = new ArrayList();
        // autodiscover all classes that can contain actions
        ClassIndex.getAnnotated(IsActionable.class).forEach(cs::add);
        
        Map<String,Action> acts = new HashMap();
        // discover all actions
        for (Class<?> man : cs) {
            for (Method m : man.getDeclaredMethods()) {
                if ((m.getModifiers() & Modifier.STATIC) != 0) {
                    IsAction a = m.getAnnotation(IsAction.class);
                    if (a != null) {
                        if (m.getParameters().length > 0)
                            throw new RuntimeException("Action Method must have 0 parameters!");
                        String name = a.name();
                        Procedure b = () -> {
                            try {
                                m.invoke(null, new Object[0]);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                                Log.err("Can not run specified method. " + e.getMessage());
                            }
                        };
                        acts.put(name,new Action(a,b));
                    }
                }
            }
        }
        
        return acts;
    }
    
/************************ shortcut helper methods *****************************/

    //shortcut running
    private static final HotkeyListener listener = (int i) -> {
        Log.deb("Global shortcut " + i + " captured.");
        try {
//            System.out.println("running " + i);
            runShortcut(i);
        } catch(IndexOutOfBoundsException e) {
            
        }
    };
    private static final IntellitypeListener Ilistener = (int i) -> {
        if(!global_media_shortcuts) return;
        Platform.runLater(() -> {
            if     (i==JIntellitype.APPCOMMAND_MEDIA_PREVIOUSTRACK) PlaylistManager.playPreviousItem();
            else if(i==JIntellitype.APPCOMMAND_MEDIA_NEXTTRACK) PlaylistManager.playNextItem();
            else if(i==JIntellitype.APPCOMMAND_MEDIA_PLAY_PAUSE) PLAYBACK.pause_resume();
            else if(i==JIntellitype.APPCOMMAND_MEDIA_STOP) PLAYBACK.stop();
            else if(i==JIntellitype.APPCOMMAND_VOLUME_DOWN) PLAYBACK.decVolume();
            else if(i==JIntellitype.APPCOMMAND_VOLUME_UP) PLAYBACK.incVolume();
            else if(i==JIntellitype.APPCOMMAND_VOLUME_MUTE) PLAYBACK.toggleMute();
        });
    };
    
    private static void runShortcut(int s_id) {
        if(s_id==lock) {
            lockReached = true;
        } else {
            Action a = shortcuts()[s_id];
            a.run();
            if(a.continuous)
                lock(s_id, 30);
            else
                lock(s_id, 800);
        }
    }
    
    // lock
    private static Timer locker;
    private static int lock = -1;
    private static boolean lockReached = false;
    
    private static void lock(int shortcut, long period) {
        unlock();
        lock = shortcut;
        locker = new Timer();
        TimerTask unlock = new TimerTask(){
            @Override public void run() {
                unlock();   // no problems, but still... Platform.runlater() maybe?
            }
        };
        locker.schedule(unlock, period);
    }
    private static void unlock() {
        Action a = lock==-1 ? null : shortcuts()[lock];
        if(lockReached && a!=null && !a.continuous) {
            lockReached = false;
            lock(lock, 250);
        } else {
            lock = -1;
            if(locker!=null){
                locker.cancel();
                locker.purge();
                locker = null;
            }
        }
    }
    
}
