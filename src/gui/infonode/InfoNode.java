/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.infonode;

/**
 * Node bindable to an element and displaying information about it.
 * 
 * @author Martin Polakovic
 */
public interface InfoNode<B> {
    
    /** 
     * Starts monitoring the bindable element.
     * 
     * @implSpec this method should first call {@link #unbind()} to remove any
     * previous monitoring.
     */
    void bind(B bindable);
    
    /** Stops monitoring the bindable element. */
    void unbind();
    
    /** Sets visibility for the graphics. */
    void setVisible(boolean v);
    
    /** Binds and sets visible true. */
    default void showNbind(B b) {
        bind(b);
        setVisible(true);
    }
    
    /** Unbinds and sets visible false. */
    default void hideNunbind() {
        unbind();
        setVisible(false);
    }
}
