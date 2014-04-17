
package Layout;

/**
 * Implementation of AltState interface. Provides mechanism around switching
 * states, supports locking.
 * 
 * TO USE:
 * Use in delegation pattern. Make your class implement AltState and use composition
 * - add this class to your class. Implement in() and out() methods and use non 
 * abstract methods to handle AltState interface functionality.
 * 
 * During lock, state change is not possible.
 * There are two ways to alterate between state - show()/hide() and in()/out().
 * in()/out() methods are object's own implementation of the state change and 
 * invoking them will have the same effect as show()/hide(), except that state
 * change will not be remembered and lock will be ignored.
 * 
 * It is important to understand the difference.
 * Use show()/hide() to get normal expected functionality of the state change.
 * If there is a need to be able to change state despite being in lock, or
 * change state 'silently', use in()/out().
 * 
 * Suppose a class will implement AltState and composite this object to gain
 * full functionality, then implementation of abstract in()/out() methods should
 * amount to no more or nor less, but equal to transition effect. Therefore, using
 * that transition in the specified class, or calling this object's in()/out()
 * method should be identical - which is why the locking and whatnot is bypassed.
 * 
 * @author uranium
 */
abstract public class AltStateHandler implements AltState {
    private boolean isAlt = false;
    private boolean locked = false;
    
    /**
     * Transition to alt state. Defines behavior that switches state normal
     * > alternative. Bypasses lock. Needs manual update of state: setAlt(true).
     * Use to get more fine-tuned functionality.
     */
    abstract public void in();
    /**
     * Transition to normal state. Defines behavior that switches state
     * alternative > normal. Bypasses lock. Needs manual update of state: setAlt(false).
     * Use to get more fine-tuned functionality.
     */
    abstract public void out();
    
    @Override
    public void show() {
        if (locked) return;
        in();
        isAlt = true;
    }

    @Override
    public void hide() {
        out();
        isAlt = false;
    }
    
    /**
     * @return true if in alternative state
     */
    public boolean isAlt() {
        return isAlt;
    }
    /**
     * Set state manually.
     * @param val 
     */
    public void setAlt(boolean val) {
        isAlt = val;
    }
    /**
     * Locked means immunity against state change.
     * If locked==true show() hide() methods will ha no effect;
     * @return true if is locked.
     */
    public boolean isLocked() {
        return locked;// || LayoutManager.active.get(0).isLocked();                // !!!!!!!  add gui locked and ad dContainer param
    }
    /**
     * Set immunity against state change.
     * If locked==true show() hid() methods will ha no effect;
     * @param val 
     */
    public void setLocked(boolean val) {
        locked = val;
    }
}
