package sp.it.util.conf

import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.reflect.Modifier
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.valueParameters
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.jvm.kotlinFunction
import sp.it.util.access.OrV
import sp.it.util.action.IsAction
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
import sp.it.util.functional.asIs
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.type.VType
import sp.it.util.type.argOf
import sp.it.util.type.findAnnotationAny
import sp.it.util.type.isPlatformType
import sp.it.util.type.isSubclassOf
import sp.it.util.type.raw
import sp.it.util.type.typeOrNothing

private val logger = KotlinLogging.logger { }

object MainConfiguration: Configuration()

fun IsConfig?.computeConfigGroup(declaringRef: Any): String = this?.toDef().computeConfigGroup(declaringRef)

fun ConfigDefinition?.computeConfigGroup(declaringRef: Any): String {
   if (this!=null && group.isNotBlank()) return group

   val groupSuffix = this?.group?.takeIf { it.isNotBlank() }
   val groupPrefix = (declaringRef as? ConfigDelegator)
      ?.configurableGroupPrefix
      ?.apply { failIf(isBlank()) { "Configurable discriminant is empty" } }

   return listOfNotNull(groupPrefix, groupSuffix).joinToString(".")
}

fun obtainConfigId(configName: String, configGroup: String) = "$configName.$configGroup".replace(' ', '_').lowercase()

fun obtainConfigId(config: Config<*>) = obtainConfigId(config.name, config.group)

fun ConfigDelegator.collectActionsOf(o: Any) = collectActionsOf(o::class.asIs(), o)

fun <T: Any> ConfigDelegator.collectActionsOf(type: KClass<T>, instance: T?) {
   val useStatic = instance!=null

   if (useStatic)
      type.companionObject?.let { collectActionsOf(it::class.asIs(), it.asIs()) }

   type.java.declaredMethods.asSequence()
      .filter { Modifier.isStatic(it.modifiers) xor useStatic && it.isAnnotationPresent(IsAction::class.java) }
      .forEach { m ->
         val f = m.kotlinFunction ?: fail { "Action method=$m can not be reflected by Kotlin" }
         failIf(f.valueParameters.any { !it.isOptional }) { "Action method=$m must have 0 parameters" }

         cr {
            runTry {
               m.isAccessible = true
               f.callBy(f.instanceParameter?.net { mapOf(it to instance) } ?: mapOf())
            }.ifError {
               logger.error(it) { "Failed to run action from method=${m.declaringClass.packageName}.${m.name}" }
            }
            Unit
         }.provideDelegate(
            if (instance is ConfigDelegator) instance else this,
            f
         )
      }
}

/** @return configs created by reflection from the specified instance's class's [KProperty]s annotated with [IsConfig] or throws on programmatic error */
fun annotatedConfigs(instance: Any): List<Config<*>> = instance::class.memberProperties.mapNotNull { annotatedConfig(it, instance) }

/** @return configs created by reflection from the specified instance's class's [KProperty]s annotated with [IsConfig] or throws on programmatic error */
fun annotatedConfigs(fieldNamePrefix: String, group: String, instance: Any): List<Config<*>> = instance::class.memberProperties.mapNotNull { annotatedConfig(fieldNamePrefix, group, it, instance) }

/** @return configs created by reflection from the specified [KProperty] annotated with [IsConfig] or null if not annotated or throws on programmatic error */
fun annotatedConfig(p: KProperty<*>, instance: Any): Config<*>? = p.findAnnotationAny<IsConfig>()?.let {
   annotatedConfig(p, instance, p.name, it, it.computeConfigGroup(instance))
}

private fun annotatedConfig(fieldNamePrefix: String, group: String, p: KProperty<*>, instance: Any): Config<*>? = p.findAnnotationAny<IsConfig>()?.let {
   annotatedConfig(p, instance, fieldNamePrefix + p.name, it, group)
}

@Suppress("UNCHECKED_CAST")
private fun <T: Any> annotatedConfig(p: KProperty<*>, instance: T, name: String, def: IsConfig, group: String): Config<T> {
   return runTry {
      p.isAccessible = true
      val isFinal = p !is KMutableProperty
      val type = p.returnType
      val typeRaw = p.returnType.raw
      fun KType.resolveNullability(): KType = if (isPlatformType) withNullability(def.nullable) else this
      when {
         typeRaw.isSubclassOf<Config<*>>() -> {
            failIf(!isFinal) { "Property must be immutable" }

            val config = p.getter.call(instance) as Config<T>
            failIf(def.nullable!=config.type.isNullable) { "Config nullability must match @${IsConfig::class.jvmName} nullability" }

            config
         }
         typeRaw.isSubclassOf<OrV<*>>() -> {
            failIf(!isFinal) { "Property must be immutable" }

            val property = p.getter.call(instance) as OrV<T>
            val propertyType = type.argOf(OrV::class, 0).typeOrNothing.resolveNullability()
            OrPropertyConfig(VType(propertyType), name, def.toDef(), setOf(), setOf(), property, group).asIs()
         }
         typeRaw.isSubclassOf<WritableValue<*>>() -> {
            failIf(!isFinal) { "Property must be immutable" }

            val property = p.getter.call(instance) as WritableValue<T>
            val propertyType = type.argOf(WritableValue::class, 0).typeOrNothing.resolveNullability()
            PropertyConfig(VType(propertyType), name, def.toDef(), setOf(), property, property.value, group).asIs()
         }
         typeRaw.isSubclassOf<ObservableValue<*>>() -> {
            failIf(!isFinal) { "Property must be immutable" }
            failIf(def.editable!==EditMode.NONE) { "Property mutability requires usage of ${EditMode.NONE}" }

            val property = p.getter.call(instance) as ObservableValue<T>
            val propertyType = type.argOf(ObservableValue::class, 0).typeOrNothing.resolveNullability()
            PropertyConfigRO(VType(propertyType), name, def.toDef(), setOf(), property, group).asIs()
         }
         typeRaw.isSubclassOf<ConfList<*>>() -> {
            failIf(!isFinal) { "Property must be immutable" }
            failIf(def.nullable) { "Config nullability must be false" }

            val list = p.getter.call(instance) as ConfList<Any?>
            ListConfig(name, def.toDef(), list, group, setOf(), setOf()).asIs()
         }
         typeRaw.isSubclassOf<CheckList<*, *>>() -> {
            failIf(!isFinal) { "Property must be immutable" }

            val list = p.getter.call(instance) as CheckList<*, *>
            CheckListConfig(name, def.toDef(), list, group, setOf()).asIs()
         }
         else -> {
            failIf(isFinal xor (def.editable===EditMode.NONE)) { "Property mutability does not correspond to specified editability=${def.editable}" }

            val propertyType = type.resolveNullability()
            FieldConfig(VType<T>(propertyType), name, def.toDef(), setOf(), instance, group, p.asIs()).asIs()
         }
      }
   }.getOrSupply { e ->
      fail(e) { "Can not create config from field=${p.name} for class=${p.javaField?.declaringClass ?: p.javaGetter?.declaringClass}" }
   }
}