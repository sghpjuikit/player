
package utilities.Parser;

/**
 *
 * @author Plutonium_
 */
interface SingleStringParser<T> extends StringParser {
    
    /**
     * Parser supports type only if it can parse both String into the type and the
     * inverse operation.
     * @return true if the class type is supported by this parser. False otherwise.
     */
    @Override
    boolean supports(Class type);
    
    /** Converts String into object.
     * @return object parsed from String.
     */
    T fromS(String source);
    
    /** Converts object into String.
     * @return String the object has been converted.
     */
    String toS(T object);
}
