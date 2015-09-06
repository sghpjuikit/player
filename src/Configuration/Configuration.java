
package Configuration;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.WritableValue;

import org.atteo.classindex.ClassIndex;

import Configuration.Config.ListConfig;
import Configuration.Config.OverridablePropertyConfig;
import Configuration.Config.PropertyConfig;
import Configuration.Config.ReadOnlyPropertyConfig;
import Configuration.Config.VarList;
import action.Action;
import main.App;
import util.File.FileUtil;
import util.access.Ѵo;
import util.collections.map.MapSet;

import static util.Util.getAllFields;
import static util.dev.Util.noFinal;
import static util.dev.Util.yesFinal;

/**
 * Provides methods to access configs of the application.
 * 
 * @author uranium
 */
public class Configuration {
    
    private static final MapSet<String,Config> configs = new MapSet<>(c -> c.getGroup()+"."+c.getName());
    private static final Lookup methodLookup = MethodHandles.lookup();
    
    
    public static void collectAppConfigs() {
        // for all discovered classes
        ClassIndex.getAnnotated(IsConfigurable.class).forEach( c -> {
            // add class fields
            discoverConfigFieldsOf(c);
            // add methods in the end to avoid incorrect initialization
            discoverMethodsOf(c);        
        });
        // add service configs
        App.services.forEach(s -> configs.addAll(s.getFields()));
        
        
        configs.stream().filter(Config::isEditable)
                        .filter(c -> c.getType().equals(Boolean.class))
                        .map(c -> (Config<Boolean>) c)
                        .forEach(c -> {
                            String name = c.getGroup()+" "+c.getName() + " - toggle";
                            Runnable r = ()->c.setNextNapplyValue();
                            Action.getActions().add(new Action(name, r, "Toggles value between yes and no", "", false, false));
                        });
        
        // add actions
        configs.addAll(Action.getActions());
    }
    
    public static List<Config> getFields() {
        return new ArrayList(configs);
    }
    
    public static List<Config> getFields(Predicate<Config> condition) {
        List<Config> cs = new ArrayList(getFields());
                     cs.removeIf(condition.negate());
        return cs;
    }
    
    /** Changes all config fields to their default value and applies them */
    public static void toDefault() {
        getFields().forEach(Config::setNapplyDefaultValue);
    }
    
    /**
     * Saves configuration to the file. The file is created if it does not exist,
     * otherwise it is completely overwritten.
     * Loops through Configuration fields and stores them all into file.
     */
    public static void save() {     
        String header = ""
            + "# " + App.getAppName() + " configuration file.\n"
            + "# " + java.time.LocalTime.now() + "\n";
        StringBuilder content = new StringBuilder(header);
        
        Function<Config,String> f = configs.keyMapper;
        getFields().stream()
                   .sorted(util.functional.Util.byNC(f::apply))
                   .forEach(c -> content.append(f.apply(c) + " : " + c.getValueS() + "\n"));
        
        FileUtil.writeFile("Settings.cfg", content.toString());
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
        File file = new File("Settings.cfg").getAbsoluteFile();
        FileUtil.readFileKeyValues(file).forEach((id,value) -> {
            Config c = configs.get(id);
            if (c!=null) c.setValueS(value);
        });
    }
    
    
/******************************************************************************/
    
    private static String getGroup(Class<?> c) {
        IsConfigurable a = c.getAnnotation(IsConfigurable.class);
        return a==null || a.value().isEmpty() ? c.getSimpleName() : a.value();
    }
    
    
    private static void discoverConfigFieldsOf(Class c) {
        configs.addAll(configsOf(c, null, true, false));
    }
    
