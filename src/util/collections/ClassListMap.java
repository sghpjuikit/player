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
     * Returns elements mapped to this class or any of its superclasses (
     * inclusing interfaces).
     */
    public List<E> getElementsOfSuper(Class key) {
        return getElementsOf(getSuperClassesInc(key));
    }
    
}
