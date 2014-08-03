
package Configuration;

import Action.Action;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import javafx.beans.property.Property;
import main.App;
import org.atteo.classindex.ClassIndex;
import utilities.FileUtil;
import utilities.Log;

/**
 * Provides methods to access configs of the application.
 * 
 * @author uranium
 */
@IsConfigurable(value = "General")
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
        Action.getActions().values().stream().map(ActionConfig::new).forEach(f->configs.put(f.getName(), f));
        
        // add methods in the end to avoid incorrect initialization
       for (Class c : classes) discoverMethodsOf(c);
       
    }
    
    private static void discoverConfigFieldsOf(Class clazz) {
        String _group = getGroup(clazz);
        for (Field f : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) {
                IsConfig a = f.getAnnotation(IsConfig.class);
                if (a != null) {
                    String group = a.group().isEmpty() ? _group : a.group();
                    String name = f.getName();

                    // create  appropriate Config
                    Config c;
                    if(Property.class.isAssignableFrom(f.getType())) {
                        try {
                            // make sure the field is final
                            if(!Modifier.isFinal(f.getModifiers())) 
                                // if not -> runtime exception, dev needs to fix his code
                                throw new IllegalStateException("Property config must be final.");
                            // make sur the field is accessible
                            f.setAccessible(true);
                            // get the property
                            Property val = (Property)f.get(null);
                            // make property config based on the property
                            c = new PropertyConfig(name, a, val, group);
                        } catch (IllegalAccessException | SecurityException ex) {
                            throw new RuntimeException("Can not access field: " + f.getName()
                                    + " for class: " + f.getDeclaringClass());
                        } finally {
                            // allways set accessible back
                            f.setAccessible(false);
                        }
                    } else {
                        // make statif field config based on the field
                        c = new StaticFieldConfig(name, a, group, f);
                    }
                    configs.put(c.getName(), c);
                }
            }
        }
    }
    private static void discoverMethodsOf(Class cls) {
        for (Method m : cls.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                for(AppliesConfig a : m.getAnnotationsByType(AppliesConfig.class)) {
                    if (a != null) {
                        String name = a.value();
                        if(!name.isEmpty() && configs.containsKey(name)) {
                            Config c = configs.get(name);
                            if(c instanceof StaticFieldConfig) {
                                ((StaticFieldConfig)c).applierMethod = m;
                                Log.deb("Adding method as applier method: " + m.getName() + " for " + name + ".");
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static String getGroup(Class<?> c) {
        IsConfigurable a = c.getAnnotation(IsConfigurable.class);
        return a==null || a.value().isEmpty() ? c.getSimpleName() : a.value();
    }
    
/******************************** setting fields ******************************/

    /**
     * @param value Object value. If not a proper type exceptions will be thrown.
     * @throws ClassCastException when value parameter not of expected type.
     * @throws IllegalArgumentException when value parameter not of expected type.
     * @throws RuntimeException if config does not exist. Such case must be a 
     * programming error.
     */
    public static void setNapplyField(String name, Object value) {
        Log.deb("Attempting to set and apply config field " + name + " to " + value);
        Config c = configs.get(name);
        Objects.requireNonNull(c,"Failed to set and apply config field: " + name + " . Reason: Does not exist.");
        c.setNapplyValue(value);
    }
    
    /**
     * 
     * @param name
     * @param value 
     * @throws RuntimeException if config does not exist. Such case must be a 
     * programming error.
     */
    public static void setField(String name, Object value) {
        Log.deb("Attempting to set config field value of: " + name + " to: " + value);
        Config c = configs.get(name);
        Objects.requireNonNull(c,"Failed to set config field: " + name + " . Reason: Does not exist.");
        c.setValue(value);
    }
    
    /**
     * 
     * @param name 
     * @throws RuntimeException if config does not exist. Such case must be a 
     * programming error.
     */
    public static void applyField(String name) {
        Log.deb("Attempting to apply config field " + name);
        Config c = configs.get(name);
        Objects.requireNonNull(c,"Failed to apply config field: " + name + " . Reason: Does not exist.");
        c.applyValue();
    }
    
    public static void applyFieldsAll() {
        configs.values().forEach(Config::applyValue);
    }
    
//    public static void applyFieldsOfClass(Class<?> clazz) {
//        configs.values().stream()
//                .filter(c->c.getDefaultValue().getClass().equals(clazz)) // this is broken
//                .forEach(Config::applyValue);
//    }
    
    public static void applyFieldsByName(List<String> fields_to_apply) {
        fields_to_apply.forEach(Configuration::applyField);
    }
    
    public static void applyFields(List<Config> fields_to_apply) {
        fields_to_apply.forEach(Config::applyValue);
    }
    
/******************************* public api ***********************************/
    
    /**
     * @param name
     * @return config with given name or null if such config does not exists
     */
    public static Config getField(String name) {
        return configs.get(name);
    }
    
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
        configs.values().forEach(c -> c.setNapplyValue(c.getDefaultValue()));
    }
    
    /**
     * Saves configuration to the file. The file is created if it does not exist,
     * otherwise it is completely overwritten.
     * Loops through Configuration fields and stores them all into file.
     */
    public static void save() {     
        Log.info("Saving configuration");
        
        String content="";
               content += "# " + App.getAppName() + " configuration file.\n";
               content += "# " + java.time.LocalTime.now() + "\n";
        
        for (Config f: getFields())
            content += f.getName() + " : " + f.toS() + "\n";
        
        FileUtil.writeFile("Settings.cfg", content);
    }
    
    /**
     * Loads previously saved configuration file and set its values for this.
     * <p>
     * Attempts to load all configuration fields from file. Fields might not be 
     * read either through I/O error or parsing errors. Parsing errors are
     * recoverable, meaning corrupted fields will be ignored.
     * Default values will be used for all unread fields.
     * <p>
     * If field of given name does not exist it will be ignored as well.
     */
    public static void load() {
        Log.info("Loading configuration");
        
        File file= new File("Settings.cfg").getAbsoluteFile();
        Map<String,String> lines = FileUtil.parseFile(file);
        if(lines.isEmpty())
            Log.info("Configuration couldnt be loaded. No content found. Using "
                    + "default settings.");
        
        lines.forEach((name,value) -> {
            Config c = getField(name);
            if (c!=null)
                c.setValueFrom(value);
            else
                Log.info("Config field " + name + " not available. Possible"
                    + " error in the configuration file.");
        });
    }
    
    
    
}