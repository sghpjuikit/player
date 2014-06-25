
package Configuration;

import Action.Action;
import Action.IsAction;
import PseudoObjects.Maximized;
import Serialization.Serializes;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.App;
import org.atteo.classindex.ClassIndex;
import utilities.FileUtil;
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
    
    private static final List<Class> classes = new ArrayList();
    private static final Map<String,Config> configs = new HashMap();
    
    
    static {        
        ClassIndex.getAnnotated(IsConfigurable.class).forEach(classes::add);
        discover();
    }
    
    private static Map<String,Config> discover(){
        Map<String,Config> list = new HashMap();
        
        // add class fields
        for (Class c : classes)
            discoverConfigFieldsOf(c);
        
        // add action fields
        Action.getActions().values().stream().map(Config::new).forEach(f->list.put(f.name, f));
        
        // add methods in the end to avoid incorrect initialization
       for (Class c : classes)
           discoverMethodsOf(c);
       
        return list;
    }
    
    private static void discoverConfigFieldsOf(Class c) {
        String _group = getGroup(c);
        for (Field f : c.getFields()) {
            if ((f.getModifiers() & Modifier.STATIC) != 0) {
                IsConfig a = f.getAnnotation(IsConfig.class);
                if (a != null) {
                    try {
                        String group = a.group().isEmpty() ? _group : a.group();
                        String name = f.getName();
                        Object val = f.get(null);
                        
                        configs.put(name, new Config(name, a, val, group, f));
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(Action.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }
    private static void discoverMethodsOf(Class c) {
        for (Method m : c.getDeclaredMethods()) {
            if ((m.getModifiers() & Modifier.STATIC) != 0) {
                for(AppliesConfig a : m.getAnnotationsByType(AppliesConfig.class)) {
                    if (a != null) {
                        String name = a.config();
                        if(!name.isEmpty() && configs.containsKey(name)) {
                            configs.get(name).applierMethod = m;
                            Log.deb("Adding method as applier method: " + m.getName() + " for " + name + ".");
                        }
                    }
                }
            }
        }
    }
    
    private static String getGroup(Class<?> c) {
        IsConfigurable a = c.getAnnotation(IsConfigurable.class);
        return a==null || a.group().isEmpty() ? c.getSimpleName() : a.group();
    }
    
/******************************** setting fields ******************************/

    /**
     * If application doesnt have any config with specified name this method is
     * a no-op
     */
    public static void setNapplyField(String name, String value) {
        Log.deb("Attempting to set and apply config field " + name + " to " + value);
        
        Config c = configs.get(name);
        if(c == null) {
            Log.deb("Failed to set and apply config field: " + name + " . Reason: Does not exist.");
            return;
        }
        
        if(c.getType().equals(Action.class)) {
            Action temp_a = Action.fromString(value);
            Action.getActions().get(name).set(temp_a.isGlobal(), temp_a.getKeys());
        } else {
            Field f = c.sourceField;
            Object new_value = Parser.fromS(f.getType(), value);
            Log.deb("Setting config field: "+ name + " to: " + new_value);

            // set new config value
            boolean was_set = false;
            try {
                f.set(null, new_value);
                was_set = true;
                Log.deb("Config field " + name + " set.");
            } catch (IllegalAccessException e) {
                Log.err("Failed to set config field: " + name + " . Reason: " + e.getMessage());
            }

            // apply new field value
            if(was_set) {
                Method m = c.applierMethod;
                if(m != null) {
                    Log.deb("Applying config: " + name);
                    try {
                        m.setAccessible(true);
                        m.invoke(null, new Object[0]);
                    } catch (IllegalAccessException | IllegalArgumentException | 
                            InvocationTargetException | SecurityException e) {
                        Log.err("Failed to apply config field: " + name + ". Reason: " + e.getMessage());
                    } finally {
                        m.setAccessible(false);
                    }
                } else {
                    Log.deb("Failed to apply config field: " + name + ". Reason: No applier method.");
                }
            }
        }
    }
    
    public static void setField(String name, String value) {
        // Log.deb("Setting field "+name+" "+value);
        
        Config c = configs.get(name);
        if(c == null) {
            Log.deb("Failed to set config field: " + name + " . Reason: Does not exist.");
            return;
        }
        
        if(c.getType().equals(Action.class)) {
            Action temp_a = Action.fromString(value);
            Action.getActions().get(name).set(temp_a.isGlobal(), temp_a.getKeys());
        } else {
            Field f = c.sourceField;
            if (f!=null) {
                Object new_value = Parser.fromS(f.getType(), value);
                Log.deb("Setting config : "+ name + " to: " + new_value);

                // set new config value
                boolean was_set = false;
                try {
                    f.set(null, new_value);
                    was_set = true;
                } catch (IllegalAccessException e) {
                    Log.err("Failed to set config: " + name + ". Reason: " + e.getMessage());
                }
            }
        }
    }
    
    public static void applyField(String name) {
        // Log.deb("Setting and applying field " + name + " " + value);
        
        Config c = configs.get(name);
        if(c == null) {
            Log.deb("Failed to apply config field: " + name + " . Reason: Does not exist.");
            return;
        }
        
        if(!c.getType().equals(Action.class)) {
            Field f = c.sourceField;
            Method m = c.applierMethod;
            if(m != null) {
                Log.deb("Applying config: " + name);
                try {
                    m.setAccessible(true);
                    m.invoke(null, new Object[0]);
                } catch (IllegalAccessException | IllegalArgumentException | 
                        InvocationTargetException | SecurityException e) {
                    Log.err("Failed to apply config: " + name + ". Reason: " + e.getMessage());
                } finally {
                    m.setAccessible(false);
                }
            }
        }
    }
    
    public static void applyFieldsAll() {
        configs.forEach((name,field)->applyField(name));
    }
    
    public static void applyFieldsOfClass(Class<?> clazz) {
        configs.values().stream()
                .filter(c->c.defaultValue.getClass().equals(clazz))
                .forEach(c->applyField(c.name));
    }
    
    public static void applyFieldsByName(List<String> fields_to_apply) {
        fields_to_apply.forEach(Configuration::applyField);
    }
    
    public static void applyFieldsByConfig(List<Config> fields_to_apply) {
        fields_to_apply.forEach(c -> applyField(c.name));
    }
    
/******************************* public api ***********************************/
    
    public static List<Config> getFields() {
        return new ArrayList(configs.values());
    }
    
    public static List<Config> getFields(Predicate<Config> condition) {
        List<Config> cs = new ArrayList(getFields());
                     cs.removeIf(condition.negate());
        return cs;
    }
    
    /**
     * Changes all config fields to their default value and applies them
     */
    public static void toDefault() {
        configs.forEach((name,config) -> setNapplyField(name,Parser.toS(config.defaultValue)));
    }
    
    /**
     * Saves configuration to the file. The file is created if it doesnt exist,
     * otherwise it is completely overwriten.
     * Loops through Configuration fields and stores them all into file.
     */
    public static void save() {     
        Log.mess("Saving configuration");
        
        String content="";
               content += "# " + App.getAppName() + " configuration file.\n";
               content += "# " + java.time.LocalTime.now() + "\n";
        
        for (Config f: getFields())
            content += f.name + " : " + Parser.toS(f.getValue()) + "\n";
        
        FileUtil.writeFile("Settings.cfg", content);
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
    public static void load() {
        Log.mess("Loading configuration");
        
        File file= new File("Settings.cfg").getAbsoluteFile();
        Map<String,String> lin = FileUtil.parseFile(file);
        if(lin.isEmpty())
            Log.mess("Configuration couldnt be set. No content. Using old settings.");
        
        lin.forEach(Configuration::setField);
    }
    
    
    
}