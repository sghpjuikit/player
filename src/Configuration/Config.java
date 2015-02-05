
package Configuration;

import static java.util.Arrays.asList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import util.Parser.ParserImpl.Parser;
import util.Parser.StringParser;
import util.dev.TODO;
import util.Util;
import static util.Util.unPrimitivize;
import util.access.ApplicableValue;
import util.access.FieldValue.EnumerableValue;
import util.access.TypedValue;

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
 * @param <T> type of value of this config
 * 
 * @author uranium
 */
public abstract class Config<T> implements ApplicableValue<T>, Configurable<T>, StringParser<T>, TypedValue<T>, EnumerableValue<T> {

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
    public abstract T getValue();
    
    /** {@inheritDoc} */
    @Override
    public abstract void setValue(T val);

    /**
     * {@inheritDoc}
     * <p>
     * The value and default value can only
     * be safely cast into the this class.
     * <p>
     * Semantically equivalent to getValue().getClass() but will never fail to
     * return proper class even if the value is null and possibly performing
     * much better.
     */
    @Override
    abstract public Class<T> getType();

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
    
/******************************* default value ********************************/

    /**
     * Get default value for this config. It is the first value this config
     * contained.
     * @return default value. Never null.
     */
    abstract public T getDefaultValue();

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
    public final String getValueS() {
        return Parser.toS(getValue());
    }
    
    /**
     * Sets value converted from string.
     * Equivalent to: return setValue(fromS(str));
     * @param str
     */
    public final void setValueS(String str) {
        setValue(fromS(str));
    }

    /**
     * Inherited method from {@link StringParser}
     * <p>
     * {@inheritDoc}
     */
    @Override
    public final boolean supports(Class<?> type) {
        return getType().isAssignableFrom(type);
    }
    
    /**
     * Inherited method from {@link StringParser}
     * Note: this config remains intact.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public final String toS(T val) {
        return Parser.toS(val);
    }
    
    /**
     * Inherited method from {@link StringParser}
     * Note: this config remains intact.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public final T fromS(String str) {
        return Parser.fromS(getType(), str);
    }
    
/*************************** configurable methods *****************************/

    Supplier<Collection<T>> valueEnumerator;
    private boolean init = false;
    
    public boolean isTypeEnumerable() {
        if(!init && valueEnumerator==null) {
            valueEnumerator = buildEnumEnumerator(getDefaultValue());
            init = true;
        }
        return valueEnumerator!=null;
    }
    
    @Override
    public Collection<T> enumerateValues() {
        if(!init && valueEnumerator==null) {
            valueEnumerator = buildEnumEnumerator(getDefaultValue());
            init = true;
        }
        return valueEnumerator.get();
    }
    
    private static <T> Supplier<Collection<T>> buildEnumEnumerator( T v) {
        // handle enums
        Class c = v.getClass();
        if(c.isEnum()) {
            return () -> asList((T[]) Util.getEnumConstants(c));
        // enums with class method bodies (they are not recognized as enums)
        } else {
            Class ec = c.getEnclosingClass();
            if(ec!=null && ec.isEnum()) {
                return () -> asList((T[]) ec.getEnumConstants());
            } else
                return null;
        }
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
    public final Config<T> getField(String name) {
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
    public final List<Config<T>> getFields() {
        return Collections.singletonList(this);
    }
    
    
    
    public static abstract class ConfigBase<T> extends Config<T> {
    
        private final String gui_name;
        private final String name;
        private final String group;
        private final String info;
        private final boolean editable;
        private final double min;
        private final double max;
        private final String defaultValue;

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
            this.defaultValue = toS(val);
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
            return fromS(defaultValue);
        }
    }
}