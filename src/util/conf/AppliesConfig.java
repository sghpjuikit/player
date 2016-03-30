
package util.conf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an applier method for a {@link IsConfig}. The method becomes
 * associated with the annotated field so setting the field
 * through this framework ({@link Configuration}) invokes this method.
 * <p/>
 * Some values are reflected in the application in some way, for example visually
 * or as functionality with a continuous effect. Changing such value might require
 * an application of it to take change.
 * The intention of this annotation is to provide an automated application of the
 * new field's value by invoking the annotated method.
 * <p/>
 * The association of the method with the field is through field's name inserted as
 * a value of this annotation.
 * <p/>
 * The annotated method must be static. The access does not play a role. Even
 * private methods will be invoked. Only one method can be associated with a
 * field at once.
 * <p/>
 * One unfortunate sideffect is the fragile dependency of the method to the
 * field. If the field name is changed the method will now be invoked. The value
 * of the annotation must be changed manually which is prone to error.
 * <p/>
 * Tip:
 * Attempts to apply new values are not always guaranteed to succeed. Certain
 * configs might require some of the application's modules to be in certain
 * state (initialized for example) and can even be inapplicable and
 * require application restart. Particularly upon application initialization
 * (during start up) the modules and code dependencies might be unavailable and
 * the invocation of
 * the applier method (that is annotated by this annotation) might not have
 * been intended to be ran at that time or during this application state. The
 * applier method must be robust enough to withstand possibly broader usage
 * this configuration framework can put it against in some situations.
 * <p/>
 * Developer should keep this in mind when annotating the method and make sure
 * its invocation will not create unexpected exceptions and that if it does the
 * problem could be because of inconsistency between expected and real
 * application state.
 *
 * @author Martin Polakovic
 */
@Documented
@Repeatable(AppliesConfigs.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AppliesConfig {

    /**
     * Associates annotated method as an applier of the field specified by this
     * String as its name. If the value is empty the annotation will be ignored.
     * Default value is empty - "".
     * <p/>
     * The value must match exactly with the name of the field annotated by
     * {@link IsConfig}, not the name value of that annotation.
     * @return
     */
    String value() default "";
}
