package sp.it.util.conf


import sp.it.util.functional.toUnit
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.reflect.KProperty0

/**
 * [Config] metadata defining basic attributes.
 *
 * For more information about the intention and use read [Config], [ConfigDelegator], [Conf.def].
 */
interface ConfigDefinition {
   /** Name of the config. Human readable. Default value is always "". */
   val name: String
   /** Group of the config. Hierarchical. Default value is always "". */
   val group: String
   /** Description of the config. Human readable. Default value is always "". */
   val info: String
   /** Editability of the config. Determines whether operations can be performed by user or application. Default is always true. */
   val editable: EditMode
}

/** Implementation of [ConfigDefinition] */
data class ConfigDef(
   override val name: String = "",
   override val info: String = "",
   override val group: String = "",
   override val editable: EditMode = EditMode.USER
): ConfigDefinition

/** @return new [ConfigDefinition] with the specified text appended to [ConfigDefinition.info] */
fun ConfigDefinition.appendInfo(text: String) = ConfigDef(name, info + text, group, editable)

/** Sets the specified [ConfigDefinition] to define this value. Overrides [IsConfig] and previously set [Conf.def] */
infix fun <T: Any?, C: Conf<T>> C.def(def: ConfigDefinition) = apply { this.def = def }

/** Sets the specified [ConfigDefinition] to define this value. Overrides [IsConfig] and previously set [Conf.def] */
fun <T: Any?, C: Conf<T>> C.def(
   name: String = "",
   info: String = "",
   group: String = "",
   editable: EditMode = EditMode.USER
) = apply {
   def = ConfigDef(name, info, group, editable)
}

/** Sets parent's [ConfigDefinition] to define this value. Overrides [IsConfig] and previously set [Conf.def]. Parent must have delegate property [Config]. */
fun <T: Any?, C: Conf<T>> C.defInherit(
   parent: KProperty0<*>,
   group: String = "",
   editable: EditMode = EditMode.USER
) = def(parent.getDelegateConfig().nameUi, parent.getDelegateConfig().info, group, editable)

fun <T: () -> Any?> cr(def: ConfigDefinition, action: T) = cr { action().toUnit() }.def(def)

/**
 * Represents [ConfigDefinition] through [Conf.def].
 *
 * Annotating a field has the same effect as [Conf.def].
 */
@MustBeDocumented
@Retention(RUNTIME)
@Target(FIELD, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, LOCAL_VARIABLE)
annotation class IsConfig(
   val name: String = "",
   val info: String = "",
   val group: String = "",
   val editable: EditMode = EditMode.USER,
   /** Has effect only in Java when using [toConfigurableByReflect] */
   val nullable: Boolean = false
)

/** @return [ConfigDefinition] represented by this annotation */
fun IsConfig.toDef() = ConfigDef(name, info, group, editable)

/** Editability of config. [Config.isEditable]. */
enum class EditMode {
   /** Editable by app and user. */
   USER,
   /** Editable by app, but not the user. */
   APP,
   /** Not editable. */
   NONE;

   /** @return true iff not editable */
   val isByNone: Boolean
      get() = this==NONE

   /** @return true iff editable by app */
   val isByApp: Boolean
      get() = this!=NONE

   /** @return true iff editable by user */
   val isByUser: Boolean
      get() = this==USER
}