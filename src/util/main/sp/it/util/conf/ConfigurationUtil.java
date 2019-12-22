package sp.it.util.conf;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import sp.it.util.access.OrV;
import static java.util.stream.Collectors.toList;
import static sp.it.util.conf.ConfigDefinitionKt.toDef;
import static sp.it.util.conf.ConfigurationUtilKt.computeConfigGroup;
import static sp.it.util.dev.FailKt.failIfFinal;
import static sp.it.util.dev.FailKt.failIfNotFinal;
import static sp.it.util.dev.FailKt.noNull;
import static sp.it.util.functional.Util.ISNT0;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.type.Util.getAllFields;
import static sp.it.util.type.Util.getRawGenericPropertyType;

public class ConfigurationUtil {

	@SuppressWarnings("unchecked")
	public static List<Config<Object>> configsOf(Class<?> clazz, Object instance) {
		noNull(instance);

		return (List) stream(getAllFields(clazz))
				.map(f -> createConfig(f, instance))
				.filter(ISNT0)
				.collect(toList());
	}

	@SuppressWarnings("unchecked")
	public static List<Config<Object>> configsOf(Class<?> clazz, String fieldNamePrefix, String group, Object instance) {
		noNull(instance);

		return (List) stream(getAllFields(clazz))
			.map(f -> createConfig(fieldNamePrefix, group, f, instance))
			.filter(ISNT0)
			.collect(toList());
	}

	public static Config<?> createConfig(Field f, Object instance) {
		noNull(instance);

		Config<?> c = null;
		IsConfig a = f.getAnnotation(IsConfig.class);
		if (a!=null) {
			String group = computeConfigGroup(a, instance);
			String name = f.getName();
			c = createConfig(f, instance, name, toDef(a), group);
		}
		return c;
	}

	private static Config<?> createConfig(String fieldNamePrefix, String group, Field f, Object instance) {
		Config<?> c = null;
		IsConfig a = f.getAnnotation(IsConfig.class);
		if (a!=null) {
			String name = fieldNamePrefix + f.getName();
			int modifiers = f.getModifiers();
			c = createConfig(f, instance, name, toDef(a), group);
		}
		return c;
	}

	@SuppressWarnings("unchecked")
	private static <T> Config<T> createConfig(Field f, T instance, String name, ConfigDefinition def, String group) {
		Class<T> type = (Class) f.getType();
		if (Config.class.isAssignableFrom(type)) {
			return newFromConfig(f, instance);
		} else if (ConfList.class.isAssignableFrom(type) || WritableValue.class.isAssignableFrom(type) || ObservableValue.class.isAssignableFrom(type)) {
			return newFromProperty(f, instance, name, def, group);
		} else {
			if (def.getEditable()==EditMode.NONE) failIfNotFinal(f); else failIfFinal(f);
			f.setAccessible(true);
			return new FieldConfig<>(name, def, Set.of(), instance, group, f);
		}
	}

	@SuppressWarnings({"unchecked", "Convert2Diamond"})
	private static <T> Config<T> newFromProperty(Field f, T instance, String name, ConfigDefinition def, String group) {
		try {
			failIfNotFinal(f);
			f.setAccessible(true);
			if (ConfList.class.isAssignableFrom(f.getType())) {
				Class<T> propertyType = getRawGenericPropertyType(f.getGenericType());
				return (Config<T>) new ListConfig<T>(name, def, (ConfList) f.get(instance), group, Set.of(), Set.of());
			}
			if (OrV.class.isAssignableFrom(f.getType())) {
				OrV<T> property = (OrV) f.get(instance);
				Class<T> propertyType = getRawGenericPropertyType(f.getGenericType());
				return (Config<T>) new OrPropertyConfig<T>(propertyType, name, def, Set.of(), property, group);
			}
			if (WritableValue.class.isAssignableFrom(f.getType())) {
				WritableValue<T> property = (WritableValue) f.get(instance);
				Class<T> propertyType = getRawGenericPropertyType(f.getGenericType());
				return new PropertyConfig<T>(propertyType, name, def, Set.of(), property, group);
			}
			if (ObservableValue.class.isAssignableFrom(f.getType())) {
				ObservableValue<T> property = (ObservableValue) f.get(instance);
				Class<T> propertyType = getRawGenericPropertyType(f.getGenericType());
				return new ReadOnlyPropertyConfig<T>(propertyType, name, def, Set.of(), property, group);
			}
			throw new IllegalArgumentException("Wrong class");
		} catch (IllegalAccessException|SecurityException e) {
			throw new RuntimeException("Can not access field: " + f.getName() + " for class: " + f.getDeclaringClass());
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Config<T> newFromConfig(Field f, Object instance) {
		try {
			failIfNotFinal(f);
			f.setAccessible(true);
			return (Config<T>) f.get(instance);
		} catch (IllegalAccessException|SecurityException ex) {
			throw new RuntimeException("Can not access field: " + f.getName() + " for class: " + f.getDeclaringClass());
		}
	}

}