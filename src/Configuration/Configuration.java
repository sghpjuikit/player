
package Configuration;

import Action.Action;
import AudioPlayer.tagging.Playcount;
import PseudoObjects.Maximized;
import Serialization.Serializator;
import Serialization.Serializes;
import java.lang.reflect.Field;
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
import org.atteo.classindex.ClassIndex;
import utilities.Log;
import utilities.Parser.Parser;

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
    public static boolean windowAlwaysOnTop = false;
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
    
    @IsConfig(name="Playcount incrementing strategy", info = "Playcount strategy for incrementing playback.")
    public static Playcount.IncrStrategy increment_playcount = Playcount.IncrStrategy.ON_PERCENT;
    @IsConfig(name="Playcount incrementing at percent", info = "Percent at which playcount is incremented.")
    public static double increment_playcount_at_percent = 0.5;
    @IsConfig(name="Playcount incrementing at time", info = "Time at which playcount is incremented.")
    public static double increment_playcount_at_time = Duration.seconds(5).toMillis();
    @IsConfig(name="Playcount incrementing min percent", info = "Minimum percent at which playcount is incremented.")
    public static double increment_playcount_min_percent = 0.0;
    @IsConfig(name="Playcount incrementing min time", info = "Minimum time at which playcount is incremented.")
    public static double increment_playcount_min_time = Duration.seconds(0).toMillis();
    
    public final Map<String,Config> fields = new HashMap();
    
    
    private static final List<Class> classes = new ArrayList<>();
    private static final Map<String,Config> default_fields = new HashMap();     // def Config
    
/******************************************************************************/
    
    static {        
        ClassIndex.getAnnotated(IsConfigurable.class).forEach(classes::add);
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
    
    

    public Collection<Config> getFields(){
        return fields.values();
    }
    
/******************************************************************************/
    
    /**
     * @throws NullPointerException if application doesnt have any config with
     * specified name
     */
    public void setField(String name, String value) {                           // Log.deb("Setting field: " + name + " " + value);
        Config def_f = default_fields.get(name);
        Class type = def_f.type;
        Object v = type.equals(Action.class) ? Action.from((Action)def_f.value, value) : Parser.fromS(type, value);
        
        fields.put(name, new Config(def_f, v));
    }

    public void applyField(Config c) {
        applyField(c.name, Parser.toS(c.value));
    }
    private void applyField(String name, String value) {                         // Log.deb("Applying field "+name+" "+value);
        Config def_f = default_fields.get(name);
        Class type = def_f.type;
        
        if(type.equals(Action.class)) { // set as shortcut
            Action temp_a = Action.fromString(value);
            Action.getActions().get(name).set(temp_a.isGlobal(), temp_a.getKeys());
            return;
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
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Configuration)) return false;
        
         Map<String,Config> f1 = fields;
         Map<String,Config> f2 = ((Configuration)o).fields;
         
//         System.out.println("AAA "+f1.get("Minimize").value);
//         System.out.println("AAA "+f2.get("Minimize").value);
         
//         if (f1.size() != f2.size()) return false;
//         return !f1.entrySet().stream().anyMatch( entry -> f2.get(entry.getKey())!=null && !entry.getValue().equals(f2.get(entry.getKey())));
         return !f1.entrySet().stream().anyMatch( entry -> {
             if(entry.getValue().type.equals(Action.class) && entry.getKey()!=null) {
//                 if(entry.getKey().equalsIgnoreCase("Minimize")){
//                    System.out.println("");
//                    System.out.println(
//                            ((Action)entry.getValue().value).name + 
//                            " " + entry.getValue().equals(f2.get(entry.getKey())) +
//                            " - " + entry.getValue().value + " + " + f2.get(entry.getKey()).value
//
//
//                    );
//                    System.out.println(f2.get(entry.getKey())!=null && !entry.getValue().equals(f2.get(entry.getKey())));
//                 }
             }
//             System.out.println("," + entry.getKey() + " " + (f2.get(entry.getKey())!=null && !entry.getValue().equals(f2.get(entry.getKey()))));
             return f2.get(entry.getKey())!=null && !entry.getValue().equals(f2.get(entry.getKey()));
                     });
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.fields);
        return hash;
    }
    
    
    public static String getGroup(Class<?> c) {
        IsConfigurable a = c.getAnnotation(IsConfigurable.class);
        return a==null || a.group().isEmpty() ? c.getSimpleName() : a.group();
    }
    
    public static List<Config> getFieldsOf(Class c) {
        List<Config> fields = new ArrayList<>();
        String _group = getGroup(c);
        for (Field f : c.getFields()) {
            if ((f.getModifiers() & Modifier.STATIC) != 0) {
                IsConfig a = f.getAnnotation(IsConfig.class);
                if (a != null) {
                    try {
                        String group = (a.group().isEmpty()) ? _group : a.group();
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
        Action.getActions().values().stream().map(Config::new).forEach(f->list.put(f.name, f));
        
        return list;
    }
    
}