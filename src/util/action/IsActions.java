package util.action;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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