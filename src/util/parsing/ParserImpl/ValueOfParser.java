/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.parsing.ParserImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import util.parsing.ObjectStringParser;

/**
 *
 * @author Plutonium_
 */
public class ValueOfParser implements ObjectStringParser {

    @Override
    public boolean supports(Class type) {
        // hadle enum with class bodies that dont identify as enums
        // simply fool the parser by changing the class to the enum
        // note: getDeclaringClass() does not seem to work here though
        if(type.getEnclosingClass()!=null && type.getEnclosingClass().isEnum())
            type = type.getEnclosingClass();
        
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
        // hadle enum with class bodies that dont identify as enums
        // simply fool the parser by changing the class to the enum
        // note: getDeclaringClass() does not seem to work here though
        if(type.getEnclosingClass()!=null && type.getEnclosingClass().isEnum())
            type = type.getEnclosingClass();
        
        //try parsing unknown types with valueOf(String) method if available
        try {
            Method m = type.getDeclaredMethod("valueOf", String.class);
            return m.invoke(null, source);
        } catch ( NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public String toS(Object object) {
        return object.toString();
    }

    @Override
    public List<Class> getSupportedClasses() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
