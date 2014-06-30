
package Configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation marking field as configuration field and turning it into
 * {@link Config} by adding customary information to it such as name,
 * description, editability or visibility.
 * <p>
 * Any field can be successfully annotated with this annotation. There are two
 * different use cases. Annotating a static field turns it into application
 * scope configuration field that can be set, applied, serialized and 
 * deserialized.
 * Second use is for annotating non static instance fields and comes in
 * combination with {@link Configurable} interface that basically exports all
 * annnotated fields of the object (or its sub-objects) as a powerful way to
 * access those fields and see the object in terms of how it can be configured.
 * <p>
 * Furthermore, the type of the field (its Class) should have implemented
 * correct equals() and toString method. The first will be used to compare
 * equality of new and old value in the field. Incorrectly implementer equals()
 * can produce unnecessary method calls and operations potentially with heavy
 * performance impact (for example if a big application module ends up refreshing
 * because of an incorrectly captured value change). The toString() method is
 * useful for debugging and messaging and should be overriden and return human
 * readable information about the state of the object or its value.
 * <p>
 * Based on the application of the value of the field, there are two kinds of
 * fields. Those that can be simply set to a value and those that require some
 * code to be executed to have their new value be reflected by the application.
 * <p>
 * Warning: Do not suddenly change the name of the field that is annotated. It can
 * be associated with a method through its name. Make sure the method association
 * does not break after the name change. See {@link AppliesConfig}.
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
     * @return human readable name of the field. Default value is "". If not 
     * provided, the name will match the name of the annotated field.
     */
    String name() default "";
    /** 
     * @return human readable description of the field. Mostly used for 
      * tooltips within graphical user interface. Default is "". */
    String info() default "";
    /** 
     * @return category of the field belongs to. Use for aggregation of
      * the configuration fields. Default value is "". The default value will be
      * set from category or name of the class containing the field. See 
      * {@link IsConfigurable}. */
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
