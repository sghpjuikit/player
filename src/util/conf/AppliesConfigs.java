package util.conf;

import java.lang.annotation.*;

/**
 * Container annotation for {@link Repeatable} {@link AppliesConfig}.
 * <p/>
 * Used solely by compiler and has no practical use for developer.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AppliesConfigs {
	AppliesConfig[] value();
}