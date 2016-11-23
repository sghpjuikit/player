package util.conf;

import java.lang.invoke.MethodHandle;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.reactfx.Subscription;

import util.access.*;
import util.access.fieldvalue.EnumerableValue;
import util.conf.IsConfig.EditMode;
import util.dev.TODO;
import util.functional.Functors.Ƒ1;
import util.functional.Try;
import util.parsing.Parser;
import util.parsing.StringConverter;
import util.type.Util;
import util.validation.Constraint;

import static java.util.Arrays.asList;
import static javafx.collections.FXCollections.observableArrayList;
import static util.conf.Config.VarList.NULL_SUPPLIER;
import static util.conf.Configuration.configsOf;
import static util.dev.Util.log;
import static util.dev.Util.noØ;
import static util.functional.Try.error;
import static util.functional.Try.ok;
import static util.functional.Util.*;
import static util.type.Util.*;

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
 * methods and implements {@link StringConverter}.
 *
 * @param <T> type of value of this config
 *
 * @author Martin Polakovic
 */
public abstract class Config<T> implements ApplicableValue<T>, Configurable<T>, StringConverter<T>, TypedValue<T>, EnumerableValue<T> {

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

	abstract public Config<T> constraints(Constraint<? super T>... constraints);

/******************************* default value ********************************/

    /**
     * Get default value for this config. It is the first value this config
     * contained.
     * @return default value. Never null.
     */
    abstract public T getDefaultValue();

    public void setDefaultValue() {
        setValue(getDefaultValue());
    }

    public void setNapplyDefaultValue() {
        setNapplyValue(getDefaultValue());
    }

/******************************** converting **********************************/

    /**
     * Converts the value to String utilizing generic {@link Parser}.
     * Use for serialization or filling out guis.
     */
    public String getValueS() {
        return toS(getValue());
    }

    /**
     * Sets value converted from string. Does nothing if conversion fails.
     * Equivalent to: {@code fromS(str).ifOk(this::setValue);}
     *
     * @param s string to parse
     */
    public void setValueS(String s) {
    	ofS(s).ifOk(this::setValue);
    }

    /**
     * This method is inherited from {@link StringConverter} for compatibility & convenience reasons.
     * Note: invoking this method produces no effects on this config instance. Consider this method static.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public String toS(T v) {
        return Parser.DEFAULT.toS(v);
    }

    /**
     * This method is inherited from {@link StringConverter} for compatibility & convenience reasons.
     * Note: invoking this method produces no effects on this config instance. Consider this method static.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public Try<T,String> ofS(String s) {
        if (isTypeEnumerable()) {
            // 1 Notice we are traversing all enumarated values to look up the one which we want to
            //   deserialize.
            //   We do this by converting each value to string and compare. This is potentially
            //   inefficient operation. It is much better to parse the string to value first and
            //   then compare obejcts. The problem is Im not sure if relying on Object equals() is
            //   very safe, this should be investigated and optimized.
            //
            // 2 OverridableConfig adds additional information as a prefix when serializing the
            //   value, then removing the prefix when deserializing. This causes the lookup not work
            //   because toS adds the prefix to the values and the string parameter of this method
            //   has it already removed. To bypass this, we rely on Parser.toS/fromS directly,
            //   rather than Config.toS/fromS. This is also dangerous. Of course we could fix this
            //   by having OverridableConfig provide its own implementation, but I dont want to
            //   spread problematic code such as this around. Not till 1 gets fixed up.
            for (T v : enumerateValues())
                if (Parser.DEFAULT.toS(v).equalsIgnoreCase(s)) return ok(v);

            log(Config.class).warn("Cant parse '{}'. No enumerable value for: {}. Using default value.", s,getGuiName());
            return error("Value does not correspond to any value of the enumeration.");
        } else {
            return Parser.DEFAULT.ofS(getType(), s);
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

    private static <T> Supplier<Collection<T>> buildEnumEnumerator(T v) {
        Class c = v==null ? Void.class : v.getClass();
        return isEnum(c) ? () -> list((T[]) Util.getEnumConstants(c)) : null;
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
     * @param name
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
     * @return
     */
    @Override
    public final List<Config<T>> getFields() {
        return list(this);
    }

/********************************* CREATING ***********************************/

