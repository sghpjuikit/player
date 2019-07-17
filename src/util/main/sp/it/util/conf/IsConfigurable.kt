package sp.it.util.conf

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS

/** Allows to specify default category name for all configuration fields within this configurable. */
@MustBeDocumented
@Retention(RUNTIME)
@Target(CLASS)
annotation class IsConfigurable(
   val groupPrefix: String = ""
)