package util.dev;

import java.lang.annotation.*;

/**
 * Denotes whether operation is idempotent, i.e., invoking it multiple times has the same effect as invoking it once, or
 * in other words, any subsequent invoke has no (side) effect at all.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
	/** Default true. */
	boolean value() default true;
}