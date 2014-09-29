/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access.FieldValue;

import java.util.List;

/**
 *
 * @author Plutonium_
 */
public interface EnumerableValue<T> {
    /**
     * Provides list of all currently available values. The list can change over
     * time.
     * 
     * @return 
     */
    public List<T> enumerateValues();
}
