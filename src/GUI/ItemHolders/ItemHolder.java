/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.ItemHolders;

/**
 * Denotes an object wrapping a value or a different object as a held item.
 *
 * @author Plutonium_
 */
public interface ItemHolder<I> {
    
    /**
     * Returns the held object or value.
     * @return the value.
     */
    I getItem();
}
