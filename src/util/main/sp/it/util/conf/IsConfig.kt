package sp.it.util.conf

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

/**
 * Annotation marking field as configuration field and turning it into
 * [Config] by adding customary information to it such as name,
 * description, editability or visibility.
 *
 * Any field can be successfully annotated with this annotation. The only absolute
 * requirement is that it must be always initialized when declared to non null
 * value.
 *
 * There are two
 * different use cases. Annotating a static field turns it into application
 * scope configuration field that can be set, applied, serialized and
 * deserialized.
 * Second use is for annotating non static instance fields and comes in
 * combination with [Configurable] interface that basically exports all
 * annotated fields of the object (or its sub-objects) as a powerful way to
 * access those fields and see the object in terms of how it can be configured.
 *
 * Furthermore, the type of the field (its Class) should have implemented
 * correct equals() and toString method. The first will be used to compare
 * equality of new and old value in the field. Incorrectly implementer equals()
 * can produce unnecessary method calls and operations potentially with heavy
 * performance impact (for example if a big application module ends up refreshing
 * because of an incorrectly captured value change). The toString() method is
 * useful for debugging and messaging and should be overridden and return human
 * readable information about the state of the object or its value.
 *
 * Based on the application of the value of the field, there are two kinds of
 * fields. Those that can be simply set to a value and those that require some
 * code to be executed to have their new value be reflected by the application.
 *
 * For more information about the intention and use read [IsConfigurable] and [Config].
 *
 * All fields are default-initialized and will never be null. Null checks are unnecessary.
 */
@MustBeDocumented
@Retention(RUNTIME)
@Target(FIELD, PROPERTY, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, LOCAL_VARIABLE)
annotation class IsConfig(
    /**
     * @return human readable name of the field. Default value is "". If not provided, the name will match the name of
     * the annotated field.
     */
    val name: String = "",
    /**
     * @return human readable description of the field. Mostly used for tooltips within graphical user interface.
     * Default is "".
     */
    val info: String = "",
    /**
     * @return category of the field belongs to. Use for aggregation of the configuration fields. Default value is "".
     * The default value will be set from category or name of the class containing the field. See [ ].
     */
    val group: String = "",
    /**
     * @return the intended editability of the configuration field by the user. Should be adhered to by the graphical
     * user interface for example by not displaying this config. Default true.
     */
    val editable: EditMode = EditMode.USER
)

enum class EditMode {
    /**
     * Editable by app and user.
     */
    USER,
    /**
     * Editable by app, but not the user.
     */
    APP,
    /**
     * Not editable.
     */
    NONE;

    /**
     * @return true iff not editable
     */
    val isByNone: Boolean
        get() = this==NONE

    /**
     * @return true iff editable by app
     */
    val isByApp: Boolean
        get() = this!=NONE

    /**
     * @return true iff editable by user
     */
    val isByUser: Boolean
        get() = this==USER
}
