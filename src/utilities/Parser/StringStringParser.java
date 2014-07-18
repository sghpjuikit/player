/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.Parser;

/**
 * String to String type of parser. For use in rare cases for generic types.
 * <p>
 * @author Plutonium_
 */
public class StringStringParser implements StringParser<String> {

    /**
     * @param type
     * @return true if and only if the class is String
     */
    @Override
    public boolean supports(Class<?> type) {
        return String.class.isAssignableFrom(type);
    }
    
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
    
}
