package util.dev;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Container annotation for {@link util.dev.TODO}.
 * <p/>
 * Used solely by compiler and has no practical use for developer.
 * 
 * @author Martin Polakovic
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface TODOs {
	TODO[] value();
}