package sp.it.util.conf;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import sp.it.util.access.OrV;
import sp.it.util.conf.ConfigImpl.ConfigBase;
import sp.it.util.conf.ConfigImpl.FieldConfig;
import sp.it.util.conf.ConfigImpl.ListConfig;
import sp.it.util.conf.ConfigImpl.PropertyConfig;
import sp.it.util.conf.ConfigImpl.ReadOnlyPropertyConfig;
import sp.it.util.conf.Constraint.IsConstraint;
import sp.it.util.conf.OrPropertyConfig.OrValue;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static sp.it.util.conf.ConfigDefinitionKt.toDef;
import static sp.it.util.conf.ConfigurationUtilKt.computeConfigGroup;
import static sp.it.util.dev.FailKt.failIfFinal;
import static sp.it.util.dev.FailKt.failIfNotFinal;
import static sp.it.util.dev.FailKt.noNull;
import static sp.it.util.functional.Util.ISNT0;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.type.Util.getAllFields;
import static sp.it.util.type.Util.getRawGenericPropertyType;
import static sp.it.util.type.Util.unPrimitivize;

public class ConfigurationUtil {

	private static final Lookup methodLookup = MethodHandles.lookup();

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
	private static <T> Config<T> createConfig(Field f, T instance, String name, ConfigDefinition annotation, String group) {
		Class<T> type = (Class) f.getType();
		if (Config.class.isAssignableFrom(type)) {
			return newFromConfig(f, instance);
		} else if (ConfList.class.isAssignableFrom(type) || WritableValue.class.isAssignableFrom(type) || ObservableValue.class.isAssignableFrom(type)) {
			return newFromProperty(f, instance, name, annotation, group);
		} else {
			try {
				if (annotation.getEditable()==EditMode.NONE) failIfNotFinal(f); else failIfFinal(f);
				f.setAccessible(true);
				MethodHandle getter = methodLookup.unreflectGetter(f);
				MethodHandle setter = methodLookup.unreflectSetter(f);
				Set<Constraint<? super T>> constraints = constraintsOf(type, f.getAnnotations());
				return new FieldConfig<>(name, annotation, constraints, instance, group, getter, setter);
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Unreflecting field " + f.getName() + " failed. " + e.getMessage());
			}
		}
	}

	@SuppressWarnings({"unchecked", "Convert2Diamond"})
	private static <T> Config<T> newFromProperty(Field f, T instance, String name, ConfigDefinition annotation, String group) {
		try {
			failIfNotFinal(f);
			f.setAccessible(true);
			if (ConfList.class.isAssignableFrom(f.getType())) {
				Class<T> propertyType = getRawGenericPropertyType(f.getGenericType());
				Set<Constraint<? super T>> constraints = constraintsOf(propertyType, f.getAnnotations());
				return (Config<T>) new ListConfig<T>(name, annotation, (ConfList) f.get(instance), group, constraints);
			}
			if (OrV.class.isAssignableFrom(f.getType())) {
				OrV<T> property = (OrV) f.get(instance);
				Class<T> propertyType = getRawGenericPropertyType(f.getGenericType());
				Set<Constraint<? super OrValue<T>>> constraints = constraintsOf((Class) OrValue.class, f.getAnnotations());
				return (Config<T>) new OrPropertyConfig<T>(propertyType, name, annotation, constraints, property, group);
			}
			if (WritableValue.class.isAssignableFrom(f.getType())) {
				WritableValue<T> property = (WritableValue) f.get(instance);
				Class<T> propertyType = getRawGenericPropertyType(f.getGenericType());
				Set<Constraint<? super T>> constraints = constraintsOf(propertyType, f.getAnnotations());
				return new PropertyConfig<T>(propertyType, name, annotation, constraints, property, group);
			}
			if (ObservableValue.class.isAssignableFrom(f.getType())) {
				ObservableValue<T> property = (ObservableValue) f.get(instance);
				Class<T> propertyType = getRawGenericPropertyType(f.getGenericType());
				Set<Constraint<? super T>> constraints = constraintsOf(propertyType, f.getAnnotations());
				return new ReadOnlyPropertyConfig<T>(propertyType, name, annotation, constraints, property, group);
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
						.filter(a ->
							Optional.ofNullable(a.annotationType().getAnnotation(IsConstraint.class))
								.filter(c -> c.value().isAssignableFrom(unPrimitivize(type)))
								.isPresent()
						)
						.map(Constraints::toConstraint),
				Constraints.Companion.getIMPLICIT_CONSTRAINTS().getElementsOfSuper(type).stream()
						.map(constraint -> (Constraint<T>) constraint)
				//		    .filter(constraint -> stream(annotations).noneMatch(annotation.))
		).collect(toSet());
	}
}