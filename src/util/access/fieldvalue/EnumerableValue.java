/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access.fieldvalue;

import java.util.Collection;

/**
 * Value that has can list its possible values, which it is restricted to.
 */
public interface EnumerableValue<T> {
    
    /**
     * Provides list of all currently available values. The list can differ if
     * constructed at different time.
     */
    Collection<T> enumerateValues();
}
