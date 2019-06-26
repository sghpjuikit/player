package sp.it.pl.layout.widget.feature

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**
 * Provides metadata information about an interface as a feature of an object implementing it.
 *
 * While implementing an interface tells compiler what an object can do, annotating the interface as a Feature then
 * tells this information to the user.
 *
 * Feature is not and can not be expressed by the type system (be part of the interface's signature), because each
 * feature carries its own information and multi-inheritance would not allow this.
 */
@Retention(RUNTIME)
@Target(CLASS)
annotation class Feature(
    /** Identifies the feature. Not necessarily unique. */
    val name: String = "",
    /** Description of the feature. */
    val description: String = "",
    /** Identifies the feature exactly. Must be unique and must be the class of the annotated interface. */
    val type: KClass<*>
)