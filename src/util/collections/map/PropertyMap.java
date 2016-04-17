/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.collections.map;

import java.util.HashMap;

import static util.dev.Util.noØ;

/** HashMap for properties with additional utility methods. */
public final class PropertyMap<K> extends HashMap<K, Object>{

    /**
     * Initializes and returns the property.
     * Effectively the same as all other get() methods, but it performs a type
     * check and throws an exception if initialization value does not conform to the
     * specified type. Type check is not needed more than once.
     * This method is not necessity, but it conveniently points to wrong code.
     * This method guarantees that after it is invoked, the specified property
     * will exist and have correct type. It does not guarantee the initialized value
     * however. If the property already existed and was valid it is returned as is.
     * Therefore, this method does not overwrite any persisted values from previous
     * session.
     * It is recommended to use this method to initialize the properties. Especially
     * use this method in scenarios where values are serialized or persisted.
     * Not using this method and then using get() methods might result in all
     * sorts of exceptions. A way to counter this is to use get() methods that
     * specify default return value, but they pollute the code and most
     * importantly silently swallow the error and always return def value.
     * @param type does not allow primitive types. Use their wrapper class type instead.
     * @param key of the property
     * @param val of the property to be initialized to, primitive types supported
     * @return newly initialized property or old one with old value if already exists.
     * @throws ClassFormatError for primitive types type parameter value
     * @throws ClassCastException when parameter types dont match.
     */
    public <T> T getOrPut(Class<T> type, K key, T val) {
        if (type.isPrimitive())
            throw new ClassFormatError("Type of property must not be primitive. (Value of course can)");
        if (type != val.getClass())
            throw new ClassCastException("The value does not match the type of property");

        Object v = get(key);
        if(v==null || (v!=null && v.getClass()!=type)) put(key,val);
        return (T) get(key);
    }


    /**
     * Type and null safe alternative to {@link #get(java.lang.Object)}.
     * Throws exception if null or wrong type.
     */
    public boolean getB(K key) {
        return (boolean) get(key);
    }

    /**
     * Type and null safe alternative to {@link #get(java.lang.Object)}.
     * Throws exception if null or wrong type.
     */
    public double getD(K key) {
        return (double) get(key);
    }

    /**
     * Type and null safe alternative to {@link #get(java.lang.Object)}.
     * Throws exception if null or wrong type.
     */
    public int getI(K key) {
        return (int) get(key);
    }

    /**
     * Type and null safe alternative to {@link #get(java.lang.Object)}.
     * Throws exception if null or wrong type.
     */
    public long getL(K key) {
        return (long) get(key);
    }

    /**
     * Type and null safe alternative to {@link #get(java.lang.Object)}.
     * Throws exception if null or wrong type.
     */
    public float getF(K key) {
        return (float) get(key);
    }

    /**
     * Type and null safe alternative to {@link #get(java.lang.Object)}.
     * Throws nullpointer exception if null.
     */
    public String getSorThrow(K key) {
        Object o = get(key);
        noØ(o);
        return (String) o;
    }

    /**
     * Type safe alternative to {@link #get(java.lang.Object)}.
     * Throws exception if wrong type. However nulls are valid return value.
     *
     * @return the value or null if none
     */
    public String getS(K key) {
        Object o = get(key);
        return o==null ? null : (String) o;
    }

}
