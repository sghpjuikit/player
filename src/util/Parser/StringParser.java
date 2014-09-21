
package util.Parser;

import java.util.Collection;
import static java.util.Collections.singletonList;
import util.Util;

/**
 * String to Object bi-directional converter.
 * <p>
 * Converter can support multiple classes.
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
    default boolean supports(Class<?> type) {
        return Util.getGenericInterface(getClass(),0,0).equals(type);
    }
    
    /**
     * @return list of all classes that are supported 
     */
    default Collection<Class> getSupportedClasses() {
        return singletonList(Util.getGenericInterface(getClass(),0,0));
    }
}
