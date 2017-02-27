package util.action;

import java.lang.annotation.*;

/**
 * Container annotation for {@link Repeatable} {@link IsAction}.
 * <p/>
 * Used solely by compiler and has no practical use for developer.
 *
 * @author Martin Polakovic
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IsActions {
	IsAction[] value();
}