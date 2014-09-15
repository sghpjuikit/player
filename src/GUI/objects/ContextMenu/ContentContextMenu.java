/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.ContextMenu;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import utilities.access.AccessibleValue;

/**
 *
 * @author Plutonium_
 */
public class ContentContextMenu<E> extends ContextMenu implements AccessibleValue<E> {
    
    E v;
    
    public ContentContextMenu() {
        setConsumeAutoHidingEvents(false);
    }
    
    public ContentContextMenu(MenuItem ... items) {
        this();
        getItems().addAll(items);
    }
    
    @Override
    public E getValue() {
        return v;
    }
    
    @Override
    public void setValue(E val) {
        this.v = val;
    }
    
    @Override
    public void show(Node n, double screenX, double screenY) {
        super.show(n.getScene().getWindow(), screenX, screenY);
    }
    
    /**
     * Shows the context menu for node at proper coordinates derived from mouse 
     * event.
     * <p>
     * Prefer this method to show context menu. Use in MouseClick handler.
     * <p>
     * Reason:
     * When showing ContextMenu, there is a big difference between show(Window,x,y)
     * and (Node,x,y). The former will not hide the menu when next click happens
     * within the node itself! This method avoids that.
     * 
     * @param n
     * @param e 
     */
    public void show(Node n, MouseEvent e) {
        super.show(n.getScene().getWindow(), e.getScreenX(), e.getScreenY());
    }
}
