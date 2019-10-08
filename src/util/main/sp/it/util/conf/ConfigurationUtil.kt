package sp.it.util.conf

import sp.it.util.dev.failIf
import sp.it.util.functional.net
import sp.it.util.type.Util.unPrimitivize
import sp.it.util.type.isSuperclassOf
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmName

object MainConfiguration: Configuration()

fun IsConfig?.computeConfigGroup(declaringRef: Any): String = this?.toDef().computeConfigGroup(declaringRef)

@Suppress("IfThenToElvis")
fun ConfigDefinition?.computeConfigGroup(declaringRef: Any): String {
   if (this!=null && group.isNotBlank()) return group

   val groupSuffix = this?.group?.takeIf { it.isNotBlank() }
   val groupPrefix = (declaringRef as? ConfigDelegator)
      ?.configurableGroupPrefix
      ?.apply { failIf(isBlank()) { "Configurable discriminant is empty" } }

   return if (groupPrefix!=null && groupSuffix!=null) {
      "$groupPrefix.$groupSuffix"
   } else if (groupSuffix!=null) {
      groupSuffix
   } else if (groupPrefix!=null) {
      groupPrefix
   } else {
      declaringRef::class.net { it.simpleName ?: it.jvmName }
   }
}

@Suppress("UNCHECKED_CAST")
fun <T> obtainConfigConstraints(configType: Class<T>, annotations: List<Annotation>): Sequence<Constraint<T>> =
   annotations.asSequence()
      .filter {
         it.annotationClass.findAnnotation<Constraint.IsConstraint>()?.value?.isSuperclassOf(unPrimitivize(configType))
            ?: false
      }
      .map { Constraints.toConstraint<T>(it) } +
      Constraints.IMPLICIT_CONSTRAINTS
         .getElementsOfSuper(configType).asSequence()
         .map { constraint -> constraint as Constraint<T> }

fun obtainConfigId(configName: String, configGroup: String) = "$configName.$configGroup".replace(' ', '_').toLowerCase()

fun obtainConfigId(config: Config<*>) = obtainConfigId(config.name, config.group)