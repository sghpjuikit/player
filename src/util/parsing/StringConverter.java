
package util.parsing;

/**
 * String to Object bidirectional converter.
 *
 * @param <T> type of object
 * @author Plutonium_
 */
public interface StringConverter<T> extends ToStringConverter<T>, FromStringConverter<T> {

}
