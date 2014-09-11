/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.Parser;

import static java.util.Collections.singletonList;
import java.util.List;

/**
 * String to String type of parser. For use in rare cases for generic types.
 * <p>
 * @author Plutonium_
 */
public class StringStringParser implements StringParser<String> {
    
    /** {@inheritDoc} */
    @Override
    public String fromS(String source) {
        return source;
    }
    
    /** {@inheritDoc} */
    @Override
    public String toS(String object) {
        return object;
    }
    
    /** {@inheritDoc} */
    @Override
    public List<Class> getSupportedClasses() {
        return singletonList(String.class);
    }
    
}
