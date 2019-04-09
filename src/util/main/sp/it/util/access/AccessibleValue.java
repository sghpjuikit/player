package sp.it.util.access;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import javafx.beans.value.WritableValue;

/**
 * {@link WritableValue), with added default methods. A lightweight interface
 * for value wrappers.
 * <p/>
 * The value does not have to be wrapped directly within this object, rather
 * this object is a means to access it, hence the more applicable name -
 * accessible vlaue.
 *
 * @param <V> type of accessible value
 * @see SequentialValue
 * @see Operable
 */
public interface AccessibleValue<V> extends WritableValue<V>, SequentialValue<V> {

	/** Sets value to specified. Convenience setter. */
	default void setVof(WritableValue<V> value) {
		setValue(value.getValue());
	}

	/**
	 * Equivalent to calling {@link #setValue(Object)} with {@link #next()};
	 */
	default void setNextValue() {
		setValue(next());
	}

	/**
	 * Equivalent to calling {@link #setValue(Object)} with {@link #previous()};
	 */
	default void setPreviousValue() {
		setValue(previous());
	}

	/**
	 * Equivalent to calling {@link #setValue(Object)} with {@link #next()};
	 */
	default void setCycledValue() {
		setValue(next());
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Only available for {@link SequentialValue} types for which
	 * next value is returned, {@link Boolean}
	 * which is equivalent to negation and {@link Enum} which return the next declared
	 * enum constant. Otherwise does nothing.
	 */
	@SuppressWarnings("unchecked")
	@Override
	default V next() {
		V val = getValue();
		if (val instanceof SequentialValue)
			return ((SequentialValue<V>) getValue()).next();
		else if (val instanceof Boolean)
			return (V) SequentialValue.next(Boolean.class.cast(getValue()));
		else if (val instanceof Enum)
			return (V) SequentialValue.next(Enum.class.cast(getValue()));
		else return val;
	}

	/**
	 * Only available for:
	 * <ul>
	 * <li> {@link SequentialValue} types for which previous value is returned
	 * <li> {@link Boolean} which is equivalent to negation
	 * <li> {@link Enum} which return the previous declared enum constant.
	 * </ul>
	 * Otherwise does nothing.
	 * <p/>
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	default V previous() {
		V val = getValue();
		if (val instanceof SequentialValue)
			return ((SequentialValue<V>) getValue()).previous();
		else if (val instanceof Boolean)
			return (V) SequentialValue.previous(Boolean.class.cast(getValue()));
		else if (val instanceof Enum)
			return (V) SequentialValue.previous(Enum.class.cast(getValue()));
		else return val;
	}

	default void setValueOf(UnaryOperator<V> op) {
		setValue(op.apply(getValue()));
	}

	default void setValueOf(V v2, BinaryOperator<V> op) {
		setValue(op.apply(getValue(), v2));
	}

}