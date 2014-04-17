/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation marking object's field as configuration field and adding customary
 * information to field such as name, additional information, editability, etc
 *
 * All fields are default-initialzied and will never be null. Null checks are
 * unnecessary.
 * 
 * @author uranium
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface IsConfig {
    String name() default "";
    String info() default "";
    boolean editable() default true;
    boolean visible() default true;
    double min() default Double.NaN;
    double max() default Double.NaN;
}