    /**
     * Creates config for plain object - value. The difference from
     * {@link #forProperty(Class, String, Object)} is that
     * property is a value wrapper while value is considered immutable, thus
     * a wrapper needs to be created (and will be automatically).
     * <p/>
     * If the value is not a value (its class is supported by ({@link #forProperty(Class, String, Object)}),
     * then that method is called.
     * or is null, runtime exception is thrown.
     */
    public static <T> Config<T> forValue(Class type, String name, Object value) {
        noØ(value, "Config can not be created for null");
        if (value instanceof Config ||
           value instanceof VarList ||
           value instanceof Vo ||
           value instanceof WritableValue ||
           value instanceof ObservableValue)
            throw new RuntimeException("Value " + value + "is a property and can"
                    + "not be turned into Config as value.");
        return forProperty(type, name, new V<>(value));
    }

    /**
     * Creates config for property. Te property will become the underlying data
     * of the config and thus reflect any value changes and vice versa. If
     * the property is read only, config will also be read only (its set()
     * methods will not do anything). If the property already is config, it is
     * returned.
     *
     * @param name of of the config, will be used as gui name
     * @param property underlying property for the config.
     * The property must be instance of any of:
     * <ul>
     * <li> {@link Config}
     * <li> {@link VarList}
     * <li> {@link WritableValue}
     * <li> {@link ObservableValue}
     * </ul>
     * so standard javafx properties will all work. If not instance of any of
     * the above, runtime exception will be thrown.
     */
    @SuppressWarnings("unchecked")
    public static <T> Config<T> forProperty(Class<T> type, String name, Object property) {
        if (property instanceof Config)
            return (Config<T>)property;
        if (property instanceof VarList)
            return new ListConfig(name,(VarList)property);
        if (property instanceof Vo)
            return new OverridablePropertyConfig<>(type,name,(Vo<T>)property);
        if (property instanceof WritableValue)
            return new PropertyConfig<>(type,name,(WritableValue<T>)property);
        if (property instanceof ObservableValue)
            return new ReadOnlyPropertyConfig<>(type,name,(ObservableValue<T>)property);
        throw new RuntimeException("Must be WritableValue or ReadOnlyValue, but is " + property.getClass());
    }

	@SuppressWarnings("unchecked")
    public static Collection<Config<?>> configs(Object o) {
        return (Collection) configsOf(o.getClass(), o, false, true);
    }

/******************************* IMPLEMENTATIONS ******************************/

    public static abstract class ConfigBase<T> extends Config<T> {

        private final Class<T> type;
        private final String gui_name;
        private final String name;
        private final String group;
        private final String info;
        private final EditMode editable;
        @util.dev.Dependency("DO NOT RENAME - accessed using reflection")
        private final T defaultValue;
		Set<Constraint<? super T>> constraints;

        /**
         *
         * @throws NullPointerException if val parameter null. The wrapped value must
         * no be null.
         */
        @TODO(note = "make static map for valueEnumerators")
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
         *
         * @param name
         * @param c
         * @param val
         * @param category
         *
         * @throws NullPointerException if val parameter null. The wrapped value must
         * no be null.
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
        MethodHandle applier = null;

        /**
         * @param name
         * @param c
         * @param category
         * @param instance owner of the field or null if static
         */
        @SuppressWarnings("unchecked")
        FieldConfig(String name, IsConfig c, Set<Constraint<? super T>> constraints, Object instance, String category, MethodHandle getter, MethodHandle setter) {
            super((Class)getter.type().returnType(), name, c, constraints, getValueFromFieldMethodHandle(getter, instance), category);
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
                else setter.invokeWithArguments(instance,val);
            } catch (Throwable e) {
                throw new RuntimeException("Error setting config field " + getName(),e);
            }
        }

        @Override
        public void applyValue(T val) {
            if (applier != null) {
                try {
                    int i = applier.type().parameterCount();

                    if (i==1) applier.invokeWithArguments(val);
                    else applier.invoke();
                } catch (Throwable e) {
                    throw new RuntimeException("Error applying config field " + getName(),e);
                }
            }
        }

