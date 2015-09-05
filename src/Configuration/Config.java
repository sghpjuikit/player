
package Configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.WritableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.reactfx.Subscription;

import unused.Log;
import util.Util;
import util.access.ApplicableValue;
import util.access.FieldValue.EnumerableValue;
import util.access.OVal;
import util.access.TypedValue;
import util.access.Ѵ;
import util.dev.TODO;
import util.functional.Functors.Ƒ1;
import util.parsing.Parser;
import util.parsing.StringConverter;

import static Configuration.Configuration.configsOf;
import static java.util.stream.Collectors.joining;
import static javafx.collections.FXCollections.observableArrayList;
import static util.Util.isEnum;
import static util.Util.unPrimitivize;
import static util.dev.Util.noØ;
import static util.functional.Util.*;

/**
 * Object representation of a configurable value.
 * <p>
 * Config encapsulates access to a value. It allows to obtain the value or
 * change it and also provides additional information associated with it.
 * <p>
 * Useful for creating {@link Configurable} objects or exporting values from
 * objects in a standardized way.
 * <p>
 * An aggregation of configs is {@link Configurable}. Note that, technically,
 * config is a singleton configurable. Therefore config actually implements
 * it and can be used as non aggregate configurable type.
 * <p>
 * Because config is convertible from String and back it also provides convert
 * methods and implements {@link StrinParser}.
 * 
 * @param <V> type of value of this config
 * 
 * @author uranium
 */
public abstract class Config<V> implements ApplicableValue<V>, Configurable<V>, StringConverter<V>, TypedValue<V>, EnumerableValue<V> {

    /**
     * Value wrapped in this config. Always {@link Object}. Primitives are
     * wrapped automatically.
     * <p>
     * The inspection of the value's class might be used. In such case checking
     * for primitives is unnecessary.
     * <pre>
     * To check for type use:
     *     value instanceof SomeClass.class
     * or
     *     value instanceof SomeClass
     *
     * For enumerations use:
     *     value instance of Enum
     * </pre>
     */
    @Override
    public abstract V getValue();
    
    /** {@inheritDoc} */
    @Override
    public abstract void setValue(V val);

    /**
     * {@inheritDoc}
     * <p>
     * Semantically equivalent to getValue().getClass(), but null-safe and 
     * potentially better performing.
     */
    @Override
    abstract public Class<V> getType();

    /**
     * Alternative name of this config. Intended to be human readable and
     * appropriately formated. 
     * <p>
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
    abstract public boolean isEditable();

    /**
     * Minimum allowable value. Applicable only for numbers. In double.
     */
    abstract public double getMin();

    /**
     * Maximum allowable value. Applicable only for numbers. In double.
     */
    abstract public double getMax();
    
    /**
     * Use to determine whether min and max fields dont dontain illegal value.
     * If they dont, they can be used to query minimal and maximal number value.
     * Otherwise Double not a number is returned and should not be used.
     * @return true if and only if value is a number and both min and max value 
     * are specified. 
     */
    abstract public boolean isMinMax();
    
    public boolean isOverridable() {
        return false;
    }
    
/******************************* default value ********************************/

    /**
     * Get default value for this config. It is the first value this config
     * contained.
     * @return default value. Never null.
     */
    abstract public V getDefaultValue();

    public final void setDefaultValue() {
        setValue(getDefaultValue());
    }

