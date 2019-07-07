package sp.it.util.conf;

import java.lang.invoke.MethodHandle;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.ObservableList;
import kotlin.jvm.functions.Function1;
import sp.it.util.access.EnumerableValue;
import sp.it.util.file.properties.PropVal;
import sp.it.util.validation.Constraint;
import sp.it.util.validation.Constraint.HasNonNullElements;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static sp.it.util.collections.UtilKt.setTo;
import static sp.it.util.dev.FailKt.failIf;
import static sp.it.util.file.properties.PropVal.PropVal1;
import static sp.it.util.file.properties.PropVal.PropValN;
import static sp.it.util.functional.Util.forEachBoth;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.Util.map;
import static sp.it.util.functional.Util.setRO;
import static sp.it.util.functional.Util.split;
import static sp.it.util.functional.UtilKt.compose;
import static sp.it.util.type.Util.getValueFromFieldMethodHandle;
import static sp.it.util.type.Util.unPrimitivize;

public interface ConfigImpl {

	abstract class ConfigBase<T> extends Config<T> {

		private final Class<T> type;
		private final String gui_name;
		private final String name;
		private final String group;
		private final String info;
		private final EditMode editable;
		private final T defaultValue;
		Set<Constraint<? super T>> constraints;

		ConfigBase(Class<T> type, String name, String gui_name, T val, String category, String info, EditMode editable) {
			this.type = unPrimitivize(type);
			this.gui_name = gui_name;
			this.name = name;
			this.defaultValue = val;
			this.group = category;
			this.info = info==null || info.isEmpty() ? gui_name : info;
			this.editable = editable;
		}

		/**
		 * @throws NullPointerException if val parameter null. The wrapped value must no be null.
		 */
		ConfigBase(Class<T> type, String name, IsConfig c, T val, String category) {
			this(type, name, c.name().isEmpty() ? name : c.name(), val, category, c.info(), c.editable());
		}

		ConfigBase(Class<T> type, String name, IsConfig c, Set<Constraint<? super T>> constraints, T val, String category) {
			this(type, name, c.name().isEmpty() ? name : c.name(), val, category, c.info(), c.editable());
			this.constraints = constraints;
		}

		@Override
		public final String getGuiName() {
			return gui_name;
		}

		@Override
		public final String getName() {
			return name;
		}

		@Override
		public final String getGroup() {
			return group;
		}

		@Override
		public Class<T> getType() {
			return type;
		}

		@Override
		public final String getInfo() {
			return info;
		}

		@Override
		public final EditMode isEditable() {
			return editable;
		}

		@SuppressWarnings({"unchecked", "RedundantCast"})
		@Override
		public Set<Constraint<T>> getConstraints() {
			return constraints==null ? setRO() : (Set) constraints;
		}

		@SafeVarargs
		@Override
		public final ConfigBase<T> addConstraints(Constraint<? super T>... constraints) {
			if (this.constraints==null) this.constraints = new HashSet<>(constraints.length);
			this.constraints.addAll(asList(constraints));
			return this;
		}

		@Override
		public T getDefaultValue() {
			return defaultValue;
		}
	}

	/** {@link Config} wrapping {@link java.lang.reflect.Field}. Can wrap both static or instance fields. */
	class FieldConfig<T> extends ConfigBase<T> {

		private final Object instance;
		private final MethodHandle getter;
		private final MethodHandle setter;

		/**
		 * @param instance owner of the field or null if static
		 */
		@SuppressWarnings("unchecked")
		FieldConfig(String name, IsConfig c, Set<Constraint<? super T>> constraints, Object instance, String category, MethodHandle getter, MethodHandle setter) {
			super((Class) getter.type().returnType(), name, c, constraints, getValueFromFieldMethodHandle(getter, instance), category);
			this.getter = getter;
			this.setter = setter;
			this.instance = instance;
		}

		@Override
		public T getValue() {
			return getValueFromFieldMethodHandle(getter, instance);
		}

		@Override
		public void setValue(T val) {
			try {
				if (instance==null) setter.invokeWithArguments(val);
				else setter.invokeWithArguments(instance, val);
			} catch (Throwable e) {
				throw new RuntimeException("Error setting config field " + getName(), e);
			}
		}

	}

