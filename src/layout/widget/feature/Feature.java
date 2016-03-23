/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package layout.widget.feature;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Provides metadata information about an interface as a feature of an object
 * implementing it. The object can then provide better information about itself
 *  (its features) by providing information about the interfaces it implements.
 * <p>
 * Simply, if implementing an interface tells compiler what an object can do,
 * annotating the interface as a Feature tells the same information to the
 * user of the application.
 *
 * @author Plutonium_
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface Feature {
    /** Identifies the feature. Not necessarily unique. */
    String name() default "";
    /** Description of the feature.. */
    String description() default "";
    /**
     * Identifies the feature exactly. Must be unique and must match the class of the annotated
     * interface.
     */
    Class type() default Void.class;
}
