package util.dev;

import java.lang.annotation.*;

/**
 * Container annotation for {@link util.dev.Dependency}
 * <p/>
 * Used solely by compiler and has no practical use for developer.
 *
 * @author Martin Polakovic
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
public @interface Dependencies {
	Dependency[] value();
}