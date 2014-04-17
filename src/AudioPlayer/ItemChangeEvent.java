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
import utilities.functional.functor.UnProcedure;

/**
 *
 * @author Plutonium_
 */
class ItemChangeEvent {
    
    private final List<UnProcedure<Metadata>> handlersU = new ArrayList();
    private final List<UnProcedure<Metadata>> handlersC = new ArrayList();
    
    public void addOnUpdateHandler(UnProcedure<Metadata> b) {
        handlersU.add(b);
    }
    public void addOnChangeHandler(UnProcedure<Metadata> b) {
        handlersC.add(b);
    }
    public void remHandler(UnProcedure<Metadata> b) {
        handlersU.remove(b);
        handlersC.remove(b);
    }
    
    
    /** 
     * Fire this event artificially 
     * @param type true - item changed, false - item updated
     */
    public void fireEvent(boolean type, Metadata newV) {
        if(type) {
            Log.deb("PLAYING ITEM CHANGED");
            handlersC.forEach(l-> l.accept(newV));
            handlersU.forEach(l-> l.accept(newV));
        }
        else {
            Log.deb("PLAYING ITEM UPDATED");
            handlersU.forEach(l-> l.accept(newV));
        }
    }
    
}
