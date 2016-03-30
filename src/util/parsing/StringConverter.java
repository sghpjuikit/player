
package util.parsing;

/**
 * String to Object bidirectional converter.
 *
 * @param <T> type of object
 * @author Martin Polakovic
 */
public interface StringConverter<T> extends ToStringConverter<T>, FromStringConverter<T> {

}