	class PropertyConfig<T> extends ConfigBase<T> {

		protected final WritableValue<T> value;

		/**
		 * Constructor to be used with framework
		 *
		 * @param c the annotation
		 * @param property WritableValue to wrap. Mostly a {@link Property}.
		 * @throws IllegalStateException if the property field is not final
		 */
		@SuppressWarnings("unchecked")
		public PropertyConfig(Class<T> propertyType, String name, IsConfig c, Set<Constraint<? super T>> constraints, WritableValue<T> property, T defaultValue, String category) {
			super(propertyType, name, c, constraints, defaultValue, category);
			value = property;

			if (value instanceof EnumerableValue)
				setValueEnumerator2nd(((EnumerableValue<T>) value)::enumerateValues);
		}

		/**
		 * Constructor to be used with framework
		 *
		 * @param c the annotation
		 * @param property WritableValue to wrap. Mostly a {@link Property}.
		 * @throws IllegalStateException if the property field is not final
		 */
		@SuppressWarnings("unchecked")
		public PropertyConfig(Class<T> propertyType, String name, IsConfig c, Set<Constraint<? super T>> constraints, WritableValue<T> property, String category) {
			super(propertyType, name, c, constraints, property.getValue(), category);
			value = property;

			if (value instanceof EnumerableValue)
				setValueEnumerator2nd(((EnumerableValue<T>) value)::enumerateValues);
		}

		/**
		 * @param property WritableValue to wrap. Mostly a {@link Property}.
		 * @param category category, for generating config groups
		 * @param info description, for tooltip for example
		 * @throws IllegalStateException if the property field is not final
		 */
		@SuppressWarnings("unchecked")
		public PropertyConfig(Class<T> propertyType, String name, String gui_name, WritableValue<T> property, String category, String info, EditMode editable) {
			super(propertyType, name, gui_name, property.getValue(), category, info, editable);
			value = property;

			if (value instanceof EnumerableValue)
				setValueEnumerator2nd(((EnumerableValue<T>) value)::enumerateValues);
		}

		/**
		 * @param property WritableValue to wrap. Mostly a {@link Property}.
		 * @param info description, for tooltip for example
		 * @throws IllegalStateException if the property field is not final
		 */
		public PropertyConfig(Class<T> propertyType, String name, WritableValue<T> property, String info) {
			this(propertyType, name, name, property, "", info, EditMode.USER);
		}

		@Override
		public T getValue() {
			return value.getValue();
		}

		@Override
		public void setValue(T val) {
			value.setValue(val);
		}

		public WritableValue<T> getProperty() {
			return value;
		}

	}

	class ReadOnlyPropertyConfig<T> extends ConfigBase<T> {

		private final ObservableValue<T> value;

		/**
		 * Constructor to be used with framework
		 *
		 * @param c the annotation
		 * @param property WritableValue to wrap. Mostly a {@link Property}.
		 * @throws IllegalStateException if the property field is not final
		 */
		@SuppressWarnings("unchecked")
		public ReadOnlyPropertyConfig(Class<T> property_type, String name, IsConfig c, Set<Constraint<? super T>> constraints, ObservableValue<T> property, String category) {
			super(property_type, name, c, constraints, property.getValue(), category);
			value = property;

			if (value instanceof EnumerableValue)
				setValueEnumerator2nd(((EnumerableValue<T>) value)::enumerateValues);
		}

		/**
		 * @param property WritableValue to wrap. Mostly a {@link Property}.
		 * @param category category, for generating config groups
		 * @param info description, for tooltip for example
		 * @throws IllegalStateException if the property field is not final
		 */
		@SuppressWarnings("unchecked")
		public ReadOnlyPropertyConfig(Class<T> property_type, String name, String gui_name, ObservableValue<T> property, String category, String info) {
			super(property_type, name, gui_name, property.getValue(), category, info, EditMode.NONE);
			value = property;

			if (value instanceof EnumerableValue)
				setValueEnumerator2nd(((EnumerableValue<T>) value)::enumerateValues);
		}

