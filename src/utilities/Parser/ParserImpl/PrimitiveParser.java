/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.Parser.ParserImpl;

import java.util.Collection;
import java.util.HashSet;
import utilities.Parser.ObjectStringParser;

/**
 *
 * @author Plutonium_
 */
public class PrimitiveParser implements ObjectStringParser {
    private static final HashSet<Class> classes = new HashSet();
    
    static {
        classes.add(Byte.class);
        classes.add(Boolean.class);
        classes.add(Short.class);
        classes.add(Integer.class);
        classes.add(Long.class);
        classes.add(Float.class);
        classes.add(Double.class);
        classes.add(byte.class);
        classes.add(boolean.class);
        classes.add(short.class);
        classes.add(int.class);
        classes.add(long.class);
        classes.add(float.class);
        classes.add(double.class);
        classes.add(String.class);
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean supports(Class type) {
        return classes.contains(type) || type.isEnum();
    }
    
    /** {@inheritDoc} */
    @Override
    public Collection<Class> getSupportedClasses() {
        return classes;
    }

    /**
     * 
     * @param type Must be primitive type or wrapper of primitive type or enum
     * or String.
     * @param source
     * @throws UnsupportedOperationException if class type not supported.
     * @throws NumberFormatException parsing fails
     * @throws NumberFormatException if type Enum and parsing fails
     */
    @Override
    public Object fromS(Class type, String source) {
        if (type.equals(boolean.class)) return Boolean.parseBoolean(source);
        else if (type.equals(Boolean.class)) return Boolean.valueOf(source);
        else if (type.equals(int.class)) return Integer.parseInt(source);
        else if (type.equals(Integer.class)) return Integer.valueOf(source);
        else if (type.equals(double.class)) return Double.parseDouble(source);
        else if (type.equals(Double.class)) return Double.valueOf(source);
        else if (type.equals(long.class)) return Long.parseLong(source);
        else if (type.equals(Long.class)) return Long.valueOf(source);
        else if (type.isEnum()) return Enum.valueOf(type, source);
        else if (type.equals(byte.class)) return Byte.parseByte(source);
        else if (type.equals(Byte.class)) return Byte.valueOf(source);
        else if (type.equals(short.class)) return Short.parseShort(source);
        else if (type.equals(Short.class)) return Short.valueOf(source);
        else if (type.equals(float.class)) return Float.parseFloat(source);
        else if (type.equals(Float.class)) return Float.valueOf(source);
        else if (type.equals(String.class)) return source;
        else throw new UnsupportedOperationException("Class type not supported");
    }

    @Override
    public String toS(Object object) {
        return object.toString();
    }
}