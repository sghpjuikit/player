package sp.it.util.conf

import sp.it.util.dev.failIf
import sp.it.util.type.Util.unPrimitivize
import sp.it.util.type.isSuperclassOf
import sp.it.util.validation.Constraint
import sp.it.util.validation.Constraints
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmName

object MainConfiguration: Configuration()

fun computeConfigGroup(declaringRef: Any): String {
   val groupDiscriminant = (declaringRef as? MultiConfigurable)
      ?.configurableDiscriminant
      ?.apply { failIf(isBlank()) { "Configurable discriminant is empty" } }
      ?: ""

   return if (groupDiscriminant.isEmpty()) {
      obtainConfigGroup(null, declaringRef::class)
   } else {
      obtainConfigGroup(null, declaringRef::class, "").takeIf { it.isNotBlank() }
         ?.let { "$it.$groupDiscriminant" }
         ?: groupDiscriminant
   }
}

fun IsConfig?.computeConfigGroup(declaringRef: Any): String {
   if (this!=null && group.isNotBlank()) return group

   val groupDiscriminant = (declaringRef as? MultiConfigurable)
      ?.configurableDiscriminant
      ?.apply { failIf(isBlank()) { "Configurable discriminant is empty" } }
      ?: ""

   return if (groupDiscriminant.isEmpty()) {
      obtainConfigGroup(this, declaringRef::class)
   } else {
      obtainConfigGroup(this, declaringRef::class, "").takeIf { it.isNotBlank() }
         ?.let { "$it.$groupDiscriminant" }
         ?: groupDiscriminant
   }
}

fun obtainConfigGroup(info: IsConfig?, type: KClass<*>, or: String = type.simpleName ?: type.jvmName): String = null
   ?: info?.let { it.group.takeIf { it.isNotBlank() } }
   ?: type.findAnnotation<IsConfigurable>()?.let { it.groupPrefix.takeIf { it.isNotBlank() } }
   ?: or

fun obtainConfigGroup(info: IsConfig?, type: Class<*>): String = obtainConfigGroup(info, type.kotlin)

fun <T: Any> obtainConfigGroup(info: IsConfig?, type: Class<T>, instance: T?): String = if (instance==null) obtainConfigGroup(info, type.kotlin) else computeConfigGroup(instance)

@Suppress("UNCHECKED_CAST")
fun <T> obtainConfigConstraints(configType: Class<T>, annotations: List<Annotation>): Sequence<Constraint<T>> =
   annotations.asSequence()
      .filter { it.annotationClass.findAnnotation<Constraint.IsConstraint>()?.value?.isSuperclassOf(unPrimitivize(configType)) ?: false }
      .map { Constraints.toConstraint<T>(it) } +
      Constraints.IMPLICIT_CONSTRAINTS
         .getElementsOfSuper(configType).asSequence()
         .map { constraint -> constraint as Constraint<T> }

fun obtainConfigId(configName: String, configGroup: String) = "$configName.$configGroup".replace(' ', '_').toLowerCase()

fun obtainConfigId(config: Config<*>) = obtainConfigId(config.name, config.group)