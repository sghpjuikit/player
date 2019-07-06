package sp.it.util.conf;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import sp.it.util.access.EnumerableValue;
import sp.it.util.access.TypedValue;
import sp.it.util.access.V;
import sp.it.util.access.OrV;
import sp.it.util.conf.Config.VarList.Elements;
import sp.it.util.dev.Dependency;
import sp.it.util.file.properties.PropVal;
import sp.it.util.functional.Functors.Ƒ1;
import sp.it.util.functional.Try;
import sp.it.util.parsing.Parsers;
import sp.it.util.type.Util;
import sp.it.util.validation.Constraint;
import sp.it.util.validation.Constraint.HasNonNullElements;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javafx.collections.FXCollections.observableArrayList;
import static sp.it.util.collections.UtilKt.setTo;
import static sp.it.util.conf.Config.VarList.NULL_SUPPLIER;
import static sp.it.util.dev.DebugKt.logger;
import static sp.it.util.dev.FailKt.failIf;
import static sp.it.util.file.properties.PropVal.PropVal1;
import static sp.it.util.file.properties.PropVal.PropValN;
import static sp.it.util.functional.Try.Java.error;
import static sp.it.util.functional.Try.Java.ok;
import static sp.it.util.functional.Util.firstNotNull;
import static sp.it.util.functional.Util.forEachBoth;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.Util.map;
import static sp.it.util.functional.Util.setRO;
import static sp.it.util.functional.Util.split;
import static sp.it.util.type.Util.getEnumConstants;
import static sp.it.util.type.Util.getValueFromFieldMethodHandle;
import static sp.it.util.type.Util.isEnum;
import static sp.it.util.type.Util.unPrimitivize;
import static sp.it.util.type.UtilKt.toRaw;

/**
 * Object representation of a configurable value.
 * <p/>
 * Config encapsulates access to a value. It allows to obtain the value or
 * change it and also provides additional information associated with it.
 * <p/>
 * Useful for creating {@link Configurable} objects or exporting values from
 * objects in a standardized way.
 * <p/>
 * An aggregation of configs is {@link Configurable}. Note that, technically,
 * config is a singleton configurable. Therefore config actually implements
 * it and can be used as non aggregate configurable type.
 * <p/>
 * Because config is convertible from String and back it also provides convert
 * methods and implements {@link sp.it.util.parsing.ConverterString}.
 *
 * @param <T> type of value of this config
 */
public abstract class Config<T> implements WritableValue<T>, Configurable<T>, TypedValue<T>, EnumerableValue<T> {

	private static final Logger LOGGER = logger(Config.class);

	@Override
	public abstract T getValue();

	@Override
	public abstract void setValue(T val);

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Semantically equivalent to getValue().getClass(), but null-safe and
	 * potentially better performing.
	 */
	@Override
	abstract public Class<T> getType();

	/**
	 * Alternative name of this config. Intended to be human readable and
	 * appropriately formated.
	 * <p/>
	 * Default value is set to be equivalent to name, but can be specified to
	 * differ. Always use for gui building instead of {@link #getName()}.
	 */
	abstract public String getGuiName();

	/**
	 * Name of this config.
	 */
	abstract public String getName();

	/**
	 * Category or group this config belongs to. Use arbitrarily to group
	 * multiple configs together - mostly semantically or by intention.
	 */
	abstract public String getGroup();

	/**
	 * Description of this config
	 */
	abstract public String getInfo();

	/**
	 * Indicates editability. Use arbitrarily. Most often sets whether this
	 * config should be editable by user via graphical user interface.
	 */
	abstract public EditMode isEditable();

	abstract public Set<Constraint<? super T>> getConstraints();

	@SuppressWarnings("unchecked")
	abstract public Config<T> constraints(Constraint<? super T>... constraints);

	@SuppressWarnings({"unchecked", "unused"})
	public Config<T> constraints(Collection<Constraint<? super T>> constraints) {
		return constraints(constraints.toArray(new Constraint[constraints.size()]));
	}

/******************************* default value ********************************/

