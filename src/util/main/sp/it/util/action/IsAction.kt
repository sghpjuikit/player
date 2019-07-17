package sp.it.util.action

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY


/**
 * Denotes method that can be turned into an [Action].
 *
 * Such method must have zero parameters. There is no restriction
 * to the access modifier and private method will work the same as public or one
 * with any other access modifier one.
 *
 * In order for the static method to be discovered the class the method resides within
 * must itself be annotated by [sp.it.util.conf.IsConfigurable] which auto-discovers the
 * class in order to scan it for action candidate methods.
 */
@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION, PROPERTY)
annotation class IsAction(
   /**
    * Name of the action. An identifier. Should be unique within the application.
    * Default "".
    */
   val name: String = "",
   /**
    * Description of the action. Can be used to provide information about what
    * does the action do. useful for filling in graphical user interface like
    * tooltips.
    * Default "".
    */
   val desc: String = "",
   /**
    * Key combination for shortcut of the action.
    * Default "".
    * For example: "CTRL+SHIFT+A", "A", "F7", "9", "ALT+T"
    */
   val keys: String = "",
   /**
    * Global action has broader activation limit. For example global shortcut
    * doesn't require application to be focused. This value denotes the global
    * attribute for the resulting action
    * Default false.
    */
   val global: Boolean = false,
   /**
    * Denotes whether this action is called once or constantly on stimulus such as key press. Default false.
    * Default false.
    */
   val repeat: Boolean = false
)