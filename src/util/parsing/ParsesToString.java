package util.parsing;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Makes the method responsible for the parsing to String.
 * <p/>
 * The annotated element must be:
 * <ul>
 * <li> method
 * <li> return String
 * <ul> take either no parameter or single parameter of type the same or supertype of the class it is defined in
 * <p/>
 * The method can be static. Or instance and taking a proper argument.
 */
@Documented
@Retention(RUNTIME)
@Target({METHOD, CONSTRUCTOR})
public @interface ParsesToString {}