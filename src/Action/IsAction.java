
package action;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes method that can be turned into an {@link Action}.
 * <p>
 * Such method must be static and have zero parameters. There is no restriction
 * to the access modifier and private method will work the same as public or one
 * with any other access modifier one.
 * <p>
 * Uses {@link Repeatable} and can be used multiple times for the same method.
 * Therefore multiple actions with different parameters of this annotation can
 * be derived from the method.
 * <p>
 * In order for the method to be discovered the class the method resides within
 * must itself be annotated by {@link IsActionable} which autodiscovers the
 * class in order to scan it for action candidate methods.
 * <p>
 * @author uranium
 */
@Documented
@Repeatable(IsActions.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IsAction {
    
    /**
     * Name of the action. An identifier. Should be unique within the application.
     * @return action name
     */
    String name() default "";
    
    /**
     * Description of the action. Can be used to provide information about what
     * does the action do. useful for filling in graphical user interface like
     * toolips.
     * @return description of the action
     */
    String descr() default "";
    
    /** 
     * Key combination for shortcut of the action. Default is "".
     * For example: CTRL+SHIFT+A, A, F7, 9, ALT+T
     * @return shortcut */
    String shortcut() default "";
    
    /**
     * Global action has broader activation limit. For example global shortcut
     * doesn't require application to be focused. This value denotes the global
     * attribute for the resulting action
     * Default false;
     * @return whether the action is global
     */
    boolean global() default false;
    
    /** 
     * Denotes attribute of action for its activation process.
     * @return whether this action is called once or constantly on stimulus such
     * as key press.
     * Default false;
     */
    boolean continuous() default false;
}
