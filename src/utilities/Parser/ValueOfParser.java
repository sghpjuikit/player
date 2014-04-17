/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.Parser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import utilities.Log;

/**
 *
 * @author Plutonium_
 */
public class ValueOfParser implements ObjectStringParser {

    @Override
    public boolean supports(Class type) {
        try {
            Method m = type.getDeclaredMethod("valueOf", String.class);
//            if (m.getReturnType().equals(type)) throw new NoSuchMethodException();
            return true;
        } catch ( NoSuchMethodException ex) {
            return false;
        }
    }

    @Override
    public Object fromS(Class type, String source) {
            //try parsing unknown types with valueOf(String) method if available
            try {
                Method m = type.getDeclaredMethod("valueOf", String.class);
                return m.invoke(null, source);
            } catch ( NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                Log.deb("Unable to parse. Encountered problem during invoking valueOf() method");
                return null;
            }
    }

    @Override
    public String toS(Object object) {
        return object.toString();
    }
    
}