	/** @return default value of this config (it is the first value of this config) */
	abstract public T getDefaultValue();

	public void setValueToDefault() {
		setValue(getDefaultValue());
	}

/******************************** converting **********************************/

	public PropVal getValueAsProperty() {
		return new PropVal1(Parsers.DEFAULT.toS(getValue()));
	}

	public void setValueAsProperty(PropVal property) {
		var s = property.getVal1();
		if (s!=null)
			convertValueFromString(this, s)
				.ifOkUse(this::setValue)
				.ifErrorUse(e -> LOGGER.warn("Unable to set config={} value from text={}, reason={}", getName(), s, e));
	}

	/** Helper method. Expert API. */
	public static <T> Try<T,String> convertValueFromString(Config<T> config, String s) {
		if (config.isTypeEnumerable()) {
			// 1 Notice we are traversing all enumerated values to look up the one which we want to
			//   deserialize.
			//   We do this by converting each value to string and compare. This is potentially
			//   inefficient operation. It is much better to parse the string to value first and
			//   then compare objects. The problem is Im not sure if relying on Object equals() is
			//   very safe, this should be investigated and optimized.
			//
			// 2 OverridableConfig adds additional information as a prefix when serializing the
			//   value, then removing the prefix when deserializing. This causes the lookup not work
			//   because toS adds the prefix to the values and the string parameter of this method
			//   has it already removed. To bypass this, we rely on Converter.toS/fromS directly,
			//   rather than Config.toS/fromS. This is also dangerous. Of course we could fix this
			//   by having OverridableConfig provide its own implementation, but I don't want to
			//   spread problematic code such as this around. Not till 1 gets fixed up.
			for (T v : config.enumerateValues())
				if (Parsers.DEFAULT.toS(v).equalsIgnoreCase(s)) return ok(v);

			logger(Config.class).warn("Cant parse '{}'. No enumerable value for: {}. Using default value.", s, config.getGuiName());
			return error("Value does not correspond to any value of the enumeration.");
		} else {
			return Parsers.DEFAULT.ofS(config.getType(), s);
		}
	}

	/*************************** configurable methods *****************************/

	Supplier<Collection<T>> valueEnumerator;
	private boolean init = false;

	public boolean isTypeEnumerable() {
		if (!init && valueEnumerator==null) {
			valueEnumerator = buildEnumEnumerator(getDefaultValue());
			init = true;
		}
		return valueEnumerator!=null;
	}

	@Override
	public Collection<T> enumerateValues() {
		if (isTypeEnumerable()) return valueEnumerator.get();
		throw new RuntimeException(getType() + " not enumerable.");
	}

	@SuppressWarnings("unchecked")
	private Supplier<Collection<T>> buildEnumEnumerator(T v) {
		var vs = (Constraint.ValueSet<T>) getConstraints().stream().filter(it -> it instanceof Constraint.ValueSet).findFirst().orElse(null);
		if (vs!=null) return () -> vs.getEnumerator().invoke();

		Class c = v==null ? Void.class : v.getClass();
		return isEnum(c) ? () -> list(getEnumConstants(c)) : null;
	}

/*************************** configurable methods *****************************/

	/**
	 * This method is inherited from Configurable and is not intended to be used
	 * manually on objects of this class, rather, in situations this config
	 * acts as singleton {@link Configurable}.
	 * <p/>
	 * {@inheritDoc }
	 * <p/>
	 * Implementation details: returns self if name equals with parameter or null
	 * otherwise
	 *
	 * @throws IllegalArgumentException if name doent equal name of this config.
	 */
	@Override
	public final Config<T> getField(String name) {
		if (!name.equals(getName())) throw new IllegalArgumentException("Name mismatch");
		else return this;
	}

