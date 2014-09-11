/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Widgets.Features;

import java.util.ArrayList;
import java.util.List;

/**
 * Common interface for objects implementing some kind of feature or behavior.
 * Because
 * <p>
 * This interface should never be used directly, always use separate 
 * extending interface.
 * <p>
 * It is expected that the application contains many widgets and widget
 * types, some of which share some behavior. This can be discovered and
 * exploited by using common behavior interface.
 * <p>
 * However the intended use is to spawn standalone and unrelated interface 
 * hierarchies for specific type of objects and object handling.
 * <p>
 * The scope of possible behavior and application defined by extending interfaces
 * is unlimited and as such this interface can theoretically be thought of as
 * a superinterface of any other class or interface, much like {@link Object} is.
 * Asking whether object implements
 * this interface, should therefore be equivalent to asking whether it extends
 * Object, within given system and whether it belongs to the system when asked
 * from context outside of it.
 * <p>
 * To produce standalone interface hierarchies, it is recommended to use
 * extending interface to define the hierarchy's root type and follow from there.
 * 
 * @author Plutonium_
 */
public interface Feature {
    
    /**
     * Returns colon separated list of names of implemented features.
     * <p>
     * Use to print human readable list of features' names of this object.
     * <p>
     * For example: Tagging, Playlist, Playback Control
     * 
     * @return human readable name of the feature.
     */
    default String getFeatureName() {
        Class<?>[] interfaces = getClass().getInterfaces();
        List<String> out = new ArrayList();
        for(Class c : interfaces)
            if (Feature.class.isAssignableFrom(c)) {
                FeatureName fn = (FeatureName) c.getAnnotation(FeatureName.class);
                if (fn!=null) out.add(fn.value());
            }
        
        return String.join(", ", out);
    }
}
