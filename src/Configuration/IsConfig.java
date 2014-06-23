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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation marking field as configuration field and turning it into
 * {@link Config} by adding customary information to it such as name,
 * description, editability or visibility. The field can also be associated
 * with a method that will be automatically invoked upon a value change (only
 * within the framework). See {@link AppliesConfig}.
 * <p>
 * Any field can be successfully annotated with this annotation. There are two
 * different use cases. Annotating a static field and annotating non-static one.
 * <p>
 * Warning: Dont suddenly change the name of the field that is annotated. It can
 * be associated with a method through its name. Make sure the method association
 * does not break after the name change.
 * <p>
 * For more information about the intention and use read {@link IsConfigurable}
 * and {@link Config}.
 * <p>
 * All fields are default-initialized and will never be null. Null checks are
 * unnecessary.
 * <p>
 * @author uranium
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface IsConfig {
    /** 
     * @return human readable name of the field. Default "".
     */
    String name() default "";
    /** 
     * @return human readable description of the field. Mostly used for 
      * tooltips within graphical user interface. Default is "". */
    String info() default "";
    /** 
     * @return category of the field belongs to. Used semantical agregation of
      * the configuration fields. Default "". The default value will cause the
      * category to be set to name of the class containing the field, unless
      * specified differently. See {@link IsConfigurable}. */
    String group() default "";
    /** 
     * @return the intended editability of the configuration field by the user.
      * Should be adhered to by the graphical user interface. Set to false for
      * 'private' fields intended for testing or debugging purposes accessible
      * only for the developer. Default true.*/
    boolean editable() default true;
    /** 
     * @return the intended visibility of the configuration field. Similar to
      * {@link #editable()} with slightly different intention. For example a
      * field could be set to non-editable but visible or the opposite. Default
      * true.
      */
    boolean visible() default true;
    /** 
     * Applicable only for numbers. Returns double.
     * @return the minimum allowable value
     */
    double min() default Double.NaN;
    /** 
     * Applicable only for numbers. Returns double.
     * @return the maximum allowable value
     */
    double max() default Double.NaN;
}
