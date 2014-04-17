
package Configuration;

import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Playcount;
import GUI.ContextManager;
import GUI.GUI;
import GUI.objects.ClickEffect;
import GUI.objects.PopOver.PopOver.ScreenCentricPos;
import GUI.objects.Thumbnail;
import Layout.LayoutManager;
import PseudoObjects.Maximized;
import Serialization.Serializator;
import Serialization.Serializes;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.util.Duration;
import utilities.Log;
import utilities.Parser.Parser;
import utilities.functional.functor.Procedure;

/**
 * Provides internal application settings.
 * This class uses Reflection API and Annotations to persist properties into file.
 * 
 * There are three ways to create a field configurable by user
 * - defining public field in this class.
 * @see IsConfig
 * 
 * - annotating public static method of class implementing marker Manager 
 * interface by IsAction Annotation. This automatically turns the method into
 * configurable action with shortcut. The methods can still be called
 * programmatically, but can be invoked manually in runtime and configured.
 * @see IsAction
 * 
 * - annotating public static field of a class registered in this class.
 * Fields will become configurable in runtime while continuing to function as
 * static class fields. Final fields will naturally not work. Interfaces can
 * only provide final fields.
 * @see IsConfig
 * 
 * Unrelated to how the field was constructed it can be
 * - persisted across application sessions
 * - its value changed in runtime
 * - support property GUI generation based on type
 * 
 * Annotation is not mandatory, but is recommended as it provides additional
 * information about fields for application which helps user experience (tooltips
 * names, etc)
 * 
 * Recommended approach:
 * - properties used wide across the application should be defined here to allow
 * centralized access
 * - properties used mostly within their own class should be defined there and
 * annotated
 * - common application behavior (which could only be represented as a static
 * method because it can not support instances) should be annotated
 * 
 * @author uranium
 */
@IsConfigurable(group = "General")
public class Configuration implements Serializes {
    
    @IsConfig(info = "name of the application", editable = false)
    public static String application_Name = "PlayerFX";
    
    // file pathnames
    @IsConfig(info = "location of playback state file", editable = false)
    public static String PLAYER_STATE_CONFIG_FILE = "PlayerState.cfg";
    @IsConfig(info = "location of widgets", editable = false)
    public static String WIDGET_FOLDER = "Widgets";
    @IsConfig(info = "location of layouts", editable = false)
    public static String LAYOUT_FOLDER = "Layouts";
    @IsConfig(info = "location of skins", editable = false)
    public static String SKIN_FOLDER = "Skins";
    @IsConfig(info = "name of user data folder", editable = false)
    public static String DATA_FOLDER = "UserData";
    @IsConfig(info = "location of saved playlists", editable = false)
    public static String PLAYLIST_FOLDER = DATA_FOLDER + File.separator + "Playlists";
        
    @IsConfig(info = "corner distance for resizing")
    public static double borderCorner = 18;                   //border limit for North, NW, W, SW, S, SE, E, NE mouse resize cursor
    @IsConfig(info = "last height of the main window", editable = false)
    public static double windowHeight = 800;
    @IsConfig(info = "last width of the main window", editable = false)
    public static double windowWidth = 1000;
    @IsConfig(info = "last x position of the main window", editable = false)
    public static double windowPosX = 200;
    @IsConfig(info = "last y position of the main window", editable = false)
    public static double windowPosY = 200;
    @IsConfig(info = "last maximized state of the main window", editable = false)
    public static Maximized windowMaximized = Maximized.NONE;
    @IsConfig(info = "last fulscreen state of the main window", editable = false)
    public static boolean windowFullscreen = false;
    @IsConfig(info = "whether main window is resizable")
    public static boolean windowResizable = true;
    @IsConfig(info = "last minimized state of the main window", editable = false)
    public static boolean windowMinimized = false;
    @IsConfig(info = "exit hint text showed on fullscreen change")
    public static String fullscreenExitHintText = "";
    @IsConfig(info = "last used layout", editable = false)
    public static String last_layout = "layout0";
    @IsConfig(info = "last used rigsht layout", editable = false)
    public static String right_layout = "new_layout1";
    @IsConfig(info = "last used left layout", editable = false)
    public static String left_layout = "new_layout-1";
    
    // preffered gui settings
    @IsConfig(info = "Turn animations on/off (if they suport disability.")
    public static boolean allowAnimation = true;
    @IsConfig(info = "Preffered editability of rating controls. This value is overridable.")
    public static boolean allowRatingChange = true;
    @IsConfig(info = "Preffered number of elements in rating control. This value is overridable.")
    public static int maxRating = 5;
    @IsConfig(info = "Preffered value for partial values in rating controls. This value is overridable.")
    public static boolean partialRating = true;
    @IsConfig(info = "Preffered hoverability of rating controls. This value is overridable.")
    public static boolean hoverRating = true;
    @IsConfig(info = "Preffered factor to scale by on hover operations")
    public static double scaleFactor = 0.1;
    
