/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Configuration;

import PseudoObjects.ReadMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javafx.geometry.Orientation;

/**
 * Storage for properties.
 * Supports serialization, therefore, even if type of object is not limited, it
 * is recommended to stick to basic values.
 * 
 * @author uranium
 */
public final class PropertyMap {
    private final Map<String, Object> map;
    
    public PropertyMap() {
        map = new HashMap<>();
    }
    
    /**
     * @return unmodifiable version of this of this property map. Consider it 
     * read only representation of this object.
     */
    public Map<String, Object> getMap() {
        return Collections.unmodifiableMap(map);
    }
    
    /**
     * Returns true if this map contains no key-value mappings - no
     * properties.
     * @return 
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }
    
/******************************************************************************/
    
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
        map.putIfAbsent(name, default_value);
        return map.get(name);
    }
    
    /**
     * Sets value of property with specified name. If it doesnt exist it is
     * created, if it does, it is rewritten.
     * @param name
     * @param value 
     */
    public void set(String name, Object value) {
        if (name == null || value == null) return;
        map.put(name, value);
    }
    
    /**
     * Returns type of value with specified property name. If it doesnt exist,
     * null is returned.
     * @param name
     * @return 
     */
    public Class<?> getType(String name) {
        if(!map.containsKey(name)) return null;
        return map.get(name).getClass();
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
        return (boolean) map.get(key);
    }    
    public double getD(String key) {
        return (double) map.get(key);
    }    
    public int getI(String key) {
        return (int) map.get(key);
    }    
    public long getL(String key) {
        return (long) map.get(key);
    }    
    public float getF(String key) {
        return (float) map.get(key);
    }  
    public String getS(String key) {
        return (String) map.get(key);
    }    
    public ReadMode getRM(String key) {
        return (ReadMode) map.get(key);
    }
    public Orientation getOr(String key) {
        return (Orientation) map.get(key);
    }  
    
}
