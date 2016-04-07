
package util.conf;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.WritableValue;

import org.atteo.classindex.ClassIndex;

import util.access.Vo;
import util.action.Action;
import util.collections.mapset.MapSet;
import util.conf.Config.*;
import util.file.Util;
import util.functional.Functors.Ƒ1;

import static util.file.Util.readFileKeyValues;
import static util.type.Util.getAllFields;
import static util.type.Util.getGenericPropertyType;
import static util.dev.Util.noFinal;
import static util.dev.Util.yesFinal;
import static util.functional.Util.byNC;

/**
 * Provides methods to access configs.
 */
public class Configuration {

    private static final Lookup methodLookup = MethodHandles.lookup();
    private static final Ƒ1<String,String> mapper = s -> s.replaceAll(" ", "_").toLowerCase();

    private final Map<String,String> properties = new ConcurrentHashMap<>();
    private final MapSet<String,Config> configs = new MapSet<>(mapper.compose(c -> c.getGroup() + "." + c.getName()));

    /**
     * Returns raw key-value ({@link java.lang.String}) pairs representing the serialized configs.
     * @return modifiable thread safe map of key-value property pairs
     */
    public Map<String,String> rawGet() {
        return properties;
    }

    public void rawAddProperty(String name, String value) {
        properties.put(name,value);
    }

    public void rawAddProperties(Map<String,String> _properties) {
        properties.putAll(_properties);
    }

    public void rawAdd(File file) {
        readFileKeyValues(file).forEach(this::rawAddProperty);
    }

    public void rawRemProperty(String key) {
        properties.remove(key);
    }

    public void rawRemProperties(Map<String,String> properties) {
        properties.forEach((name,value) -> rawRemProperty(name));
    }

    public void rawRem(File file) {
        readFileKeyValues(file).forEach((name,value) -> rawRemProperty(name));
    }

    public <C> void collect(Configurable<C> c) {
        collectConfigs(c.getFields());
    }

    public <C> void collect(Collection<? extends Config<C>> c) {
	    collectConfigs(c);
    }

    public <C> void collect(Configurable<C>... cs) {
        for(Configurable<C> c : cs) collect(c);
    }

    public void collectStatic() {
        // for all discovered classes
        ClassIndex.getAnnotated(IsConfigurable.class).forEach(c -> {
            discoverConfigFieldsOf(c);  // add class fields
            discoverMethodsOf(c);   // add methods in the end to avoid incorrect initialization
        });
    }

    private <C> void collectConfigs(Collection<? extends Config<C>> _configs) {
        configs.addAll(_configs);
        _configs.stream()
                .filter(Config::isEditable)
                .filter(c -> c.getType().equals(Boolean.class))
                // .map(c -> (Config<Boolean>) c) // unnecessary, but safe
                .forEach(c -> {
                    String name = c.getGroup() + " " + c.getName() + " - toggle";
                    Runnable r = c::setNextNapplyValue;
                    Action a = new Action(name, r, "Toggles value between yes and no", c.getGroup(), "", false, false);
                    Action.getActions().add(a);
                    configs.add(a);
                });
    }

	public <T> void drop(Config<T> config) {
		configs.remove(config);
	}

	// TODO: add more drop implementations for convenience

    public List<Config> getFields() {
        return new ArrayList<>(configs);
    }

    public List<Config> getFields(Predicate<Config> condition) {
        List<Config> cs = new ArrayList<>(getFields());
                     cs.removeIf(condition.negate());
        return cs;
    }

    /** Changes all config fields to their default value and applies them */
    public void toDefault() {
        getFields().forEach(Config::setNapplyDefaultValue);
    }

    /**
     * Saves configuration to the file. The file is created if it does not exist,
     * otherwise it is completely overwritten.
     * Loops through Configuration fields and stores them all into file.
     */
    public void save(String title, File file) {
        StringBuilder content = new StringBuilder()
            .append("# " + title + " property file" + "\n")
            .append("# Last auto-modified: " + java.time.LocalDateTime.now() + "\n")
            .append("#\n")
            .append("# Properties are in the format: {property path}.{property.name}{separator}{property value}\n")
            .append("# \t{property path}  must be lowercase with period as path separator, e.g.: this.is.a.path\n")
            .append("# \t{property name}  must be lowercase and contain no spaces (use underscores '_' instead)\n")
            .append("# \t{separator}      must be ' - ' sequence\n")
            .append("# \t{property value} can be any string (even empty)\n")
            .append("# Properties must be separated by combination of '\\n', '\\r' characters\n")
            .append("#\n")
            .append("# Ignored lines:\n")
            .append("# \tcomment lines (start with '#')\n")
            .append("# \tempty lines\n")
            .append("\n");

        Function<Config,String> converter = configs.keyMapper;
        getFields().stream()
                   .sorted(byNC(converter))
                   .forEach(c -> content.append("\n" + converter.apply(c) + " : " + c.getValueS()));

        Util.writeFile(file, content.toString());
    }