    // skining
    @IsConfig(visible = false, editable = false)
    public static double TABLE_ROW_HEIGHT = 14;                    //get rid of this
    
    //tagging
    @IsConfig(info = "Preffered text when no tag value for field. This value is overridable.")
    public static String TAG_NO_VALUE = "-- no assigned value --";
    @IsConfig(info = "Preffered text when multiple tag values per field. This value is overridable.")
    public static String TAG_MULTIPLE_VALUE = "-- multiple values --";
    public static boolean ALBUM_ARTIST_WHEN_NO_ARTIST = true;
    public static Playcount.IncrStrategy increment_playcount = Playcount.IncrStrategy.ON_PERCENT;
    public static double increment_playcount_at_percent = 0.5;
    public static double increment_playcount_at_time = Duration.seconds(5).toMillis();
    public static double increment_playcount_min_percent = 0.0;
    public static double increment_playcount_min_time = Duration.seconds(0).toMillis();
    
    @IsConfig(name = "Allow global shortcuts", info = "Allows using the shortcuts even if application is not focused. Not all platforms supported.")
    public static boolean global_shortcuts = true;
    
    // notification
    @IsConfig(info = "show notifications")
    public static boolean showNotification = true;
    @IsConfig(info = "show notifications about playback status")
    public static boolean showStatusNotification = true;
    @IsConfig(info = "show notifications about playing item")
    public static boolean showSongNotification = true;
    @IsConfig(info = "time for notification to autohide")
    public static double notificationDuration = 2500;
    @IsConfig(info = "Fade in/out notifications")
    public static boolean notificationAnimated = true;
    @IsConfig(info = "Fade in/out time for notification")
    public static double notifFadeTime = 500;
    @IsConfig(info = "Closes notification when clicked anywhere.")
    public static boolean notifCloseOnClickAny = true;
    @IsConfig(info = "Closes notification when clicked on it.")
    public static boolean notifCloseOnClick = true;
    @IsConfig(info = "Deminimize application on notification click when minimized.")
    public static boolean notifclickOpenApp = true;
    @IsConfig(info = "Position of notification.")
    public static ScreenCentricPos notifPos = ScreenCentricPos.ScreenBottomRight;
    
    @IsConfig(name = "Manage Layout (fast) Shortcut", info = "Enables layout managment mode")
    public static String Shortcut_ALTERNATE = "Alt";
    
    private final Map<String,Config> fields = new HashMap();
    
    private static final Map<String,Action> actions = gatherActions();
    private static final List<Class> classes = new ArrayList<>();
    private static final Map<String,Config> default_fields = new HashMap();     // def Config
    
/******************************************************************************/
    
    static {
        classes.add(Configuration.class);
        classes.add(ClickEffect.class);
        classes.add(Thumbnail.class);
        classes.add(PLAYBACK.class);
        classes.add(GUI.class);
        classes.add(ContextManager.class);
        
        default_fields.putAll(gatherFields());
    }
    private Configuration() {
    }
    private Configuration(boolean def) {
        if (def)
            fields.putAll(default_fields);
        else
            fields.putAll(gatherFields());
    }
    
    /** @return default initialized configuration. */
    public static Configuration getDefault() {
        return new Configuration(true);
    }
    
    /** @return current configuration. */
    public static Configuration getCurrent() {
        return new Configuration(false);
    }
    
    /**
     * Do not use.
     * @return empty configuration. */
    public static Configuration getEmpty() {
        return new Configuration();
    }
    
/******************************************************************************/
    
    
    /** @return all shortcut fields. */
    public List<Action> getShortcuts(){
        return new ArrayList(actions.values());
    }
    public Collection<Config> getFields(){
        return fields.values();
    }
    
/******************************************************************************/
    
    /**
     * @throws NullPointerException if application doesnt have any config with
     * specified name
     */
    public void setField(String name, String value) {                           Log.deb("Setting field: " + name + " " + value);
        Config def_f = default_fields.get(name);
        Class type = def_f.type;
        Object v = type.equals(Action.class) ? value : Parser.fromS(type, value);
        fields.put(name, new Config(def_f, v));
    }

