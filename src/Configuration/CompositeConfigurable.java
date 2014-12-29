/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Defines a {@link Configurable} object which contains another Configurable as
 * its part, for example when using Composition.
 * <p>
 * This enables the object to act as a Configurable proxy for its internal
 * Configurable.
 * <p>
 * New introduced method returns the internal sub configurable.
 * @author Plutonium_
 */
public interface CompositeConfigurable<T> extends Configurable<T> {

    /** {@inheritDoc} */
    @Override
    public default Collection<Config<T>> getFields() {
        Collection<Config<T>> l = new ArrayList(Configurable.super.getFields());
        if (getSubConfigurable()!=null)
            l.addAll(getSubConfigurable().getFields());
        return l;
    }

    /** {@inheritDoc} */
    @Override
    public default Config getField(String name) {
        Config c = Configurable.super.getField(name);
        return c == null && getSubConfigurable()!=null ? getSubConfigurable().getField(name) : c;
    }
    
    /** 
     * Returns a configurable that is a composite of this object. 
     * @return sub Configurable or null if not available
     */
    public Configurable getSubConfigurable();
}