        /**
         * Equals if and only if non null, is Config type and source field is equal.
         */
        @Override
        public boolean equals(Object o) {
            if (this==o) return true;

            if (o == null || !(o instanceof FieldConfig)) return false;

            FieldConfig c = (FieldConfig)o;
            return setter.equals(c.setter) && getter.equals(c.getter) &&
                   applier.equals(c.applier);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 23 * hash + Objects.hashCode(this.applier);
            hash = 23 * hash + Objects.hashCode(this.getter);
            hash = 23 * hash + Objects.hashCode(this.setter);
            return hash;
        }

    }
    public static class PropertyConfig<T> extends ConfigBase<T> {

        protected final WritableValue<T> value;

        /**
         * Constructor to be used with framework
         * @param name
         * @param c the annotation
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param category
         * @throws IllegalStateException if the property field is not final
         */
        public PropertyConfig(Class<T> property_type, String name, IsConfig c, Set<Constraint<? super T>> constraints, WritableValue<T> property, String category) {
            super(property_type, name, c, constraints, property.getValue(), category);
            value = property;

            // support enumeration by delegation if property supports is
            if (value instanceof EnumerableValue)
                valueEnumerator = EnumerableValue.class.cast(value)::enumerateValues;
        }

        /**
         * @param name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param category category, for generating config groups
         * @param info description, for tooltip for example
         * @param editable
         * @throws IllegalStateException if the property field is not final
         */
        public PropertyConfig(Class<T> property_type, String name, String gui_name, WritableValue<T> property, String category, String info, EditMode editable) {
            super(property_type, name, gui_name, property.getValue(), category, info, editable);
            value = property;

            // support enumeration by delegation if property supports is
            if (value instanceof EnumerableValue)
                valueEnumerator = EnumerableValue.class.cast(value)::enumerateValues;
        }

        /**
         * @param name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @throws IllegalStateException if the property field is not final
         */
        public PropertyConfig(Class<T> property_type, String name, WritableValue<T> property) {
            this(property_type, name, name, property, "", "", EditMode.USER);
        }

        /**
         * @param name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param info description, for tooltip for example
         * @throws IllegalStateException if the property field is not final
         */
        public PropertyConfig(Class<T> property_type, String name, WritableValue<T> property, String info) {
            this(property_type, name, name, property, "", info, EditMode.USER);
        }

        @Override
        public T getValue() {
            return value.getValue();
        }

        @Override
        public void setValue(T val) {
            value.setValue(val);
        }

        @Override
        public void applyValue() {
            if (value instanceof ApplicableValue)
                ApplicableValue.class.cast(value).applyValue();
        }

        @Override
        public void applyValue(T val) {
            if (value instanceof ApplicableValue)
                ApplicableValue.class.cast(value).applyValue(val);
        }

        public WritableValue<T> getProperty() {
            return value;
        }

        /**
         * Equals if and only if object instance of PropertyConfig and its property
         * is the same property as property of this: property==o.property;
         * @param o
         * @return
         */
        @Override
        public boolean equals(Object o) {
            if (o==this) return true;
            return (o instanceof PropertyConfig && value==((PropertyConfig)o).value);
        }

        @Override
        public int hashCode() {
            return 43 * 7 + Objects.hashCode(this.value);
        }

    }
    public static class ReadOnlyPropertyConfig<T> extends ConfigBase<T> {

        private final ObservableValue<T> value;

        /**
         * Constructor to be used with framework
         * @param name
         * @param c the annotation
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param category
         * @throws IllegalStateException if the property field is not final
         */
        ReadOnlyPropertyConfig(Class<T> property_type, String name, IsConfig c, Set<Constraint<? super T>> constraints, ObservableValue<T> property, String category) {
            super(property_type, name, c, constraints, property.getValue(), category);
            value = property;

            // support enumeration by delegation if property supports is
            if (value instanceof EnumerableValue)
                valueEnumerator = EnumerableValue.class.cast(value)::enumerateValues;
        }

        /**
         * @param name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param category category, for generating config groups
         * @param info description, for tooltip for example
         * @throws IllegalStateException if the property field is not final
         */
        public ReadOnlyPropertyConfig(Class<T> property_type, String name, String gui_name, ObservableValue<T> property, String category, String info) {
            super(property_type, name, gui_name, property.getValue(), category, info, EditMode.NONE);
            value = property;

            if (value instanceof EnumerableValue)
                valueEnumerator = EnumerableValue.class.cast(value)::enumerateValues;
        }

        /**
         * @param name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @throws IllegalStateException if the property field is not final
         */
        public ReadOnlyPropertyConfig(Class<T> property_type, String name, ObservableValue<T> property) {
            this(property_type, name, name, property, "", "");
        }

        /**
         * @param name
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

        @Override
        public void applyValue() {}

        @Override
        public void applyValue(T val) {}

        public ObservableValue<T> getProperty() {
            return value;
        }

        /**
         * Equals if and only if object instance of PropertyConfig and its property
         * is the same property as property of this: property==o.property;
         * @param o
         * @return
         */
        @Override
        public boolean equals(Object o) {
            if (o==this) return true;
            return (o instanceof ReadOnlyPropertyConfig && value==((ReadOnlyPropertyConfig)o).value);
        }

        @Override
        public int hashCode() {
            return 43 * 7 + Objects.hashCode(this.value);
        }

    }
    public static class OverridablePropertyConfig<T> extends PropertyConfig<T> {
        private final boolean defaultOverride_value;

        public OverridablePropertyConfig(Class<T> property_type, String name, IsConfig c, Set<Constraint<? super T>> constraints, Vo<T> property, String category) {
            super(property_type, name, c, constraints, property, category);
            Util.setField(this, "defaultValue", property.real.getValue());
            defaultOverride_value = property.override.getValue();
        }

        public OverridablePropertyConfig(Class<T> property_type, String name, Vo<T> property) {
            this(property_type, name, name, property, "", "", EditMode.USER);
        }

        public OverridablePropertyConfig(Class<T> property_type, String name, Vo<T> property, String info) {
            this(property_type, name, name, property, "", info, EditMode.USER);
        }

        public OverridablePropertyConfig(Class<T> property_type, String name, String gui_name, Vo<T> property, String category, String info, EditMode editable) {
            super(property_type, name, gui_name, property, category, info, editable);
            Util.setField(this, "defaultValue", property.real.getValue());
            defaultOverride_value = property.override.getValue();
        }

        public Vo<T> getProperty() {
            return (Vo)value;
        }

        public boolean getDefaultOverrideValue() {
            return defaultOverride_value;
        }

        public void setDefaultValue() {
            getProperty().override.setValue(defaultOverride_value);
            setValue(getDefaultValue());
        }

        public void setNapplyDefaultValue() {
            setDefaultValue();
        }

        ///**
        // * Converts the value to String utilizing generic {@link Parser}.
        // * Use for serialization or filling out guis.
        // */
        //@Override
        //public String getValueS() {
        //    String prefix = value instanceof Ѵo ? "overrides:"+((Ѵo)value).override.getValue()+", " : "";
        //    return prefix + super.toS(getValue());
        //}

        /**
         * Sets value converted from string.
         * Equivalent to: return setValue(fromS(str));
         * @param s
         */
        @Override
        public void setValueS(String s) {
	        ofS(s).ifOk(getProperty().real::setValue);
        }

        /**
         * Inherited method from {@link StringConverter}
         * Note: this config remains intact.
         * <p/>
         * {@inheritDoc}
         */
        @Override
        public String toS(T v) {
            return "overrides:"+((Vo)value).override.getValue() + ", " + ((Vo)value).real.getValue();
        }

        /**
         * Inherited method from {@link StringConverter}
         * Note: this config remains intact.
         * <p/>
         * {@inheritDoc}
         */
        @Override
        public Try<T,String> ofS(String str) {
            String s = str;
            if (s.contains("overrides:true, ")) {
                getProperty().override.setValue(true);
                s = s.replace("overrides:true, ", "");
            }
            if (s.contains("overrides:false, ")) {
                getProperty().override.setValue(false);
                s = s.replace("overrides:false, ", "");
            }
            return super.ofS(s);
        }

    }
    public static final class ListConfig<T> extends ConfigBase<ObservableList<T>> {

        public final VarList<T> a;

        @SuppressWarnings("ubnchecked")
        public ListConfig(String name, IsConfig c, VarList<T> val, String category) {
            super((Class)ObservableList.class, name, c, val.getValue(), category);
            a = val;
        }

        @SuppressWarnings("ubnchecked")
        public ListConfig(String name, String gui_name, VarList<T> val, String category, String info, EditMode editable) {
            super((Class)ObservableList.class, name, gui_name, val.getValue(), category, info, editable);
            a = val;
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
        public void applyValue(ObservableList<T> val) {}

        @Override
        public ObservableList<T> next() {
            return getValue();
        }

        @Override
        public ObservableList<T> previous() {
            return getValue();
        }

        @Override
        public ObservableList<T> cycle() {
            return getValue();
        }

        //************************* string converting

        @Override
        public String getValueS() {
            return toS(getValue());
        }

        @Override
        public void setValueS(String s) {
	        ofS(s).ifOk(a.list::setAll);
        }

        @Override
        public String toS(ObservableList<T> v) {
            // we convert every item of the list to string joining with ';;' delimiter
            // we convert items by converting all their fields, joining with ';' delimiter
            return stream(v)
                   .map(t -> stream(a.toConfigurable.apply(t).getFields()).map(Config::getValueS).joining(";"))
                   .joining(";;");
        }

        @Override
        public Try<ObservableList<T>,String> ofS(String str) {
            ObservableList<T> l = observableArrayList();
	        boolean isFixedSizeAndHasConfigurableItems = a.factory==NULL_SUPPLIER;
	        AtomicInteger i = isFixedSizeAndHasConfigurableItems ? new AtomicInteger(0) : null;
            split(str, ";;", x->x).stream()
                .map(s -> {
                    T t = isFixedSizeAndHasConfigurableItems ? a.list.get(i.getAndIncrement()) : a.factory.get();
                    List<Config> configs = list(a.toConfigurable.apply(t).getFields());
                    List<String> values = split(s, ";");
                    if (configs.size()==values.size())
                         // its important to apply the values too
                        forEachBoth(configs, values, (c,v)-> c.setNapplyValue(c.ofS(v).getOr(null))); // TODO: wtf

                    return (T) (a.itemType.isAssignableFrom(configs.get(0).getType()) ? configs.get(0).getValue() : t);
                })
                .forEach(l::add);

            return ok(l);
        }
    }

    public static class VarList<T> extends V<ObservableList<T>> {
    	static final Object[] EMPTY_ARRAY = {};
        static final Supplier NULL_SUPPLIER = () -> null;

    	public final Class<T> itemType;
        public final ObservableList<T> list;
        public final Supplier<? extends T> factory;
        public final Ƒ1<? super T, ? extends Configurable<?>> toConfigurable;

	    // Note: What a strange situation. We must overload varargs constructor for empty case or we run into
	    // runtime problems (NoSuchMethodError) even though compilation succeeds. Compiler/JVM bug?
	    // What's worse we can not
	    // - call this(params...) with no array, or we get compilation error: recursive constructor call
	    // - call this(params..., new T[0]) with empty array, compilation error: can not instantiate T directly
	    public VarList(Class<T> itemType, Supplier<? extends T> factory, Ƒ1<T,Configurable<?>> toConfigurable) {
		    this(itemType, factory, toConfigurable, (T[])EMPTY_ARRAY);
	    }

	    public VarList(Class<T> itemType, Supplier<? extends T> factory, Ƒ1<T,Configurable<?>> toConfigurable, T...items) {
		    // construct the list and inject it as value (by calling setValue)
		    super(observableArrayList(items));
		    // remember the reference
		    list = getValue();

		    this.itemType = itemType;
		    this.factory = factory;
		    this.toConfigurable = toConfigurable;
	    }

        /** This method does nothing.*/
        @Deprecated
        @Override
        public void setValue(ObservableList<T> v) {
             // guarantees that the list will be permanent value since it is
             // only null before initialization. thus we no overwriting it
            if (list==null) super.setValue(v);
        }

        /**
         * Clears list and adds items to it. Fires 1 event.
         * Fluent API - returns this object. This is to avoid multiple constructors.
         */
        public VarList<T> setItems(Collection<? extends T> items) {
            list.setAll(items);
            return this;
        }
        /** Array version of {@link #setItems(java.util.Collection)}*/
        public VarList<T> setItems(T... items) {
            list.setAll(items);
            return this;
        }

        /**
         * Adds invalidation listener to the list.
         * Returns subscription to dispose of the listening.
         */
        public Subscription onListInvalid(Consumer<ObservableList<T>> listener) {
            InvalidationListener l = o -> listener.accept((ObservableList<T>)o);
            list.addListener(l);
            return () -> list.removeListener(l);
        }

        /**
         * Adds list change listener to the list.
         * Returns subscription to dispose of the listening.
         */
        public Subscription onListChange(ListChangeListener<? super T> listener) {
            list.addListener(listener);
            return () -> list.removeListener(listener);
        }
    }
    public static class ConfigurableVarList<T extends Configurable> extends VarList<T> {
	    public ConfigurableVarList(Class<T> itemType, T...items) {
		    super(
		    	itemType,
			    NULL_SUPPLIER,
			    configurable -> configurable,
			    items
		    );
	    }
    }

	/**
	 * Functional implementation of {@link util.conf.Config} that does not store nor wrap the
	 * value, instead contains the getter and setter which call the code that
	 * provides the actual value. This can be thought of some kind of intermediary.
	 * See {@link util.access.FunctAccessor} which this config implements.
	 * <p/>
	 * Use when wrapping the value is not desired, rather it is defined by a means
	 * of accessing it.
	 *
	 * @author Martin Polakovic
	 */
	public static class AccessorConfig<T> extends ConfigBase<T> implements FunctAccessibleValue<T> {

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

	    @Override
	    public Consumer<T> getSetter() {
	        return setter;
	    }

	    @Override
	    public Supplier<T> getGetter() {
	        return getter;
	    }

	    @Override
	    public T getValue() {
	        return getter.get();
	    }

	    @Override
	    public void setValue(T val) {
	        setter.accept(val);
	    }

	    @Override
	    public void applyValue(T val) {
	        // do nothing
	    }

	}
}