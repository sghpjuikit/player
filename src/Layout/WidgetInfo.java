/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author uranium
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WidgetInfo {
    String name() default "";
    String description() default "";
    String version() default "";
    String author() default "";
    String programmer() default "";
    String attributor() default "";
    String year() default "";
    String notes() default "";
    Widget.Group group() default Widget.Group.UNKNOWN;
}