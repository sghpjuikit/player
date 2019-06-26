@file:Suppress("FINAL_UPPER_BOUND")

package sp.it.util.conf

import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.ObservableList
import sp.it.util.access.V
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.action.Action
import sp.it.util.action.IsAction
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
import sp.it.util.functional.asIf
import sp.it.util.reactive.attach
import sp.it.util.reactive.sync
import sp.it.util.type.InstanceMap
import sp.it.util.type.Util.getRawGenericPropertyType
import sp.it.util.validation.Constraint
import java.io.File
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

fun <T: Any> c(initialValue: T): ConfS<T> = ConfS(initialValue).but(Constraint.ObjectNonNull)
fun <T: Any> cn(initialValue: T?): ConfS<T?> = ConfS(initialValue)
fun <T: Any> cv(initialValue: T): ConfV<T, V<T>> = ConfV<T, V<T>>(initialValue, { v(it) }).but(Constraint.ObjectNonNull)
fun <T: Any, W: WritableValue<T>> cv(initialValue: T, valueSupplier: (T) -> W): ConfV<T, W> = ConfV(initialValue, valueSupplier).but(Constraint.ObjectNonNull)
fun <T: Any, W: ObservableValue<T>> cvro(initialValue: T, valueSupplier: (T) -> W): ConfVRO<T, W> = ConfVRO(initialValue, valueSupplier).but(Constraint.ObjectNonNull)
fun <T: Any> cvn(initialValue: T?): ConfV<T?, V<T?>> = ConfV(initialValue, { vn(it) })
fun <T: Any, W: WritableValue<T?>> cvn(initialValue: T?, valueSupplier: (T?) -> W): ConfV<T?, W> = ConfV(initialValue, valueSupplier)
fun <T: Any, W: ObservableValue<T?>> cvnro(initialValue: T?, valueSupplier: (T?) -> W): ConfVRO<T?, W> = ConfVRO(initialValue, valueSupplier)
fun <T: () -> Unit> cr(action: T): ConfR<T> = ConfR(action)
inline fun <reified T: Any?> cList(): ConfL<T> = ConfL(T::class.java, null is T)

/** Adds the specified constraint for this [Config], which allows value restriction and fine-grained behavior. */
fun <T: Any?, C: Conf<T>> C.but(vararg restrictions: Constraint<T>) = apply { constraints += restrictions }

fun <T: String, C: Conf<T>> C.nonEmpty() = but(Constraint.StringNonEmpty())
fun <T: Number, C: Conf<T>> C.min(min: T) = but(Constraint.NumberMinMax(min.toDouble(), Double.MAX_VALUE))
fun <T: Number, C: Conf<T>> C.max(max: T) = but(Constraint.NumberMinMax(Double.MIN_VALUE, max.toDouble()))
fun <T: Number, C: Conf<T>> C.between(min: T, max: T) = but(Constraint.NumberMinMax(min.toDouble(), max.toDouble()))
fun <T: File?, C: Conf<T>> C.only(type: Constraint.FileActor) = but(type)
fun <T: Any, C: Conf<T>> C.readOnlyIf(condition: Boolean) = but(Constraint.ReadOnlyIf(condition))
fun <T: Any, C: Conf<T>> C.readOnlyIf(condition: ObservableValue<Boolean>) = but(Constraint.ReadOnlyIf(condition, false))
fun <T: Any, C: Conf<T>> C.readOnlyUnless(condition: Boolean) = but(Constraint.ReadOnlyIf(!condition))
fun <T: Any, C: Conf<T>> C.readOnlyUnless(condition: ObservableValue<Boolean>) = but(Constraint.ReadOnlyIf(condition, true))

/**
 * Gives hint to ui editor to use the specified to storing converter instead of the default one.
 * This provides a fine-grained per [Config] control for this behavior.
 */
fun <T: Any?, C: Conf<T>> C.uiConverter(converter: (T) -> String) = but(Constraint.UiConverter(converter))

/**
 * If this config is [enumerable][Config.isTypeEnumerable], the value order in ui editor will follow the order of the
 * enumeration, i.e. no sort will be applied.
 */
