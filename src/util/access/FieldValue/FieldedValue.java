/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access.FieldValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Plutonium_
 */
public interface FieldedValue<T, F extends FieldEnum<T>> {
    
    /**
     * Returns value of this object's specified field
     * 
     * @param field
     * @return 
     */
    Object getField(F field);
    
    F getMainField();
    
    /**
     * Returns Field-value map representation of this object.
     * @return Field-value map representation of this object.
     */
    default Map<F,Object> getFields() {
        Map<F,Object> m = new HashMap();
        for(F f : getFieldConstants())
            m.put(f, getField(f));
        return m;
    }
    
    /**
     * @return all possible fields
     */
    default List<F> getFieldConstants() {
        return Arrays.asList((F[]) getMainField().getClass().getEnumConstants());
    }
}
