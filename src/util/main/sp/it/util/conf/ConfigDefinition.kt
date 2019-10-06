package sp.it.util.conf


import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

/**
 * [Config] metadata defining basic attributes.
 *
 * For more information about the intention and use read [Config], [ConfigDelegator].
 */
interface ConfigDefinition {
   /** Name of the config. Human readable. */
   val configName: String
   /** Group of the config. Hierarchical. */
   val configGroup: String
   /** Description of the config. Human readable. */
   val configInfo: String
   /** Editability of the config. Determines whether operations can be performed by user or application. */
   val configEditable: EditMode
}

/** Implementation of [ConfigDefinition] */
data class ConfigDef(
   override val configName: String,
   override val configInfo: String,
   override val configGroup: String,
   override val configEditable: EditMode = EditMode.USER
): ConfigDefinition

/** @return new [ConfigDefinition] with the specified text appended to [ConfigDefinition.configInfo] */
fun ConfigDefinition.appendInfo(info: String) = ConfigDef(configName, configInfo + info, configGroup, configEditable)

/**
 * Represents [ConfigDefinition] through [IsConfig.toDef].
 *
 * For more information about the intention and use read [Config], [ConfigDelegator].
 */
@MustBeDocumented
@Retention(RUNTIME)
@Target(FIELD, PROPERTY, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, LOCAL_VARIABLE)
annotation class IsConfig(
   /** Default value is "". If not provided, the name will match the name of the annotated field. */
   val name: String = "",
   /** Default is "". */
   val info: String = "",
   /** Default value is "". */
   val group: String = "",
   /** Default is true. */
   val editable: EditMode = EditMode.USER
)

/** @return [ConfigDefinition] represented by this annotation  */
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