/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout;

import javafx.util.Duration;

/**

 * Defines GUI object with alternate state and provides methods to handle
 * transition between states and also constants to unify behavior of implementing
 * objects.
 * 
 * Container class also implements this interface and it is its responsibility 
 * to pass down the call to its children if they also implement this interface.
 * 
 * This is how the implementation of passing down the call should look like:
 * 
 *   public void showInfo(boolean show) {
 *       for(Object child: children.values()) {
 *           if (child instanceof ShowableInfo) {
 *               ((ShowableInfo) child).showInfo(show);
 *           }
 *       }
 *   }
 * 
 * @author uranium
 */
public interface AltState {
    /**
     * Switch states transition duration. Its consistent for all AltState
     * implementations.
     */
    static final Duration TIME = Duration.millis(300);
    
    /**
     * Transition to alternative state.
     */
    public void show();
    /**
     * Transition to normal state.
     */
    public void hide();
    
    /** Invokes show() if true else hide(). */
    public default void setShow(boolean v) {
        if(v) show(); else hide();
    }
}