fun <T: Any?, C: Conf<T>> C.uiNoOrder() = but(Constraint.PreserveOrder)

/**
 * Restricts the allowed value to those contained in the specified value range.
 *
 * Consistency:
 *
 * In order to guarantee consistency, the enumerator must be immutable collection.
 * In case it is mutable, remove operation mey render the config's value out of range. It is responsibility of the
 * caller to recover from such case.
 * At the minimum, mutable collection should be [observable][javafx.beans.Observable].
 *
 * Observability:
 *
 * If the enumerator collection is [observable][javafx.beans.Observable], the value range in the ui editor will reflect the
 * changes.
 *
 * Nullability:
 *
 * Nullability of this config is respected and reflected by nullability of the elements in the value range. I.e., if
 * this config is nullable, the collection may contain null value. However it is responsibility of the caller, hence
 * even of this config is nullable it may not give user the option to select null.
 *
 * @param enumerator value range supplier
 */
fun <T: Any?, C: Conf<T>> C.values(enumerator: Collection<T>) = but(Constraint.ValueSet { enumerator })

/**
 * Restricts the allowed value to those contained in the specified value range.
 *
 * This method breaks consistency as if mutable not observable collection would be used. See [values] for details.
 *
 * @param enumerator value range supplier
 */
fun <T: Any?, C: Conf<T>> C.valuesIn(enumerator: () -> Sequence<T>) = but(Constraint.ValueSet { enumerator().toList() })

/**
 * Restricts the allowed value to those contained in the specified value range.
 *
 * Nullability:
 *
 * The specified generic type argument will be used as a key to get the value range. Nullability is respected,
 * thus if it is nullable, the value range given by the instance source will also contain null as a possible
 * value.
 *
 * @param instanceSource instance source that maps values to types and which will be used to find the exact
 * value range, which is the association in the source for the type represented by the specified generic type
 * argument
 */
inline fun <reified T: Any?, C: Conf<T>> C.valuesIn(instanceSource: InstanceMap) = values(instanceSource.getInstances())

/** Singleton configuration used by delegated configurable properties. */
private val configuration = object: ConfigValueSource {
   override fun register(config: Config<*>) {
      MainConfiguration.collect(config)
   }

   override fun initialize(config: Config<*>) {
      MainConfiguration.rawSet(config)
   }
}

/** Contract to specify per-instance configurable group (discriminant) for delegated config properties. */
interface MultiConfigurable {
   /** Group suffix shared by all configs of this configurable. Usually unique for every instances. */
   val configurableDiscriminant: String?
   /** Group (shared prefix path) shared by all configs of this configurable. Usually same for all instances. */
   @JvmDefault
   val configurableGroup
      get() = computeConfigGroup(this)
   /** Config register and value provider. By default common value store. */
   @JvmDefault
   val configurableValueStore: ConfigValueSource
      get() = configuration
}

/** Implementation of [MultiConfigurable] with the [MultiConfigurable.configurableDiscriminant] supplied at creation time. */
open class MultiConfigurableBase(override val configurableDiscriminant: String): MultiConfigurable

interface ConfigValueSource {
   fun register(config: Config<*>)
   fun initialize(config: Config<*>)

   companion object {

      fun empty() = object: ConfigValueSource {
         override fun initialize(config: Config<*>) {}
         override fun register(config: Config<*>) {}
      }

      fun <T> simple() = SimpleConfigValueStore<T>()

      open class SimpleConfigValueStore<T>: ConfigValueSource, Configurable<T> {
         private val configs = ArrayList<Config<T>>()

         override fun getField(name: String): Config<T>? = configs.find { it.name==name }

         override fun getFields() = configs

         override fun initialize(config: Config<*>) {}

         override fun register(config: Config<*>) {
            @Suppress("UNCHECKED_CAST")
            configs += config as Config<T>
         }
      }
   }
}


interface Delegator<REF: Any?, D: Any> {
   operator fun provideDelegate(ref: REF, property: KProperty<*>): D
}

fun <REF: Any, DP: Any> Delegator<REF, DP>.forInstance(instance: REF): Delegator<Nothing?, DP> = object: Delegator<Nothing?, DP> {
   override fun provideDelegate(ref: Nothing?, property: KProperty<*>): DP = this@forInstance.provideDelegate(instance, property)
}

