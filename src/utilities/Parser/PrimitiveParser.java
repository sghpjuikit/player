/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.Parser;

import utilities.Log;

/**
 *
 * @author Plutonium_
 */
public class PrimitiveParser implements ObjectStringParser {

    @Override
    public boolean supports(Class type) {
        return  type.isEnum() || 
                type.isPrimitive() ||
                type.equals(Integer.class) ||
                type.equals(String.class) || 
                type.equals(Boolean.class) ||
                type.equals(Double.class) ||
                type.equals(Long.class);
    }

    /**
     * 
     * @param type Must be primitive type or wrapper of primitive type or enum
     * or String.
     * @param source
     * @throws UnsupportedOperationException if class type not supported.
     */
    @Override
    public Object fromS(Class type, String source) {
        try {
            if      (type.isEnum()) return Enum.valueOf(type, source);
            else if (type.equals(int.class)) return Integer.parseInt(source);
            else if (type.equals(Integer.class)) return Integer.valueOf(source);
            else if (type.equals(String.class)) return source;
            else if (type.equals(boolean.class)) return Boolean.parseBoolean(source);
            else if (type.equals(Boolean.class)) return Boolean.valueOf(source);
            else if (type.equals(double.class)) return Double.parseDouble(source);
            else if (type.equals(Double.class)) return Double.valueOf(source);
            else if (type.equals(long.class)) return Long.parseLong(source);
            else if (type.equals(Long.class)) return Long.valueOf(source);
        } catch (NumberFormatException ex) {
            Log.err("Unable to parse. Returning 0. " + ex.getMessage());
            return 0;
        }
        throw new UnsupportedOperationException("Class type not supported");
    }

    @Override
    public String toS(Object object) {
        return object.toString();
    }
}
