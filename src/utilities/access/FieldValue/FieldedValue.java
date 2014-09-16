/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities.access.FieldValue;

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
    
    /**
     * Returns Field-value map representation of this object.
     * @return Field-value map representation of this object.
     */
    Map<F,Object> getFields();
}
