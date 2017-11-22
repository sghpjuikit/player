package sp.it.pl.util.action;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.atteo.classindex.IndexAnnotated;

/**
 * Marks class containing {@link IsAction} annotated methods and enables discovery of the actions.
 * {@implSpec}
 * It is necessary to use this annotation, as it marks class to be processed by the annotation processor during
 * compilation. There is no runtime performance lookup overhead. See {@link IndexAnnotated}.
 */
@Documented
@IndexAnnotated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IsActionable {
	/**
	 * Provides an optional category name for the actions in the annotated class.
	 * Otherwise {@link Class#getSimpleName()} will be used instead.
	 */
	String value() default "";
}