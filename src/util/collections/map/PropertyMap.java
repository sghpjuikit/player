/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.collections.map;

import java.util.HashMap;

import javafx.geometry.Orientation;

/** HashMap for properties with additional utility methods. */
public final class PropertyMap extends HashMap<String, Object>{

    /**
     * Returns value of the property with specified name. If no such property is
     * found, property is created and initialized to provided value and returned.
     * Always returns back usable value.
     *
     * The value needs to be cast to desired value. The casting will always
     * succeed as only correct value can be returned back.
     *
    * @param name
     * @param default_value
     * @return always value for given property.
     */
    public Object get(String name, Object default_value) {
        putIfAbsent(name, default_value);
        return get(name);
    }

    /**
     * Returns type of value with specified key. Null If it doesn't exist.
     * @param name
     * @return
     */
    public Class<?> getType(String name) {
        if(!containsKey(name)) return null;
        return get(name).getClass();
    }

/******************************************************************************/

    /**
     * Initializes and returns the property.
     * Effectively the same as all other get() methods, but it performs a type
     * check and throws an exception if initialization value doesnt conform to the
     * specified type. Type check is not needed more than once.
     * This method is not necessity, but it conveniently points to wrong code.
     * This method guarantees that after it is invoked, the specified property
     * will exist and have correct type. It doesnt guarantee the initialized value
     * however. If the property already existed and was valid it is returned as is.
     * Therefore, this method doesnt overwrite any persisted values from previous
     * session.
     * It is recommended to use this method to initialize the properties. Especially
     * use this method in scenarios where values are serialized or persisted.
     * Not using this method and then using get() methods might result in all
     * sorts of exceptions. A way to counter this is to use get() methods that
     * specify default return value, but they pollute the code and most
     * importantly silently swallow the error and always return def value.
     * @param type doesnt allow primitive types. Use their wrapper class type instead.
     * @param name of the property
     * @param val of the property to be initialized to, primitive types supported
     * @return newly initialized property or old one with old value if already exists.
     * @throws ClassFormatError for primitive types type parameter value
     * @throws ClassCastException when parameter types dont match.
     */
    public Object initProperty(Class<?> type, String name, Object val) {
        if (type.isPrimitive())
            throw new ClassFormatError("Type of property must not be primitive. (Value of course can)");
        if (!type.equals(val.getClass()))
            throw new ClassCastException("The value doesnt match the type of property");
        return get(name, val);
    }

    public boolean getB(String key) {
        return (boolean) get(key);
    }
    public double getD(String key) {
        return (double) get(key);
    }
    public int getI(String key) {
        return (int) get(key);
    }
    public long getL(String key) {
        return (long) get(key);
    }
    public float getF(String key) {
        return (float) get(key);
    }
    public String getS(String key) {
        return (String) get(key);
    }
    public Orientation getOriet(String key) {
        return (Orientation) get(key);
    }

}
