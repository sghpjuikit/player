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

    @Override
    public boolean supports(Class type) {
        return type.equals(String.class);
    }

    @Override
    public String fromS(String source) {
        return source;
    }

    @Override
    public String toS(String object) {
        return object;
    }
    
}
