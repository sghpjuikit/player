/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Widgets.Features;

import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 *
 * @author Plutonium_
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface Feature {
    String name() default "";
    String description() default "";
    Class type() default Object.class;
}
