/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer;

import AudioPlayer.tagging.Metadata;
import java.util.ArrayList;
import java.util.List;
import utilities.Log;
import utilities.functional.functor.BiProcedure;

/**
 *
 * @author Plutonium_
 */
public class ItemChangeEvent {
    
    private final List<ItemChangeHandler<Metadata>> handlersU = new ArrayList();
    private final List<ItemChangeHandler<Metadata>> handlersC = new ArrayList();
    
    public void addOnUpdateHandler(ItemChangeHandler<Metadata> b) {
        handlersU.add(b);
    }
    public void addOnChangeHandler(ItemChangeHandler<Metadata> b) {
        handlersC.add(b);
    }
    public void remHandler(ItemChangeHandler<Metadata> b) {
        handlersU.remove(b);
        handlersC.remove(b);
    }
    
    
    /** 
     * Fire this event artificially 
     * @param type true - item changed, false - item updated
     */
    public void fireEvent(boolean type, Metadata oldV, Metadata newV) {
        if(type) {
            Log.deb("PLAYING ITEM CHANGED");
            handlersC.forEach(l-> l.accept(oldV,newV));
            handlersU.forEach(l-> l.accept(oldV,newV));
        }
        else {
            Log.deb("PLAYING ITEM UPDATED");
            handlersU.forEach(l-> l.accept(oldV,newV));
        }
    }
    
    /**
     * Generic event handler handling item change event, where item can be any
     * type of object represented by the I parameter. When the event fires, the
     * handler is provided the old item and the new item accessed as the method's
     * parameters.
     * <p>
     * This handler is a functor taking two parameters and returning no output.
     * In java8 terms a BiConsumer (which in fact it also implements).
     * <pre> 
     * An example of use:
     * new ItemChangeHandler() {
     *     Override public accept( I oldItem, I newItem) {
     *         // inner logic
     *     }
     * };
     * </pre>
     * @param <I> An item type.
     */
    @FunctionalInterface
    public static interface ItemChangeHandler<I> extends BiProcedure<I,I> {}
    
}
