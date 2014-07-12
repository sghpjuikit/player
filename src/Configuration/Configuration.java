
package Configuration;

import Action.Action;
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
 * Provides methods to access configs of the application.
 * 
 * @author uranium
 */
@IsConfigurable(group = "General")
public class Configuration {
    
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
    
    private static void discover(){
        Map<String,Config> list = new HashMap();
        
        // add class fields
        for (Class c : classes) discoverConfigFieldsOf(c);
        
        // add action fields
        Action.getActions().values().stream().map(ObjectConfig::new).forEach(f->configs.put(f.getName(), f));
        
        // add methods in the end to avoid incorrect initialization
       for (Class c : classes) discoverMethodsOf(c);
       
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
                        
                        configs.put(name, new ClassConfig(name, a, val, group, f));
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
     * @param value Object value. If not a proper type one of the two exceptions
     * will be thrown.
     * @throws ClassCastException when value parameter not of expected type.
     * @throws IllegalArgumentException when value parameter not of expected type.
     */
    public static void setNapplyField(String name, Object value) {
        Log.deb("Attempting to set and apply config field " + name + " to " + value);
        
        Config c = configs.get(name);
        if(c == null) {
            Log.deb("Failed to set and apply config field: " + name + " . Reason: Does not exist.");
            return;
        }
        
        if(c.getType().equals(Action.class)) {
            Action a = (Action)value;
            Action.getActions().get(name).set(a.isGlobal(), a.getKeys());
        } else {
            c.setNapplyValue(value);
        }
    }
    
    public static void setField(String name, Object value) {
        // Log.deb("Setting field "+name+" "+value);
        
        Config c = configs.get(name);
        if(c == null) {
            Log.deb("Failed to set config field: " + name + " . Reason: Does not exist.");
            return;
        }
        
        if(c.getType().equals(Action.class)) {
            Action temp_a = (Action) value;
            Action.getActions().get(name).set(temp_a.isGlobal(), temp_a.getKeys());
        } else {
            c.setValue(value);
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
                .forEach(c->applyField(c.getName()));
    }
    
    public static void applyFieldsByName(List<String> fields_to_apply) {
        fields_to_apply.forEach(Configuration::applyField);
    }
    
    public static void applyFieldsByConfig(List<Config> fields_to_apply) {
        fields_to_apply.forEach(c -> applyField(c.getName()));
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
        configs.forEach((name,config) -> setNapplyField(name,config.defaultValue));
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
            content += f.getName() + " : " + Parser.toS(f.getValue()) + "\n";
        
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
        Map<String,String> lines = FileUtil.parseFile(file);
        if(lines.isEmpty())
            Log.mess("Configuration couldnt be loaded. No content found. Using "
                    + "default settings.");
        
        lines.forEach((name,value) -> {
            if(configs.containsKey(name)) {
                Configuration.setField(name,Parser.fromS(configs.get(name).getType(),value));
            } else Log.mess("Config field " + name + " not available. Possible"
                    + " error in the configuration file.");
            
        });
    }
    
    
    
}