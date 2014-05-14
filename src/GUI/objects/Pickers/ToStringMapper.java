/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.Pickers;

/**
 *
 * @author Plutonium_
 */
@FunctionalInterface
public interface ToStringMapper<T> {
    
    /**
     * Converts item to String.
     * @param item
     * @return string representation of the object according to this mapper's
     * specification.
     */
    String convert(T item);
}