		/**
		 * @param property WritableValue to wrap. Mostly a {@link Property}.
		 * @param info description, for tooltip for example
		 * @throws IllegalStateException if the property field is not final
		 */
		public ReadOnlyPropertyConfig(Class<T> property_type, String name, ObservableValue<T> property, String info) {
			this(property_type, name, name, property, "", info);
		}

		@Override
		public T getValue() {
			return value.getValue();
		}

		@Override
		public void setValue(T val) {}

		public ObservableValue<T> getProperty() {
			return value;
		}

	}

	class ListConfig<T> extends ConfigBase<ObservableList<T>> {

		public final ConfList<T> a;
		public final Function1<? super T,? extends Configurable<?>> toConfigurable;
		private final List<T> defaultItems;

		@SuppressWarnings("unchecked")
		public ListConfig(String name, IsConfig c, ConfList<T> list, String category, Set<Constraint<? super T>> constraints) {
			super((Class) ObservableList.class, name, c, list.list, category);
			failIf(isReadOnly(list.list)!=(c.editable()==EditMode.NONE));

			a = list;
			defaultItems = isFixedSizeAndHasConfigurableItems() ? null : list(list.list);
			toConfigurable = compose(list.toConfigurable, configurable -> {
				if (configurable instanceof Config)
					((Config) configurable).addConstraints(constraints);
				return configurable;
			});

			if (!list.isNullable) addConstraints(HasNonNullElements.INSTANCE);
		}

		@SuppressWarnings("unchecked")
		public ListConfig(String name, String gui_name, ConfList<T> list, String category, String info, EditMode editable) {
			super((Class) ObservableList.class, name, gui_name, list.list, category, info, isReadOnly(list.list) ? EditMode.NONE :  editable);

			a = list;
			defaultItems = isFixedSizeAndHasConfigurableItems() ? null : list(list.list);
			toConfigurable = list.toConfigurable;

			if (!list.isNullable) addConstraints(HasNonNullElements.INSTANCE);
		}

		public ListConfig(String name, ConfList<T> val) {
			this(name, name, val, "", "", EditMode.USER);
		}

		@Override
		public ObservableList<T> getValue() {
			return a.list;
		}

		@Override
		public void setValue(ObservableList<T> val) {}

		@Override
		public void setValueToDefault() {
			if (isFixedSizeAndHasConfigurableItems())
				a.list.setAll(defaultItems);
		}

		@Override
		public PropVal getValueAsProperty() {
			return new PropValN(
				map(
					getValue(),
					t -> a.toConfigurable.invoke(t).getFields().stream()
						.map(c -> {
							var p = c.getValueAsProperty();
							failIf(p.getValN().size()!=1, () -> "Only single-value within multi value is supported"); // TODO: support multi-value
							return p.getVal1();
						})
						.collect(joining(";"))
				)
			);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void setValueAsProperty(PropVal property) {
			boolean isFixedSizeAndHasConfigurableItems = isFixedSizeAndHasConfigurableItems();
			AtomicInteger i = isFixedSizeAndHasConfigurableItems ? new AtomicInteger(0) : null;
			var values = property.getValN().stream()
				.map(s -> {
					T t = isFixedSizeAndHasConfigurableItems ? a.list.get(i.getAndIncrement()) : a.factory.get();
					List<Config> configs = list(a.toConfigurable.invoke(t).getFields());
					List<String> cValues = split(s, ";");
					if (configs.size()==cValues.size())
						forEachBoth(configs, cValues, (c, v) -> c.setValueAsProperty(new PropVal1(v))); // TODO: support multi-value

					return (T) (a.itemType.isAssignableFrom(configs.get(0).getType()) ? configs.get(0).getValue() : t);
				})
				.filter(a.isNullable ? (it -> true) : (it -> it!=null))
				.collect(toList());
			setTo(a.list, values);
		}

		private boolean isFixedSizeAndHasConfigurableItems() {
			return a.factory==ConfList.FailFactory;
		}

		private static boolean isReadOnly(ObservableList<?> list) {
			return list.getClass().getSimpleName().toLowerCase().contains("unmodifiable");
		}

	}

}