package sp.it.util.conf

import mu.KotlinLogging
import sp.it.util.action.IsAction
import sp.it.util.dev.failIf
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import java.lang.reflect.Modifier
import kotlin.reflect.full.companionObject
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.jvm.kotlinFunction

private val logger = KotlinLogging.logger { }

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

fun obtainConfigId(configName: String, configGroup: String) = "$configName.$configGroup".replace(' ', '_').toLowerCase()

fun obtainConfigId(config: Config<*>) = obtainConfigId(config.name, config.group)

fun ConfigDelegator.collectActionsOf(o: Any) = collectActionsOf(o.javaClass, o)

fun <T: Any> ConfigDelegator.collectActionsOf(type: Class<T>, instance: T?) {
   val useStatic = instance!=null

   if (useStatic)
      type.kotlin.companionObject?.let { collectActionsOf(it::class.java.asIs(), it.asIs()) }

   type.declaredMethods.asSequence()
      .filter { Modifier.isStatic(it.modifiers) xor useStatic && it.isAnnotationPresent(IsAction::class.java) }
      .forEach { m ->
         failIf(m.parameters.isNotEmpty()) { "Action method=$m must have 0 parameters" }

         cr {
            runTry {
               m.isAccessible = true
               m(instance)
            }.ifError {
               logger.error(it) { "Failed to run action from method=${type.packageName}.${m.name}" }
            }
            Unit
         }.provideDelegate(
            this,
            m.kotlinFunction!!
         )
      }
}