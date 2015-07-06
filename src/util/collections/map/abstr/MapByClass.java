/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.collections.map.abstr;

import java.util.Collection;
import java.util.List;
import static util.Util.getSuperClassesInc;

/**
 *
 * @author Plutonium_
 */
public interface MapByClass<E> {

    
    /** 
     * Multi key get returning the aggregation of results for each key.
     * @return list of all values mapped to any of the keys.
     */
    public List<E> getElementsOf(Collection<Class> keys);
    
    /** @see #getElementsOf(java.util.Collection) */
    public List<E> getElementsOf(Class... keys);
    
    
    /** 
     * Returns elements mapped to one of:
     * <ul>
     * <li>specified class
     * <li>any of specified class' superclasses up to Object.class
     * <li>any of specified class' interfaces
     * <li>Void.class
     * <ul>
     * <p>
     * Note: Void.class is useful for mapping objects based on their generic
     * type.
     */
    public default List<E> getElementsOfSuperV(Class key) {
        List<E> o = getElementsOf(getSuperClassesInc(key));
        if(!Void.class.equals(key) || void.class.equals(key)) o.addAll(getElementsOf(Void.class));
        return o;
    }
    
    /** 
     * Returns elements mapped to one of:
     * <ul>
     * <li>specified class
     * <li>any of specified class' superclasses up to Object.class
     * <li>any of specified class' interfaces
     * <ul>
     */
    public default List<E> getElementsOfSuper(Class key) {
        List<E> o = getElementsOf(getSuperClassesInc(key));
        if(!Void.class.equals(key) || void.class.equals(key)) o.addAll(getElementsOf(Void.class));
        return o;
    }
}