    /**
     * Loads previously saved configuration file and set its values for this.
     * <p/>
     * Attempts to load all configuration fields from file. Fields might not be
     * read either through I/O error or parsing errors. Parsing errors are
     * recoverable, meaning corrupted fields will be ignored.
     * Default values will be used for all unread fields.
     * <p/>
     * If field of given name does not exist it will be ignored as well.
     */
    public void rawSet() {
	    properties.forEach((key, value) -> {
            Config<?> c = configs.get(mapper.apply(key));
            if (c!=null) c.setValueS(value);
        });
    }

/******************************************************************************/

    private static String getGroup(Class<?> c) {
        IsConfigurable a = c.getAnnotation(IsConfigurable.class);
        return a==null || a.value().isEmpty() ? c.getSimpleName() : a.value();
    }

    private void discoverConfigFieldsOf(Class<?> c) {
        collectConfigs(configsOf(c, null, true, false));
    }

    private void discoverMethodsOf(Class<?> c) {
        for (Method m : c.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                for(AppliesConfig a : m.getAnnotationsByType(AppliesConfig.class)) {
                    if (a != null) {
                        String name = a.value();
                        String group = getGroup(c);
                        String config_id = mapper.apply(group + "." + name);
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

    static List<Config<Object>> configsOf(Class<?> clazz, Object instance, boolean include_static, boolean include_instance) {
        if(include_instance && instance==null)
            throw new IllegalArgumentException("Instance must not be null if instance fields flag is true");

        List<Config<Object>> out = new ArrayList<>();

        for(Field f : getAllFields(clazz)) {
            Config c = createConfig(clazz, f, instance, include_static, include_instance);
            if(c!=null) out.add(c);
    }
        return out;
    }

    static Config<?> createConfig(Class<?> cl, Field f, Object instance, boolean include_static, boolean include_instance) {
        Config<?> c = null;
        IsConfig a = f.getAnnotation(IsConfig.class);
        if (a != null) {
            String group = a.group().isEmpty() ? getGroup(cl) : a.group();
            String name = f.getName();
            int modifiers = f.getModifiers();
            if (include_static && Modifier.isStatic(modifiers))
                c = createConfig(f, instance, name, a, group);

            if (include_instance && !Modifier.isStatic(modifiers))
                c = createConfig(f, instance, name, a, group);

        }
        return c;
    }

    private static Config<?> createConfig(Field f, Object instance, String name, IsConfig annotation, String group) {
        Class<?> c = f.getType();
        if(Config.class.isAssignableFrom(c)) {
            return newFromConfig(f, instance);
        } else
        if(WritableValue.class.isAssignableFrom(c) || ReadOnlyProperty.class.isAssignableFrom(c)) {
            return newFromProperty(f, instance, name, annotation, group);
        } else {
            try {
                noFinal(f);                // make sure the field is not final
                f.setAccessible(true);     // make sure the field is accessible
                MethodHandle getter = methodLookup.unreflectGetter(f);
                MethodHandle setter = methodLookup.unreflectSetter(f);
                return new FieldConfig<>(name, annotation, instance, group, getter, setter);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unreflecting field " + f.getName() + " failed. " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Config<?> newFromProperty(Field f, Object instance, String name, IsConfig annotation, String group) {
        try {
            yesFinal(f);                // make sure the field is final
            f.setAccessible(true);      // make sure the field is accessible
            if(VarList.class.isAssignableFrom(f.getType()))
                return new ListConfig<>(name, annotation, (VarList)f.get(instance), group);
            if(Vo.class.isAssignableFrom(f.getType())) {
                Vo<?> property = (Vo)f.get(instance);
                Class<?> property_type = getGenericPropertyType(f.getGenericType());
                return new OverridablePropertyConfig(property_type, name, annotation, property, group);
            }
            if(WritableValue.class.isAssignableFrom(f.getType())) {
                WritableValue<?> property = (WritableValue)f.get(instance);
                Class<?> property_type = getGenericPropertyType(f.getGenericType());
                return new PropertyConfig(property_type, name, annotation, property, group);
            }
            if(ReadOnlyProperty.class.isAssignableFrom(f.getType())) {
                ReadOnlyProperty<?> property = (ReadOnlyProperty)f.get(instance);
                Class<?> property_type = getGenericPropertyType(f.getGenericType());
                return new ReadOnlyPropertyConfig(property_type, name, annotation, property, group);
            }
            throw new IllegalArgumentException("Wrong class");
        } catch (IllegalAccessException | SecurityException e) {
            throw new RuntimeException("Can not access field: " + f.getName() + " for class: " + f.getDeclaringClass());
        }
    }

    private static Config<?> newFromConfig(Field f, Object instance) {
        try {
            yesFinal(f);            // make sure the field is final
            f.setAccessible(true);      // make sure the field is accessible
            return (Config)f.get(instance);
        } catch (IllegalAccessException | SecurityException ex) {
            throw new RuntimeException("Can not access field: " + f.getName() + " for class: " + f.getDeclaringClass());
        }
    }

}