	/**
	 * This method is inherited from Configurable and is not intended to be used
	 * manually on objects of this class, rather, in situations this config
	 * acts as singleton {@link Configurable}.
	 * <p/>
	 * {@inheritDoc }
	 * <p/>
	 * Implementation details: returns singleton list of self.
	 */
	@Override
	public final List<Config<T>> getFields() {
		return list(this);
	}

/********************************* CREATING ***********************************/

	/**
	 * Creates config for value. Attempts in order:
	 * <ul>
	 *     <li> {@link #forProperty(Class, String, Object)}
	 *     <li> create {@link sp.it.util.conf.Config.ListConfig} if the value is {@link javafx.collections.ObservableList}
	 *     <li> wraps the value in {@link sp.it.util.access.V} and calls {@link #forProperty(Class, String, Object)}
	 * </ul>
	 */
	@SuppressWarnings({"unchecked", "Convert2Diamond"})
	public static <T> Config<T> forValue(Type type, String name, Object value) {
		return firstNotNull(
			() -> forPropertyImpl((Class) toRaw(type), name, value),
			() -> {
				if (value instanceof ObservableList) {
					Class<T> itemType = firstNotNull(() -> {
						if (type instanceof ParameterizedType) {
							Type[] genericTypes = ((ParameterizedType) type).getActualTypeArguments();
							var gt = genericTypes.length==0 ? null : toRaw(genericTypes[0]);
							return gt==null ? Object.class : gt;
						} else {
							return (Class) Object.class;
						}
					});
					return new ListConfig<T>(name, new VarList<T>(itemType, Elements.NULLABLE, (ObservableList) value));
				} else {
					return forProperty(toRaw(type), name, new V<>(value));
				}
			}
		);
	}

	/**
	 * Creates config for property. Te property will become the underlying data
	 * of the config and thus reflect any value changes and vice versa. If
	 * the property is read only, config will also be read only (its set()
	 * methods will not do anything). If the property already is config, it is
	 * returned.
	 *
	 * @param name of of the config, will be used as gui name
	 * @param property underlying property for the config. The property must be instance of any of:
	 * <ul>
	 *     <li> {@link Config}
	 *     <li> {@link VarList}
	 *     <li> {@link WritableValue}
	 *     <li> {@link ObservableValue}
     * </ul>
	 * so standard javafx properties will all work. If not instance of any of the above, runtime exception will be thrown.
	 */
	public static <T> Config<T> forProperty(Class<T> type, String name, Object property) {
		return firstNotNull(
			() -> forPropertyImpl(type, name, property),
			() -> {
				throw new RuntimeException("Property " + name + " must be WritableValue or ReadOnlyValue, but is " + property.getClass());
			}
		);
	}

	@SuppressWarnings("unchecked")
	private static <T> Config<T> forPropertyImpl(Class<T> type, String name, Object property) {
		if (property instanceof Config)
			return (Config<T>) property;
		if (property instanceof VarList)
			return new ListConfig(name, (VarList) property);
		if (property instanceof OrV)
			return new OverridablePropertyConfig<>(type, name, (OrV<T>) property);
		if (property instanceof WritableValue)
			return new PropertyConfig<>(type, name, (WritableValue<T>) property, "");
		if (property instanceof ObservableValue)
			return new ReadOnlyPropertyConfig<>(type, name, (ObservableValue<T>) property, "");
		return null;
	}

	/******************************* IMPLEMENTATIONS ******************************/

	public static abstract class ConfigBase<T> extends Config<T> {

		private final Class<T> type;
		private final String gui_name;
		private final String name;
		private final String group;
		private final String info;
		private final EditMode editable;
		@Dependency("DO NOT RENAME - accessed using reflection")
		private final T defaultValue;
		Set<Constraint<? super T>> constraints;

		// TODO: make static map for valueEnumerators
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

		@Override
		public Set<Constraint<? super T>> getConstraints() {
			return constraints==null ? setRO() : constraints;
		}

