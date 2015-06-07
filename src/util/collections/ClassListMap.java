/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.collections;

import java.util.List;
import java.util.function.Function;
import static util.Util.getSuperClassesInc;

/**
 *
 * @author Plutonium_
 */
public class ClassListMap<E> extends ListMap<E,Class> {

    public ClassListMap(Function<E, Class> keyMapper) {
        super(keyMapper);
    }

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
    public List<E> getElementsOfSuperV(Class key) {
        List<E> o = getElementsOf(getSuperClassesInc(key));
                o.addAll(getElementsOf(Void.class));
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
    public List<E> getElementsOfSuper(Class key) {
        List<E> o = getElementsOf(getSuperClassesInc(key));
                o.addAll(getElementsOf(Void.class));
        return o;
    }
    
}
