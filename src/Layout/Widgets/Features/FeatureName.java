/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Widgets.Features;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 * @author Plutonium_
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FeatureName {
    String value() default "";
}
