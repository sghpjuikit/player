package util.parsing;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Makes the method or constructor responsible for the parsing from String.
 * <p/>
 * The annotated element must be:
 * <ul>
 * <li> constructor or static method
 * <li> return type same or subtype of class it was defined in - basically a factory method/constructor.
 * <ul> take either no parameter or single string parameter
 */
@Documented
@Retention(RUNTIME)
@Target({METHOD, CONSTRUCTOR})
public @interface ParsesFromString {}
