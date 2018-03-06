package sp.it.pl.util.action;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks class containing {@link IsAction} annotated methods and enables discovery of the actions.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IsActionable {
	/**
	 * Provides an optional category name for the actions in the annotated class.
	 * Otherwise {@link Class#getSimpleName()} will be used instead.
	 */
	String value() default "";
}