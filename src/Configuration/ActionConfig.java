/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import Action.Action;
import static java.lang.Double.NaN;
import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 * Config implementation specifically for {@link Action}. Actions deserve special
 * treatment because they should be immutable and act as a singleton instances
 * within the application.
 * <p>
 * For that reason, this class is Immutable, the wrapped Action is final and
 * its life cycle is same as that of this object.
 * <p>
 * Basically developer should not think of an Action as a value, rather an
 * object he operates on.
 * <br><p>
 * This class has use only for generating gui settings for Actions.
 * 
 * @author Plutonium_
 */
@Immutable
public class ActionConfig extends Config<Action> {
    
    private final Action a;

    /**
     * 
     * @param c action to be wrapped. Becomes final as this config is immutable.
     */
    ActionConfig(Action c) {
        super(c.name,c.name+" Shortcut", c, "Shortcuts", c.info, true, true, NaN, NaN);
        a = c;
    }
    
    /**
     * Returns the wrapped action. Always the same instance, which is final.
     * @return 
     */
    @Override
    public Action getValue() {
       return a;
    }

    /** Changes globality and keys for the wrapped Action based on the specified
      * one. Does not change instance, which is final.
      */
    @Override
    public boolean setValue(Action val) {
        a.set(val.isGlobal(), val.getKeys());
        return true;
    }

    /** Does nothing. Equivalent to: return true; */
    @Override
    public boolean applyValue() {
        return true;
    }

    /** equivalent to: return Action.class; */
    @Override
    public Class<Action> getType() {
        return Action.class;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Action getDefaultValue() {
        return Action.defaultOf(a);
    }
    
    
    
}
