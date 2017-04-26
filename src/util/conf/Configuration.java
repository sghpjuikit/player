package util.conf;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.WritableValue;
import org.atteo.classindex.ClassIndex;
import util.access.Vo;
import util.action.Action;
import util.collections.mapset.MapSet;
import util.conf.Config.*;
import util.file.Properties;
import util.file.Properties.Property;
import util.functional.Functors.Ƒ1;
import util.validation.Constraint;
import util.validation.Constraint.IsConstraint;
import static java.util.stream.Collectors.toSet;
import static util.dev.Util.throwIfFinal;
import static util.dev.Util.throwIfNotFinal;
import static util.functional.Util.ISNTØ;
import static util.functional.Util.stream;
import static util.type.Util.*;

/**
 * Provides methods to access configs.
 */
public class Configuration {

	private static final Lookup methodLookup = MethodHandles.lookup();
	private static final Ƒ1<String,String> mapper = s -> s.replaceAll(" ", "_").toLowerCase();

	private final Map<String,String> properties = new ConcurrentHashMap<>();
	private final MapSet<String,Config> configs = new MapSet<>(new ConcurrentHashMap<>(), mapper.compose(c -> c.getGroup() + "." + c.getName()));

	/**
	 * Returns raw key-value ({@link java.lang.String}) pairs representing the serialized configs.
	 *
	 * @return modifiable thread safe map of key-value property pairs
	 */
	public Map<String,String> rawGet() {
		return properties;
	}

	public void rawAddProperty(String name, String value) {
		properties.put(name, value);
	}

	public void rawAddProperties(Map<String,String> _properties) {
		properties.putAll(_properties);
	}

	public void rawAdd(File file) {
		Properties.load(file).forEach(this::rawAddProperty);
	}

	public void rawRemProperty(String key) {
		properties.remove(key);
	}

	public void rawRemProperties(Map<String,String> properties) {
		properties.forEach((name, value) -> rawRemProperty(name));
	}

	public void rawRem(File file) {
		Properties.load(file).forEach((name, value) -> rawRemProperty(name));
	}

	public <C> void collect(Configurable<C> c) {
		collectConfigs(c.getFields());
	}

	public <C> void collect(Collection<? extends Config<C>> c) {
		collectConfigs(c);
	}

	@SafeVarargs
	public final <C> void collect(Configurable<C>... cs) {
		for (Configurable<C> c : cs) collect(c);
	}

	public void collectStatic() {
		// for all discovered classes
		ClassIndex.getAnnotated(IsConfigurable.class).forEach(c -> {
			discoverConfigFieldsOf(c);  // add class fields
			discoverMethodsOf(c);   // add methods in the end to avoid incorrect initialization
		});
	}

	@SuppressWarnings("unchecked")
	private <C> void collectConfigs(Collection<? extends Config<C>> _configs) {
		configs.addAll(_configs);

		// generate boolean toggle actions
		_configs.stream()
				.filter(c -> c.isEditable().isByUser())
				.filter(c -> c.getType()==Boolean.class)
				.map(c -> (Config<Boolean>) c)
				.forEach(c -> {
					String name = c.getGuiName() + " - toggle";
					String description = "Toggles value " + c.getName() + " between true/false";
					Runnable r = c::setNextNapplyValue;
					Action a = new Action(name, r, description, c.getGroup(), "", false, false);
					Action.getActions().add(a);
					configs.add(a);
				});

		// generate enumerable loopNext actions
		// TODO
	}

	public <T> void drop(Config<T> config) {
		configs.remove(config);
	}

	// TODO: add more drop implementations for convenience

	public List<Config> getFields() {
		return new ArrayList<>(configs);
	}