		@SafeVarargs
		@Override
		public final ConfigBase<T> constraints(Constraint<? super T>... constraints) {
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
	public static class FieldConfig<T> extends ConfigBase<T> {

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

	public static class PropertyConfig<T> extends ConfigBase<T> {

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

			if (value instanceof EnumerableValue) {
				valueEnumerator = ((EnumerableValue<T>) value)::enumerateValues;
			} else if(isEnum(propertyType)) {
				valueEnumerator = () -> list(getEnumConstants(propertyType));
			}
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

			if (value instanceof EnumerableValue) {
				valueEnumerator = ((EnumerableValue<T>) value)::enumerateValues;
			} else if(isEnum(propertyType)) {
				valueEnumerator = () -> list(getEnumConstants(propertyType));
			}
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

			if (value instanceof EnumerableValue) {
				valueEnumerator = ((EnumerableValue<T>) value)::enumerateValues;
			} else if(isEnum(propertyType)) {
				valueEnumerator = () -> list(getEnumConstants(propertyType));
			}
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

	public static class ReadOnlyPropertyConfig<T> extends ConfigBase<T> {

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

			// support enumeration by delegation if property supports is
			if (value instanceof EnumerableValue)
				valueEnumerator = ((EnumerableValue<T>) value)::enumerateValues;
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
				valueEnumerator = ((EnumerableValue<T>) value)::enumerateValues;
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

	public static class OverridablePropertyConfig<T> extends PropertyConfig<T> {
		private final boolean defaultOverride_value;

		public OverridablePropertyConfig(Class<T> property_type, String name, IsConfig c, Set<Constraint<? super T>> constraints, OrV<T> property, String category) {
			super(property_type, name, c, constraints, property, category);
			Util.setField(this, "defaultValue", property.real.getValue());
			defaultOverride_value = property.override.getValue();
		}

		public OverridablePropertyConfig(Class<T> property_type, String name, OrV<T> property) {
			this(property_type, name, name, property, "", "", EditMode.USER);
		}

		public OverridablePropertyConfig(Class<T> property_type, String name, String gui_name, OrV<T> property, String category, String info, EditMode editable) {
			super(property_type, name, gui_name, property, category, info, editable);
			Util.setField(this, "defaultValue", property.real.getValue());
			defaultOverride_value = property.override.getValue();
		}

		@SuppressWarnings("unchecked")
		public OrV<T> getProperty() {
			return (OrV) value;
		}

		public boolean getDefaultOverrideValue() {
			return defaultOverride_value;
		}

		public void setValueToDefault() {
			getProperty().override.setValue(defaultOverride_value);
			setValue(getDefaultValue());
		}

		@Override
		public void setValueAsProperty(PropVal property) {
			var s = property.getVal1();
			if (s!=null) {
				if (s.contains("overrides:true, ")) {
					getProperty().override.setValue(true);
					s = s.replace("overrides:true, ", "");
				}
				if (s.contains("overrides:false, ")) {
					getProperty().override.setValue(false);
					s = s.replace("overrides:false, ", "");
				}
			}
		}
	}

	// TODO: handle unmodifiable lists properly, including at deserialization
	public static class ListConfig<T> extends ConfigBase<ObservableList<T>> {

		public final VarList<T> a;
		public final Ƒ1<? super T,? extends Configurable<?>> toConfigurable;

		@SuppressWarnings("unchecked")
		public ListConfig(String name, IsConfig c, VarList<T> val, String category, Set<Constraint<? super T>> constraints) {
			super((Class) ObservableList.class, name, c, val.getValue(), category);
			failIf(val.list.getClass().getSimpleName().toLowerCase().contains("unmodifiable")!=(c.editable()==EditMode.NONE));
			a = val;
			if (val.nullElements==Elements.NOT_NULL) constraints(HasNonNullElements.INSTANCE);
			toConfigurable = val.toConfigurable.andApply(configurable -> {
				if (configurable instanceof Config)
					((Config) configurable).constraints(constraints);
			});
		}

		@SuppressWarnings("unchecked")
		public ListConfig(String name, String gui_name, VarList<T> val, String category, String info, EditMode editable) {
			super((Class) ObservableList.class, name, gui_name, val.getValue(), category, info, val.list.getClass().getSimpleName().toLowerCase().contains("unmodifiable") ? EditMode.NONE :  editable);
			a = val;
			toConfigurable = val.toConfigurable;
		}

		public ListConfig(String name, VarList<T> val) {
			this(name, name, val, "", "", EditMode.USER);
		}

		@Override
		public ObservableList<T> getValue() {
			return a.getValue();
		}

		@Override
		public void setValue(ObservableList<T> val) {}

		@Override
		public PropVal getValueAsProperty() {
			return new PropValN(
				map(
					getValue(),
					t -> a.toConfigurable.apply(t).getFields().stream()
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
			boolean isFixedSizeAndHasConfigurableItems = a.factory==NULL_SUPPLIER;
			AtomicInteger i = isFixedSizeAndHasConfigurableItems ? new AtomicInteger(0) : null;
			var values = property.getValN().stream()
				.map(s -> {
					T t = isFixedSizeAndHasConfigurableItems ? a.list.get(i.getAndIncrement()) : a.factory.get();
					List<Config> configs = list(a.toConfigurable.apply(t).getFields());
					List<String> cValues = split(s, ";");
					if (configs.size()==cValues.size())
						forEachBoth(configs, cValues, (c, v) -> c.setValueAsProperty(new PropVal1(v))); // TODO: support multi-value

					return (T) (a.itemType.isAssignableFrom(configs.get(0).getType()) ? configs.get(0).getValue() : t);
				})
				.filter(a.nullElements==Elements.NOT_NULL ? (it -> it!=null) : (it -> true))
				.collect(toList());
			setTo(a.list, values);
		}

	}

	public static class VarList<T> extends V<ObservableList<T>> {

		public enum Elements {
			NULLABLE, NOT_NULL
		}

		static final Object[] EMPTY_ARRAY = {};
		static final Supplier NULL_SUPPLIER = () -> null;
		private static <T> Ƒ1<T, Configurable<?>> computeDefaultToConfigurable(Class<T> itemType) {
			return Configurable.class.isAssignableFrom(itemType)
				? f -> (Configurable<?>) f
				: f -> Config.forValue(itemType, "Item", f);
		}

		public final Class<T> itemType;
		public final ObservableList<T> list;
		public final Supplier<? extends T> factory;
		private final Ƒ1<? super T,? extends Configurable<?>> toConfigurable;
		private Elements nullElements;

		public VarList(Class<T> itemType, Elements nullElements) {
			this(itemType, () -> null, computeDefaultToConfigurable(itemType));
			this.nullElements = nullElements;
		}

		public VarList(Class<T> itemType, Elements nullElements, ObservableList<T> items) {
			this(itemType, () -> null, computeDefaultToConfigurable(itemType), items);
			this.nullElements = nullElements;
		}

		public VarList(Class<T> itemType, Supplier<? extends T> factory, Ƒ1<? super T, ? extends Configurable<?>> toConfigurable, ObservableList<T> items) {
			super(items);
			this.list = items;
			this.itemType = itemType;
			this.factory = factory;
			this.toConfigurable = toConfigurable;
			this.nullElements = Elements.NOT_NULL;
		}

		@SafeVarargs
		public VarList(Class<T> itemType, Supplier<? extends T> factory, Ƒ1<? super T, ? extends Configurable<?>> toConfigurable, T... items) {
			this(itemType, factory, toConfigurable, observableArrayList(items));
		}

		// Note: What a strange situation. We must overload varargs constructor for empty case or we run into
		// runtime problems (NoSuchMethodError) even though compilation succeeds. Compiler/JVM bug?
		// What's worse we can not
		// - call this(params...) with no array, or we get compilation error: recursive constructor call
		// - call this(params..., new T[0]) with empty array, compilation error: can not instantiate T directly
		@SuppressWarnings("unchecked")
		public VarList(Class<T> itemType, Supplier<? extends T> factory, Ƒ1<T,Configurable<?>> toConfigurable) {
			this(itemType, factory, toConfigurable, (T[]) EMPTY_ARRAY);
		}

		@Deprecated
		@Override
		public void setValue(ObservableList<T> v) {
			// guarantees that the list will be permanent value since it is
			// only null before initialization. thus we no overwriting it
			if (list==null) super.setValue(v);
		}

		/**
		 * Clears list and adds items to it. Fires 1 event.
		 *
		 * @return this (fluent api)
		 */
		public VarList<T> setItems(Collection<? extends T> items) {
			list.setAll(items);
			return this;
		}

		/**
		 * Array version of {@link #setItems(java.util.Collection)}
		 *
		 * @return this (fluent api)
		 */
		@SuppressWarnings("unchecked")
		public VarList<T> setItems(T... items) {
			list.setAll(items);
			return this;
		}

		/**
		 * Adds invalidation listener to the list.
		 *
		 * @return this (fluent api)
		 */
		@SuppressWarnings("unchecked")
		public VarList<T> onListInvalid(Consumer<ObservableList<T>> listener) {
			InvalidationListener l = o -> listener.accept((ObservableList<T>) o);
			list.addListener(l);
//			return () -> list.removeListener(l);
			return this;
		}

		/**
		 * Adds list change listener to the list.
		 *
		 * @return this (fluent api)
		 */
		public VarList<T> onListChange(ListChangeListener<? super T> listener) {
			list.addListener(listener);
//			return () -> list.removeListener(listener);
			return this;
		}
	}

	public static class ConfigurableVarList<T extends Configurable> extends VarList<T> {
		@SuppressWarnings("unchecked")
		public ConfigurableVarList(Class<T> itemType, T... items) {
			super(
					itemType,
					NULL_SUPPLIER,
					configurable -> configurable,
					items
			);
		}
	}

	/**
	 * Functional implementation of {@link sp.it.util.conf.Config} that does not store nor wrap the
	 * value, instead contains the getter and setter which call the code that
	 * provides the actual value. This can be thought of some kind of intermediary.
	 * <p/>
	 * Use when wrapping the value is not desired, rather it is defined by a means
	 * of accessing it.
	 */
	public static class AccessorConfig<T> extends ConfigBase<T> implements WritableValue<T> {

		private final Consumer<T> setter;
		private final Supplier<T> getter;

		/**
		 * @param setter defines how the value will be set
		 * @param getter defines how the value will be accessed
		 */
		public AccessorConfig(Class<T> type, String name, String gui_name, Consumer<T> setter, Supplier<T> getter, String category, String info, EditMode editable) {
			super(type, name, gui_name, getter.get(), name, info, editable);
			this.getter = getter;
			this.setter = setter;
		}

		/**
		 * @param setter defines how the value will be set
		 * @param getter defines how the value will be accessed
		 */
		public AccessorConfig(Class<T> type, String name, Consumer<T> setter, Supplier<T> getter) {
			super(type, name, name, getter.get(), "", "", EditMode.USER);
			this.getter = getter;
			this.setter = setter;
		}

		/**
		 * @param setter defines how the value will be set
		 * @param getter defines how the value will be accessed
		 */
		public AccessorConfig(Class<T> type, String name, String description, Consumer<T> setter, Supplier<T> getter) {
			super(type, name, name, getter.get(), "", description, EditMode.USER);
			this.getter = getter;
			this.setter = setter;
		}

		public AccessorConfig(Class<T> type, String name, String description, String category, T objectAccessed) {
			super(type, name, name, objectAccessed, category, description, EditMode.USER);
			this.getter = () -> objectAccessed;
			this.setter = o -> {};
		}

		@Override
		public T getValue() {
			return getter.get();
		}

		@Override
		public void setValue(T val) {
			setter.accept(val);
		}

	}
}