    private static void discoverMethodsOf(Class c) {
        for (Method m : c.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                for(AppliesConfig a : m.getAnnotationsByType(AppliesConfig.class)) {
                    if (a != null) {
                        String name = a.value();
                        String group = getGroup(c);
                        String config_id = group+"."+name;
                        if(configs.containsKey(config_id) && !name.isEmpty()) {
                            Config config = configs.get(config_id);
                            if(config instanceof FieldConfig) {
                                try {
                                    m.setAccessible(true);
                                    ((FieldConfig)config).applier = methodLookup.unreflect(m);
                                    // System.out.println("Adding method as applier method: " + m.getName() + " for " + config_id + ".");
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    static List<Config> configsOf(Class clazz, Object instnc, boolean include_static, boolean include_instance) {
        // check arguments
        if(include_instance && instnc==null)
            throw new IllegalArgumentException("Instance must not be null if instance fields flag is true");
        
        List<Config> out = new ArrayList();
        
        for (Field f : getAllFields(clazz)) {
            Config c = createConfig(clazz, f, instnc, include_static, include_instance);
            if(c!=null) out.add(c);
        }
        return out;
    }
    
    static Config createConfig(Class cl, Field f, Object instnc, boolean include_static, boolean include_instance) {
        // that are annotated
        Config c = null;
        IsConfig a = f.getAnnotation(IsConfig.class);
        if (a != null) {
            String group = a.group().isEmpty() ? getGroup(cl) : a.group();
            String name = f.getName();
            int modifiers = f.getModifiers();
            if (include_static && Modifier.isStatic(modifiers))
                c = createConfig(f, instnc, name, a, group);
            
            if (include_instance && !Modifier.isStatic(modifiers))
                c = createConfig(f, instnc, name, a, group);
            
        }
        return c;
    }
    
    private static Config createConfig(Field f, Object instance, String name, IsConfig anotation, String group) {
        Class c = f.getType();
        if(Config.class.isAssignableFrom(c)) {
            return newFromConfig(f, instance);
        } else
        if(WritableValue.class.isAssignableFrom(c) || ReadOnlyProperty.class.isAssignableFrom(c)) {
            return newFromProperty(f, instance, name, anotation, group);
        } else {
            try {
                noFinal(f);            // make sure the field is not final
                f.setAccessible(true);     // make sure the field is accessible
                MethodHandle getter = methodLookup.unreflectGetter(f);
                MethodHandle setter = methodLookup.unreflectSetter(f);
                return new FieldConfig(name, anotation, instance, group, getter, setter);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unreflecting field " + f.getName() + " failed. " + e.getMessage());
            }
        }
    }
    
    private static Config newFromProperty(Field f, Object instance, String name, IsConfig anotation, String group) {
        try {
            yesFinal(f);            // make sure the field is final
            f.setAccessible(true);      // make sure the field is accessible
            if(VarList.class.isAssignableFrom(f.getType()))
                return new ListConfig(name, anotation, (VarList)f.get(instance), group);
            if(Ѵo.class.isAssignableFrom(f.getType()))
                return new OverridablePropertyConfig(name, anotation, (Ѵo)f.get(instance), group);
            if(WritableValue.class.isAssignableFrom(f.getType()))
                return new PropertyConfig(name, anotation, (WritableValue)f.get(instance), group);
            if(ReadOnlyProperty.class.isAssignableFrom(f.getType()))
                return new ReadOnlyPropertyConfig(name, anotation, (ReadOnlyProperty)f.get(instance), group);
            throw new IllegalArgumentException("Wrong class");
        } catch (IllegalAccessException | SecurityException e) {
            throw new RuntimeException("Can not access field: " + f.getName() + " for class: " + f.getDeclaringClass());
        }
    }
    
    private static Config newFromConfig(Field f, Object instance) {
        try {
            yesFinal(f);            // make sure the field is final
            f.setAccessible(true);      // make sure the field is accessible
            return (Config)f.get(instance);
        } catch (IllegalAccessException | SecurityException ex) {
            throw new RuntimeException("Can not access field: " + f.getName() + " for class: " + f.getDeclaringClass());
        }
    }
    
}