abstract class Conf<T: Any?> {
   protected var refSubstitute: Any? = null
   val constraints = HashSet<Constraint<T>>()

   protected fun addAnnotationConstraints(type: Class<T>, property: KProperty<*>) {
      constraints += obtainConfigConstraints(type, property.annotations)
   }

   protected fun KProperty<*>.obtainConfigMetadata() = findAnnotation<IsConfig>() ?: fail { "${IsConfig::class} annotation required for $this" }

   protected fun validateValue(v: T) {
      constraints.forEach { it.validate(v).ifError { failIf(true) { "Value $v doesn't conform to: $it" } } }
   }

   protected fun KProperty<*>.makeAccessible() = apply {
      isAccessible = true
      javaField?.isAccessible = true
   }

   fun obtainConfigValueStore(ref: Any?): ConfigValueSource = when (ref) {
      is MultiConfigurable -> ref.configurableValueStore
      else -> configuration
   }

   protected fun <CFGT, CFG: Config<CFGT>> CFG.registerConfig(ref: Any?) = apply {
      obtainConfigValueStore(ref).register(this)
   }
}

class ConfR<T: () -> Unit>(private val action: T): Conf<T>() {
   operator fun provideDelegate(ref: Any, property: KProperty<*>): ReadOnlyProperty<Any, T> {
      property.makeAccessible()
      val info = property.findAnnotation<IsConfig>()
      val infoExt = property.findAnnotation<IsAction>()
      val group = info.computeConfigGroup(ref)

      val isFinal = property !is KMutableProperty
      failIf(!isFinal) { "Property must be immutable" }

      fun String.orNull() = takeIf { it.isNotBlank() }
      val name = infoExt?.name?.orNull() ?: info?.name?.orNull() ?: property.name
      val desc = infoExt?.desc?.orNull() ?: info?.info?.orNull()
      val keys = infoExt?.keys ?: ""
      val isGlobal = infoExt?.global ?: false
      val isContinuous = infoExt?.repeat ?: false

      return object: Action(name, Runnable { action() }, desc, group, keys, isGlobal, isContinuous), ReadOnlyProperty<Any, T> {
         override fun getValue(thisRef: Any, property: KProperty<*>) = action
      }.registerConfig(ref)
   }
}

class ConfL<T: Any?>(val type: Class<T>, val isNullable: Boolean): Conf<T>() {
   operator fun provideDelegate(ref: Any, property: KProperty<*>): ReadOnlyProperty<Any, ObservableList<T>> {
      property.makeAccessible()
      val info = property.obtainConfigMetadata()
      val group = info.computeConfigGroup(ref)
      addAnnotationConstraints(type, property)

      val isFinal = property !is KMutableProperty
      failIf(!isFinal) { "Property must be immutable" }

      val list = Config.VarList<T>(type, if (isNullable) Config.VarList.Elements.NULLABLE else Config.VarList.Elements.NOT_NULL)
      val c = Config.ListConfig(property.name, info, list, group, constraints)
      obtainConfigValueStore(ref).initialize(c)

      return object: Config.ListConfig<T>(property.name, info, list, group, constraints), ReadOnlyProperty<Any, ObservableList<T>> {
         override fun getValue(thisRef: Any, property: KProperty<*>) = list.list
      }.registerConfig(ref)
   }
}

@Suppress("UNCHECKED_CAST")
class ConfS<T: Any?>(private val initialValue: T): Conf<T>() {

   operator fun provideDelegate(ref: Any, property: KProperty<*>): ReadWriteProperty<Any, T> {
      property.makeAccessible()
      val info = property.obtainConfigMetadata()
      val type = property.returnType.javaType as Class<T>
      val group = info.computeConfigGroup(ref)
      addAnnotationConstraints(type, property)

      val isFinal = property !is KMutableProperty
      failIf(isFinal xor (info.editable===EditMode.NONE)) { "Property mutability does not correspond to specified editability=${info.editable}" }

      val c = ValueConfig(type, property.name, "", initialValue, group, "", info.editable).constraints(constraints)
      obtainConfigValueStore(ref).initialize(c)
      validateValue(c.value)

      return object: Config.PropertyConfig<T>(type, property.name, info, constraints, vn(c.value), initialValue, group), ReadOnlyProperty<Any, T>, ReadWriteProperty<Any, T> {
         override fun getValue(thisRef: Any, property: KProperty<*>) = getValue()
         override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            setValue(value)
         }
      }.registerConfig(ref)
   }
}

