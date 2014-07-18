
package utilities.Parser;

/**
 * String to Object bi-directional converter.
 * 
 * @param <T> type of object
 * @author Plutonium_
 */
public interface StringParser<T> extends ToStringConverter<T>, FromStringConverter<T> {
    
    /**
     * Parser supports type only if it can parse both String into the type and the
     * inverse operation.
     * <p>
     * This method should only be used for parsers that convert multiple unrelated
     * classes and cant rely on generic parameter alone.
     * 
     * @return true if the class type is supported by this parser. False otherwise.
     */
    boolean supports(Class<?> type);
}