    /**
     * @throws NullPointerException if application doesnt have any config with
     * specified name
     */
    public void applyField(String name, String value) {                         System.out.println("applying "+name+" "+value);
        Config def_f = default_fields.get(name);
        Class type = def_f.type;
        
        if(type.equals(Action.class)) { // set as shortcut
            actions.get(name).changeKeys(value);
        } else {                        // set as class field
            if (classes.stream().flatMap(c->Stream.of(c.getFields()))
                    .filter(f->(f.getModifiers() & Modifier.STATIC) != 0)
                    .anyMatch(f->f.getName().equals(name))) {

                classes.stream().flatMap(c->Stream.of(c.getFields()))
                        .filter(f->(f.getModifiers() & Modifier.STATIC) != 0)
                        .filter(f->f.getName().equals(name))
                        .forEach(f-> {                                              // System.out.println("setting "+f.getName() + " "+ parse(f.getType(), value));
                            try {
                                f.set(null, Parser.fromS(f.getType(), value));      // getFields().stream().filter(k->k.name.equals(name)).forEachOrdered(k->System.out.println(k.name + k.value));
                            } catch (IllegalAccessException ex) {
                                Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        });
                return;
            }
        }
        
        Log.mess("Configuration value couldnt be set for field: " + name + " .");
    }
    
    /**
     * Sets all configurations fields to values from specified configuration.
     * @param config
     * @throws NullPointerException if param null
     */
    public void to(Configuration config) {
        fields.clear();
        fields.putAll(config.fields);
    }
    
    /** Sets all configuration fields to default values. */
    public void reset() {
        to(getDefault());
    }
    
    /** Sets all configuration fields to latest default values. */
    public void update() {
        fields.clear();
        fields.putAll(gatherFields());
    }
    
    /** @return true if and only if configuration has all fields at default value */
    public boolean isDefault() {
        return equals(getDefault());
    }
    /** @return true if and only if configuration has all fields at default value */
    public boolean isCurrent() {
        return equals(getCurrent());
    }
    
    /** @return new configuration initialized to the same values - a clone. */
    public Configuration copy() {
        Configuration new_config = getDefault();
                      new_config.to(this);
        return new_config;
    }
    
    /**
     * Saves configuration to the file. The file is created if it doesnt exist,
     * otherwise it is completely overwriten.
     * Loops through Configuration fields and stores them all into file.
     */
    public void save() {
        Serializator.serialize(this);
    }

    /**
     * Loads previously saved configuration file and set its values for this.
     * 
     * Attempts to load all configuration fields from file. Fields might not be 
     * read either through I/O error or parsing errors. Parsing errors are
     * recoverable, meaning only corrupted fields will not be read regardless of
     * their position in the file.
     * Old values will be used for all unread fields.
     */
    public void load() {
        Configuration c = (Configuration) Serializator.deserialize(this);
        to(c);
    }
    
    /**
     * Compares this object to another one.
     * According to this implementation object is equal to this only if the
     * following all checks out:
     *  - is not null
     *  - is instance of Configuration class
     *  - has all its field values exactly the same as this
     * @param o
     * @return 
     */
    @Override
    public boolean equals(Object o) {//return false;
        if (o == null || !(o instanceof Configuration)) 
            return false;
        
         Map<String,Config> f1 = fields;
         Map<String,Config> f2 = ((Configuration)o).fields;
         
//         if (f1.size() != f2.size()) return false;
         return !f1.entrySet().stream().anyMatch( entry -> f2.get(entry.getKey())!=null && !entry.getValue().equals(f2.get(entry.getKey())));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.fields);
        return hash;
    }
    
    
    public static String getGroup(Class c) {
        IsConfigurable a = (IsConfigurable) c.getAnnotation(IsConfigurable.class);
        return a==null ? c.getSimpleName() : a.group();
    }
    
    public static List<Config> getFieldsOf(Class c) {
        List<Config> fields = new ArrayList<>();
        String group = getGroup(c);
        for (Field f : c.getFields()) {
            if ((f.getModifiers() & Modifier.STATIC) != 0) {
                IsConfig a = f.getAnnotation(IsConfig.class);
                if (a != null) {
                    try {
                        String name = f.getName();
                        Object val = f.get(null);
                        fields.add(new Config(name, a, val, group));
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(Action.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        return fields;
    }
    
    /** @return list of all configurable fields with latest values. */
    private static Map<String,Config> gatherFields(){
        Map<String,Config> list = new HashMap();
        
        // add class fields
        for (Class c : classes)
            getFieldsOf(c).forEach(f->list.put(f.name, f));
        
        // add action fields
        actions.values().stream().map(Config::new).forEach(f->list.put(f.name, f));
        
        return list;
    }
    
    /** @return all actions of this application*/
    private static Map<String,Action> gatherActions() {
        List<Class<? extends Configurable>> cs = new ArrayList();
        cs.add(PLAYBACK.class);
        cs.add(PlaylistManager.class);
        cs.add(LayoutManager.class);
        cs.add(GUI.class);
        
        Map<String,Action> acts = new HashMap();
        
        for (Class<? extends Configurable> man : cs) {
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
                            } catch (IllegalAccessException | InvocationTargetException ex) {
                                Log.err("Can not run specified method. " + ex.getMessage());
                            }
                        };
                        acts.put(name,new Action(name,b,a.info(),a.shortcut()));
                    }
                }
            }
        }
        
        return acts;
    }
}