	public List<Config> getFields(Predicate<Config> condition) {
		return stream(getFields()).filter(condition).toList();
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
		String comment = title + " property file" + "\n"
				+ " Last auto-modified: " + java.time.LocalDateTime.now() + "\n"
				+ "\n"
				+ " Properties are in the format: {property path}.{property name}{separator}{property value}\n"
				+ " \t{property path}  must be lowercase with '.' as path separator, e.g.: this.is.a.path\n"
				+ " \t{property name}  must be lowercase and contain no spaces (use underscores '_' instead)\n"
				+ " \t{separator}      must be ' = ' string\n"
				+ " \t{property value} can be any string (even empty)\n"
				+ " Properties must be separated by (any) combination of '\\n', '\\r' characters\n"
				+ "\n"
				+ " Ignored lines:\n"
				+ " \tcomment lines (start with '#')\n"
				+ " \tcomment lines (start with '!')\n"
				+ " \tempty lines\n"
				+ "\n"
				+ " Some properties are read-only or have additional value constraints. Such properties will ignore "
				+ "custom or unfit values";

		// TODO: persist raw properties that are not configs too
		Map<String, Properties.Property> properties = stream(getFields())
				.filter(c -> c.getType()!=Void.class)
				.toMap(configs.keyMapper, c -> new Property(c.getInfo(), c.getValueS()));
		Properties.saveP(file, comment, properties);
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
			if (c!=null && c.isEditable().isByApp()) c.setValueS(value);
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
				for (AppliesConfig a : m.getAnnotationsByType(AppliesConfig.class)) {
					if (a!=null) {
						String name = a.value();
						String group = getGroup(c);
						String config_id = mapper.apply(group + "." + name);
						if (configs.containsKey(config_id) && !name.isEmpty()) {
							Config config = configs.get(config_id);
							if (config instanceof FieldConfig) {
								try {
									m.setAccessible(true);
									((FieldConfig) config).applier = methodLookup.unreflect(m);
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

	@SuppressWarnings("unchecked")
	static List<Config<Object>> configsOf(Class<?> clazz, Object instance, boolean include_static, boolean include_instance) {
		if (include_instance && instance==null)
			throw new IllegalArgumentException("Instance must not be null if instance fields flag is true");

		return (List) stream(getAllFields(clazz))
				.map(f -> createConfig(clazz, f, instance, include_static, include_instance))
				.filter(ISNTØ)
				.toList();
	}

	static Config<?> createConfig(Class<?> cl, Field f, Object instance, boolean include_static, boolean include_instance) {
		Config<?> c = null;
		IsConfig a = f.getAnnotation(IsConfig.class);
		if (a!=null) {
			String group = a.group().isEmpty() ? getGroup(cl) : a.group();
			String name = f.getName();
			int modifiers = f.getModifiers();
			if ((include_static && Modifier.isStatic(modifiers)) || (include_instance && !Modifier.isStatic(modifiers)))
				c = createConfig(f, instance, name, a, group);
		}
		return c;
	}

	@SuppressWarnings("unchecked")
	private static <T> Config<T> createConfig(Field f, T instance, String name, IsConfig annotation, String group) {
		Class<T> type = (Class) f.getType();
		if (Config.class.isAssignableFrom(type)) {
			return newFromConfig(f, instance);
		} else if (WritableValue.class.isAssignableFrom(type) || ReadOnlyProperty.class.isAssignableFrom(type)) {
			return newFromProperty(f, instance, name, annotation, group);
		} else {
			try {
				throwIfFinal(f);                // make sure the field is not final
				f.setAccessible(true);     // make sure the field is accessible
				MethodHandle getter = methodLookup.unreflectGetter(f);
				MethodHandle setter = methodLookup.unreflectSetter(f);
				Set<Constraint<? super T>> constraints = constraintsOf(type, f.getAnnotations());
				return new FieldConfig(name, annotation, constraints, instance, group, getter, setter);
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Unreflecting field " + f.getName() + " failed. " + e.getMessage());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Config<T> newFromProperty(Field f, T instance, String name, IsConfig annotation, String group) {
		try {
			throwIfNotFinal(f);                // make sure the field is final
			f.setAccessible(true);      // make sure the field is accessible
			if (VarList.class.isAssignableFrom(f.getType())) {
				Class<T> property_type = getGenericPropertyType(f.getGenericType());
				Set<Constraint<? super T>> constraints = constraintsOf(property_type, f.getAnnotations());
				return new ListConfig<>(name, annotation, (VarList) f.get(instance), group, constraints);
			}
			if (Vo.class.isAssignableFrom(f.getType())) {
				Vo<T> property = (Vo) f.get(instance);
				Class<T> property_type = getGenericPropertyType(f.getGenericType());
				Set<Constraint<? super T>> constraints = constraintsOf(property_type, f.getAnnotations());
				return new OverridablePropertyConfig<>(property_type, name, annotation, constraints, property, group);
			}
			if (WritableValue.class.isAssignableFrom(f.getType())) {
				WritableValue<T> property = (WritableValue) f.get(instance);
				Class<T> property_type = getGenericPropertyType(f.getGenericType());
				Set<Constraint<? super T>> constraints = constraintsOf(property_type, f.getAnnotations());
				return new PropertyConfig<>(property_type, name, annotation, constraints, property, group);
			}
			if (ReadOnlyProperty.class.isAssignableFrom(f.getType())) {
				ReadOnlyProperty<T> property = (ReadOnlyProperty) f.get(instance);
				Class<T> property_type = getGenericPropertyType(f.getGenericType());
				Set<Constraint<? super T>> constraints = constraintsOf(property_type, f.getAnnotations());
				return new ReadOnlyPropertyConfig<>(property_type, name, annotation, constraints, property, group);
			}
			throw new IllegalArgumentException("Wrong class");
		} catch (IllegalAccessException|SecurityException e) {
			throw new RuntimeException("Can not access field: " + f.getName() + " for class: " + f.getDeclaringClass());
		}
	}

	// TODO: support annotations
	@SuppressWarnings("unchecked")
	private static <T> Config<T> newFromConfig(Field f, Object instance) {
		try {
			throwIfNotFinal(f);                // make sure the field is final
			f.setAccessible(true);      // make sure the field is accessible
			Config<T> config = (Config<T>) f.get(instance);
			((ConfigBase) config).constraints = constraintsOf(config.getType(), f.getAnnotations());
			return config;
		} catch (IllegalAccessException|SecurityException ex) {
			throw new RuntimeException("Can not access field: " + f.getName() + " for class: " + f.getDeclaringClass());
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Set<Constraint<? super T>> constraintsOf(Class<T> type, Annotation[] annotations) {
		return (Set) Stream.concat(
				stream(annotations)
						// restrict to annotations marked to be constraint annotations
						.filter(a -> Optional.ofNullable(a.annotationType().getAnnotation(IsConstraint.class))
								.filter(c -> c.value().isAssignableFrom(unPrimitivize(type))).isPresent())
						.map(Constraint::toConstraint),
				Constraint.IMPLICIT_CONSTRAINTS.getElementsOfSuper(type).stream()
						.map(constraint -> (Constraint<T>) constraint)
				//		    .filter(constraint -> stream(annotations).noneMatch(annotation.))
		).collect(toSet());
	}

}