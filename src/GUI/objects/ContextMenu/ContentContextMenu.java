/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.ContextMenu;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

/**
 *
 * @author Plutonium_
 */
public class ContentContextMenu<E> extends ContextMenu implements GUI.ItemHolders.ItemHolder<E> {
    
    E item;
    
    public ContentContextMenu() {
        setConsumeAutoHidingEvents(false);
    }
    
    public ContentContextMenu(MenuItem ... items) {
        this();
        getItems().addAll(items);
    }
    
    @Override
    public E getItem() {
        return item;
    }
    public void setItem(E item) {
        this.item = item;
    }
    
    // when showing ContextMenu, there is a big difference between show(Window,x,y)
    // and (Node,x,y). The former will not hide the menu when next click happens
    // within the node itself!
    @Override
    public void show(Node anchor, double screenX, double screenY) {
        super.show(anchor.getScene().getWindow(), screenX, screenY);
    }
}
