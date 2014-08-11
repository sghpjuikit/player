/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.ContextMenu;

import javafx.scene.control.ContextMenu;

/**
 *
 * @author Plutonium_
 */
public class ContentContextMenu<E> extends ContextMenu implements GUI.ItemHolders.ItemHolder<E>{
    E item;
    
    @Override
    public E getItem() {
        return item;
    }
    public void setItem(E item) {
        this.item = item;
    }
}
