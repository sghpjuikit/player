package sp.it.util.conf;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.WritableValue;
import sp.it.util.access.OrV;
import sp.it.util.conf.ConfigImpl.ConfigBase;
import sp.it.util.conf.ConfigImpl.FieldConfig;
import sp.it.util.conf.ConfigImpl.ListConfig;
import sp.it.util.conf.ConfigImpl.PropertyConfig;
import sp.it.util.conf.ConfigImpl.ReadOnlyPropertyConfig;
import sp.it.util.conf.OrPropertyConfig.OrValue;
import sp.it.util.validation.Constraint;
import sp.it.util.validation.Constraint.IsConstraint;
import sp.it.util.validation.Constraints;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static sp.it.util.conf.ConfigurationUtilKt.obtainConfigGroup;
import static sp.it.util.dev.FailKt.failIfFinal;
import static sp.it.util.dev.FailKt.failIfNotFinal;
import static sp.it.util.functional.Util.ISNTØ;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.type.Util.getAllFields;
import static sp.it.util.type.Util.getRawGenericPropertyType;
import static sp.it.util.type.Util.unPrimitivize;

public class ConfigurationUtil {

	private static final Lookup methodLookup = MethodHandles.lookup();

	@SuppressWarnings("unchecked")
	public static List<Config<Object>> configsOf(Class<?> clazz, Object instance, boolean include_static, boolean include_instance) {
		if (include_instance && instance==null)
			throw new IllegalArgumentException("Instance must not be null if instance fields flag is true");

		return (List) stream(getAllFields(clazz))
				.map(f -> createConfig(clazz, f, instance, include_static, include_instance))
				.filter(ISNTØ)
				.collect(toList());
	}

	@SuppressWarnings("unchecked")
	public static List<Config<Object>> configsOf(Class<?> clazz, String fieldNamePrefix, String category, Object instance, boolean include_static, boolean include_instance) {
		if (include_instance && instance==null)
			throw new IllegalArgumentException("Instance must not be null if instance fields flag is true");

		return (List) stream(getAllFields(clazz))
			.map(f -> createConfig(clazz, fieldNamePrefix, category, f, instance, include_static, include_instance))
			.filter(ISNTØ)
			.collect(toList());
	}

	@SuppressWarnings("unchecked")
	public static Config<?> createConfig(Class<?> cl, Field f, Object instance, boolean include_static, boolean include_instance) {
		Config<?> c = null;
		IsConfig a = f.getAnnotation(IsConfig.class);
		if (a!=null) {
			String group = obtainConfigGroup(a, (Class) cl, instance);
			String name = f.getName();
			int modifiers = f.getModifiers();
			if ((include_static && Modifier.isStatic(modifiers)) || (include_instance && !Modifier.isStatic(modifiers)))
				c = createConfig(f, instance, name, a, group);
		}
		return c;
	}

	private static Config<?> createConfig(Class<?> cl, String fieldNamePrefix, String category, Field f, Object instance, boolean include_static, boolean include_instance) {
		Config<?> c = null;
		IsConfig a = f.getAnnotation(IsConfig.class);
		if (a!=null) {
			String group = category;
			String name = fieldNamePrefix + f.getName();
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
		} else if (ConfList.class.isAssignableFrom(type) || WritableValue.class.isAssignableFrom(type) || ReadOnlyProperty.class.isAssignableFrom(type)) {
			return newFromProperty(f, instance, name, annotation, group);
		} else {
			try {
				if (annotation.editable()==EditMode.NONE) failIfNotFinal(f); else failIfFinal(f);
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
	private static <T> Config<T> newFromProperty(Field f, T instance, String name, IsConfig annotation, String group) {
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
			if (ReadOnlyProperty.class.isAssignableFrom(f.getType())) {
				ReadOnlyProperty<T> property = (ReadOnlyProperty) f.get(instance);
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