@Suppress("UNCHECKED_CAST")
class ConfV<T: Any?, W: WritableValue<T>>: Conf<T>, Delegator<Any, ReadOnlyProperty<Any?, W>> {
   private val initialValue: T
   private var v: (T) -> W

   constructor(initialValue: T, valueSupplier: (T) -> W): super() {
      this.initialValue = initialValue
      this.v = valueSupplier
   }

   /** Invokes [attach] with the specified block on the observable value that will be created and returns this. */
   infix fun attach(block: (T) -> Unit) = apply {
      val s = v
      v = { s(it).apply { asIf<ObservableValue<T>>()?.attach(block) } }
   }

   /** Invokes [sync] with the specified block on the observable value that will be created and returns this. */
   infix fun sync(block: (T) -> Unit) = apply {
      val s = v
      v = { s(it).apply { asIf<ObservableValue<T>>()?.sync(block) } }
   }

   override operator fun provideDelegate(ref: Any, property: KProperty<*>): ReadOnlyProperty<Any?, W> {
      property.makeAccessible()
      val info = property.obtainConfigMetadata()
      val type = getRawGenericPropertyType(property.returnType.javaType) as Class<T>
      val group = info.computeConfigGroup(ref)
      addAnnotationConstraints(type, property)

      val isFinal = property !is KMutableProperty
      failIf(!isFinal) { "Property must be immutable" }

      val c = ValueConfig(type, property.name, "", initialValue, group, "", info.editable).constraints(constraints)
      obtainConfigValueStore(ref).initialize(c)
      validateValue(c.value)

      return object: Config.PropertyConfig<T>(type, property.name, info, constraints, v(c.value), initialValue, group), ReadOnlyProperty<Any?, W> {
         override fun getValue(thisRef: Any?, property: KProperty<*>): W = this.property as W
      }.registerConfig(ref)
   }

}

@Suppress("UNCHECKED_CAST")
class ConfVRO<T: Any?, W: ObservableValue<T>>: Conf<T>, Delegator<Any, ReadOnlyProperty<Any?, W>> {
   private val initialValue: T
   private var v: (T) -> W

   constructor(initialValue: T, valueSupplier: (T) -> W): super() {
      this.initialValue = initialValue
      this.v = valueSupplier
   }

   /** Invokes [attach] with the specified block on the observable value that will be created and returns this. */
   infix fun attach(block: (T) -> Unit) = apply {
      val s = v
      v = { s(it).apply { attach(block) } }
   }

   /** Invokes [sync] with the specified block on the observable value that will be created and returns this. */
   infix fun sync(block: (T) -> Unit) = apply {
      val s = v
      v = { s(it).apply { sync(block) } }
   }

   override operator fun provideDelegate(ref: Any, property: KProperty<*>): ReadOnlyProperty<Any?, W> {
      property.makeAccessible()
      val info = property.obtainConfigMetadata()
      val type = getRawGenericPropertyType(property.returnType.javaType) as Class<T>
      val group = info.computeConfigGroup(ref)
      addAnnotationConstraints(type, property)

      val isFinal = property !is KMutableProperty
      failIf(!isFinal) { "Property must be immutable" }
      failIf(info.editable!==EditMode.NONE) { "Property mutability requires usage of ${EditMode.NONE}" }

      val c = ValueConfig(type, property.name, "", initialValue, group, "", info.editable).constraints(constraints)
      validateValue(c.value)

      return object: Config.ReadOnlyPropertyConfig<T>(type, property.name, info, constraints, v(c.value), group), ReadOnlyProperty<Any?, W> {
         override fun getValue(thisRef: Any?, property: KProperty<*>): W = this.property as W
      }.registerConfig(ref)
   }

}