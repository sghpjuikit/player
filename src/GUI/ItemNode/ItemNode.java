/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.ItemNode;

import java.util.function.Consumer;
import javafx.scene.Node;

/**
 * Graphics with a value. Mostly a gui that allows user customize the value.
 * Fires value update events.
 */
public abstract class ItemNode<T> {
    protected T value;
    /** 
     * Behavior to execute when item changes. The item change ignores
     * equality check and will fire even for same object to be set.
     */
    public Consumer<T> onItemChange;
    
    /** Returns current value. Should not be null if possible. Document. */
    public T getValue() {
        return value;
    }
    
    /** Returns the root node. Use to attach this to scene graph. */
    public abstract Node getNode();
    
    /** Sets value & fires itemChange if available. Internal use only.*/
    protected final void changeValue(T nv) {
        value = nv;
        if(onItemChange!=null) onItemChange.accept(nv);
    }
    
    /** 
     * Focuses this node's content. Depends on implementation. Usually, the
     * most important element requiring user input (i.e. text box) will receive
     * focus. If there is none, does nothing.
     */
    public void focus() {}
}