    public final void setNapplyDefaultValue() {
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
     * Sets value converted from string.
     * Equivalent to: return setValue(fromS(str));
     * @param str
     */
    public void setValueS(String str) {
        V v = fromS(str);
        if(v!=null) setValue(v);
    }
    
    /**
     * Inherited method from {@link StringConverter}
     * Note: this config remains intact.
     * <p>66 139 221
     * {@inheritDoc}
     */
    @Override
    public String toS(V v) {
        return Parser.toS(v);
    }
    
    /**
     * Inherited method from {@link StringConverter}
     * Note: this config remains intact.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public V fromS(String str) {
        if(isTypeEnumerable()) {
            for(V v : enumerateValues()) 
                if(toS(v).equals(str)) return v;
            
            Log.warn("Can not parse '" + str + "'. No enumerable config value for: "
                    + getGuiName() + ". Using default value.");
            return getDefaultValue();
        } else {
            return Parser.fromS(getType(), str);
        }
    }
    
/*************************** configurable methods *****************************/

    Supplier<Collection<V>> valueEnumerator;
    private boolean init = false;
    
    public boolean isTypeEnumerable() {
        if(!init && valueEnumerator==null) {
            valueEnumerator = buildEnumEnumerator(getDefaultValue());
            init = true;
        }
        return valueEnumerator!=null;
    }
    
    @Override
    public Collection<V> enumerateValues() {
        if(isTypeEnumerable()) return valueEnumerator.get();
        throw new RuntimeException(getType() + " not enumerable.");
    }
    
    private static <T> Supplier<Collection<T>> buildEnumEnumerator(T v) {
        Class c = v.getClass();
        return isEnum(c) ? () -> list((T[]) Util.getEnumConstants(c)) : null;
    }
    
/*************************** configurable methods *****************************/

    /**
     * This method is inherited from Configurable and is not intended to be used
     * manually on objects of this class, rather, in situations this config
     * acts as singleton {@link Configurable}.
     * <p>
     * {@inheritDoc }
     * <p>
     * Implementation details: returns self if name equals with parameter or null
     * otherwise
     * @param name
     * @throws IllegalArgumentException if name doent equal name of this config.
     */
    @Override
    public final Config<V> getField(String name) {
        if(!name.equals(getName())) throw new IllegalArgumentException("Name mismatch");
        else return this;
    }

    /**
     * This method is inherited from Configurable and is not intended to be used
     * manually on objects of this class, rather, in situations this config
     * acts as singleton {@link Configurable}.
     * <p>
     * {@inheritDoc }
     * <p>
     * Implementation details: returns singleton list of self.
     * @return 
     */
    @Override
    public final List<Config<V>> getFields() {
        return Collections.singletonList(this);
    }
    
    
    
    
/********************************* CREATING ***********************************/
    
    /**
     * Creates config for plain object - value. The difference from 
     * {@link #forProperty(java.lang.String, java.lang.Object) } is that 
     * property is a value wrapper while value is considered immutable, thus
     * new wrapper needs to be created (and will be automatically).
     * <p>
     * If the value is not a value (its class is supported by 
     * ({@link #forProperty(java.lang.String, java.lang.Object)}) 
     * Equivalent of: {@code return forProperty(name, new Accessor(property));}
     * or is null, runtime exception is thrown.
     */
    public static <T> Config<T> forValue(String name, Object value) {
        noØ(value, "Config can not be created for null");
        if(value instanceof Config ||
           value instanceof VarList ||
           value instanceof WritableValue ||
           value instanceof ReadOnlyProperty)
            throw new RuntimeException("Value " + value + "is a property and can"
                    + "not be turned into Config as value.");
        return forProperty(name, new Ѵ<>(value));
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
     * <li> {@link ReadOnlyProperty}
     * </ul>
     * so standard javafx properties will all work. If not instance of any of
     * the above, runtime exception will be thrown.
     */
    public static <T> Config<T> forProperty(String name, Object property) {
        if(property instanceof Config)
            return (Config<T>)property;
        if(property instanceof VarList)
            return new ListConfig(name,(VarList)property);
        if(property instanceof WritableValue)
            return new PropertyConfig<>(name,(WritableValue<T>)property);
        if(property instanceof ReadOnlyProperty)
            return new ReadOnlyPropertyConfig<>(name,(ReadOnlyProperty<T>)property);
        throw new RuntimeException("Must be WritableValue or ReadOnlyValue");
    }
    
    
    public static Collection<Config> configs(Object o) {
        return configsOf(o.getClass(), o, false, true);
    }
    
/******************************* IMPLEMENTATIONS ******************************/
    
    public static abstract class ConfigBase<T> extends Config<T> {
    
        private final String gui_name;
        private final String name;
        private final String group;
        private final String info;
        private final boolean editable;
        private final double min;
        private final double max;
        private final T defaultValue;

        /**
         * 
         * @param name
         * @param gui_name
         * @param val
         * @param category
         * @param info
         * @param editable
         * @param visible
         * @param min
         * @param max 
         * 
         * @throws NullPointerException if val parameter null. The wrapped value must
         * no be null.
         */
        @TODO(note = "make static map for valueEnumerators")
        ConfigBase(String name, String gui_name, T val, String category, String info, boolean editable, double min, double max) {
            Objects.requireNonNull(val);
            this.gui_name = gui_name;
            this.name = name;
            this.defaultValue = val;
            this.group = category;
            this.info = info;
            this.editable = editable;
            this.min = min;
            this.max = max;
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
        ConfigBase(String name, IsConfig c, T val, String category) {
            this(name, c.name().isEmpty() ? name : c.name(), val, category, c.info(), c.editable(), c.min(), c.max());
        }

        /** {@inheritDoc} */
        @Override
        public final String getGuiName() {
            return gui_name;
        }

        /** {@inheritDoc} */
        @Override
        public final String getName() {
            return name;
        }

        /** {@inheritDoc} */
        @Override
        public final String getGroup() {
            return group;
        }

        /** {@inheritDoc} */
        @Override
        public final String getInfo() {
            return info;
        }

        /** {@inheritDoc} */
        @Override
        public final boolean isEditable() {
            return editable;
        }

        /** {@inheritDoc} */
        @Override
        public final double getMin() {
            return min;
        }

        /** {@inheritDoc} */
        @Override
        public final double getMax() {
            return max;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isMinMax() {
            return !(Double.compare(min, Double.NaN)==0 || Double.compare(max, Double.NaN)==0) &&
                    Number.class.isAssignableFrom(unPrimitivize(getType()));
        }
        
        /** {@inheritDoc} */
        @Override
        public T getDefaultValue() {
            return defaultValue;
        }
    }
    public static class PropertyConfig<T> extends ConfigBase<T> {

        private final WritableValue<T> value;
        private final boolean isOverridable;

        /**
         * Constructor to be used with framework
         * @param _name
         * @param c the annotation
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param category
         * @throws IllegalStateException if the property field is not final
         */
        public PropertyConfig(String _name, IsConfig c, WritableValue<T> property, String category) {
            super(_name, c, property.getValue(), category);
            value = property;
            
            // support overridable values
            isOverridable = property instanceof OVal;
            // support enumeration by delegation if property supports is
            if(value instanceof EnumerableValue)
                valueEnumerator = EnumerableValue.class.cast(value)::enumerateValues;
        }
        /**
         * @param _name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @throws IllegalStateException if the property field is not final
         */
        public PropertyConfig(String name, WritableValue<T> property) {
            this(name, name, property, "", "", true, Double.NaN, Double.NaN);
        }
         /**
         * @param _name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param info description, for tooltip for example
         * @throws IllegalStateException if the property field is not final
         */
        public PropertyConfig(String name, WritableValue<T> property, String info) {
            this(name, name, property, "", info, true, Double.NaN, Double.NaN);
        }
        /**
         * @param _name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param category category, for generating config groups
         * @param info description, for tooltip for example
         * @param editable 
         * @param min use in combination with max if value is Number
         * @param max use in combination with min if value is Number
         * @throws IllegalStateException if the property field is not final
         */
        public PropertyConfig(String name, String gui_name, WritableValue<T> property, String category, String info, boolean editable, double min, double max) {
            super(name, gui_name, property.getValue(), category, info, editable, min, max);
            value = property;
            
            // support overridable values
            isOverridable = property instanceof OVal;
            // support enumeration by delegation if property supports is
            if(value instanceof EnumerableValue)
                valueEnumerator = EnumerableValue.class.cast(value)::enumerateValues;
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

        @Override
        public Class getType() {
            return getValue().getClass();
        }

        public WritableValue<T> getProperty() {
            return value;
        }

        @Override
        public boolean isOverridable() {
            return isOverridable;
        }

        /**
         * Equals if and only if object instance of PropertyConfig and its property
         * is the same property as property of this: property==o.property;
         * @param o
         * @return 
         */
        @Override
        public boolean equals(Object o) {
            if(o==this) return true;
            return (o instanceof PropertyConfig && value==((PropertyConfig)o).value);
        }

        @Override
        public int hashCode() {
            return 43 * 7 + Objects.hashCode(this.value);
        }
    }
    public static class ReadOnlyPropertyConfig<T> extends ConfigBase<T> {

        private final ReadOnlyProperty<T> value;

        /**
         * Constructor to be used with framework
         * @param _name
         * @param c the annotation
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param category
         * @throws IllegalStateException if the property field is not final
         */
        ReadOnlyPropertyConfig(String _name, IsConfig c, ReadOnlyProperty<T> property, String category) {
            super(_name, c, property.getValue(), category);
            value = property;

            // support enumeration by delegation if property supports is
            if(value instanceof EnumerableValue)
                valueEnumerator = EnumerableValue.class.cast(value)::enumerateValues;
        }
        /**
         * @param _name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @throws IllegalStateException if the property field is not final
         */
        public ReadOnlyPropertyConfig(String name, ReadOnlyProperty<T> property) {
            this(name, name, property, "", "", Double.NaN, Double.NaN);
        }
         /**
         * @param _name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param info description, for tooltip for example
         * @throws IllegalStateException if the property field is not final
         */
        public ReadOnlyPropertyConfig(String name, ReadOnlyProperty<T> property, String info) {
            this(name, name, property, "", info, Double.NaN, Double.NaN);
        }
        /**
         * @param _name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param category category, for generating config groups
         * @param info description, for tooltip for example
         * @param editable 
         * @param min use in combination with max if value is Number
         * @param max use in combination with min if value is Number
         * @throws IllegalStateException if the property field is not final
         */
        public ReadOnlyPropertyConfig(String name, String gui_name, ReadOnlyProperty<T> property, String category, String info, double min, double max) {
            super(name, gui_name, property.getValue(), category, info, false, min, max);
            value = property;
            
            if(value instanceof EnumerableValue)
                valueEnumerator = EnumerableValue.class.cast(value)::enumerateValues;
        }

        @Override
        public T getValue() {
            return value.getValue();
        }

        @Override
        public void setValue(T val) {
        }

        @Override
        public void applyValue() {}

        @Override
        public void applyValue(T val) {}

        @Override
        public Class<T> getType() {
            return (Class) getValue().getClass();
        }

        public ReadOnlyProperty<T> getProperty() {
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
            if(o==this) return true;
            return (o instanceof ReadOnlyPropertyConfig && value==((ReadOnlyPropertyConfig)o).value);
        }

        @Override
        public int hashCode() {
            return 43 * 7 + Objects.hashCode(this.value);
        }
        
    }
    public static final class ListConfig<T> extends ConfigBase<ObservableList<T>> {
        
        public final VarList<T> a;
        
        public ListConfig(String name, IsConfig c, VarList<T> val, String category) {
            super(name, c, val.getValue(), category);
            a = val;
        }
        public ListConfig(String name, String gui_name, VarList<T> val, String category, String info, boolean editable, double min, double max) {
            super(name, gui_name, val.getValue(), category, info, editable, min, max);
            a = val;
        }
        public ListConfig(String name, VarList<T> val) {
            this(name, name, val, "", "", true, Double.NaN, Double.NaN);
        }

        @Override
        public ObservableList<T> getValue() {
            return a.getValue();
        }

        @Override
        public void setValue(ObservableList<T> val) {}

        @Override
        public Class<ObservableList<T>> getType() {
            return (Class) ObservableList.class;
        }

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
        public void setValueS(String str) {
            List<T> v = fromS(str);
            a.list.setAll(v);
        }

        @Override
        public String toS(ObservableList<T> v) {
            // we convert every item of the list to string joining with ';;' delimiter
            // we convert items by converting all their fields, joining with ';' delimiter
            return v.stream().map(t ->
                a.toConfigurable.apply(t).getFields().stream().map(Config::getValueS).collect(joining(";"))
            ).collect(joining(";;"));
        }

        @Override
        public ObservableList<T> fromS(String str) {
            ObservableList<T> l = observableArrayList();
            split(str, ";;", x->x).stream()
                .map(s -> {
                    T t = a.factory.get();
                    List<Config<Object>> configs = (List)list(a.toConfigurable.apply(t).getFields());
                    List<String> vals = split(s, ";", x->x);
                    //configs.forEach(c -> System.out.println("c-"+c)); DEBUG
                    //vals.forEach(v -> System.out.println("v-"+v));
                    if(configs.size()==vals.size())
                         // its important to apply the values too
                        forEachBoth(configs, vals, (c,v)-> c.setNapplyValue(c.fromS(v)));
                    
                    if(t.getClass().equals(configs.get(0).getType()))
                        return (T)configs.get(0).getValue();
                    else 
                    return t;
                })
                .forEach(l::add);
                    
            return l;
        }
        
        
    }
    
    public static class VarList<T> extends Ѵ<ObservableList<T>> {
        public final ObservableList<T> list;
        public final Supplier<T> factory;
        public final Ƒ1<T,Configurable<?>> toConfigurable;
        
        public VarList(Supplier<T> factory, Ƒ1<T,Configurable<?>> toConfigurable) {
            // construct the list and inject it as value (by calling setValue)
            super(observableArrayList());
            // remember the reference
            list = getValue();
            
            this.factory = factory;
            this.toConfigurable = toConfigurable;
        }
        
        /** This method does nothing.*/
        @Deprecated
        @Override
        public void setValue(ObservableList<T> v) {
             // guarantees that the list will be permanent value since it is
             // only null before initialization. thus we no overwriting it
            if(list==null) super.setValue(v);
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
        public Subscription onInvalid(Consumer<ObservableList<T>> listener) {
            InvalidationListener l = o -> listener.accept((ObservableList<T>)o);
            list.addListener(l);
            return () -> list.removeListener(l);
        }
        
        /** 
         * Adds invalidation listener to the list.
         * Returns subscription to dispose of the listening.
         */
        public Subscription onInvalidRun(Runnable listener) {
            InvalidationListener l = o -> listener.run();
            list.addListener(l);
            return () -> list.removeListener(l);
        }
        
        /** 
         * Adds list change listener to the list.
         * Returns subscription to dispose of the listening.
         */
        public Subscription onChange(ListChangeListener<? super T> listener) {
            list.addListener(listener);
            return () -> list.removeListener(listener);
        }
    }
}