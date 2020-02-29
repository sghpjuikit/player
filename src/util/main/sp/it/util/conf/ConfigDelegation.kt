@file:Suppress("FINAL_UPPER_BOUND")

package sp.it.util.conf

import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import sp.it.util.access.V
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.access.vx
import sp.it.util.action.Action
import sp.it.util.action.IsAction
import sp.it.util.dev.failIf
import sp.it.util.file.FileType
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.toUnit
import sp.it.util.reactive.attach
import sp.it.util.reactive.sync
import sp.it.util.type.InstanceMap
import sp.it.util.type.VType
import sp.it.util.type.argOf
import sp.it.util.type.type
import sp.it.util.type.typeResolved
import java.io.File
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod
import kotlin.properties.ReadOnlyProperty as RoProperty
import kotlin.properties.ReadWriteProperty as RwProperty

fun <T: Any> c(initialValue: T): ConfS<T> = ConfS(initialValue).nonNull()
fun <T: Any> cn(initialValue: T?): ConfS<T?> = ConfS(initialValue)
fun <T: Any> cv(initialValue: T): ConfV<T, V<T>> = ConfV(initialValue, { v(it) }).nonNull()
fun <T: Any, W: WritableValue<T>> cv(initialValue: T, valueSupplier: (T) -> W): ConfV<T, W> = ConfV(initialValue, valueSupplier).nonNull()
fun <T: Any, W: ObservableValue<T>> cvro(initialValue: T, valueSupplier: (T) -> W): ConfVRO<T, W> = ConfVRO(initialValue, valueSupplier).nonNull()
fun <T: Any> cvn(initialValue: T?): ConfV<T?, V<T?>> = ConfV(initialValue, { vn(it) })
fun <T: Any, W: WritableValue<T?>> cvn(initialValue: T?, valueSupplier: (T?) -> W): ConfV<T?, W> = ConfV(initialValue, valueSupplier)
fun <T: Any, W: ObservableValue<T?>> cvnro(initialValue: T?, valueSupplier: (T?) -> W): ConfVRO<T?, W> = ConfVRO(initialValue, valueSupplier)
fun <T: () -> Unit> cr(action: T): ConfR = ConfR(action).nonNull()
inline fun <reified T: Any?> cList(vararg initialItems: T): ConfL<T> = ConfL(ConfList(type(), observableArrayList(*initialItems))).nonNull()
inline fun <reified T: Any?> cList(noinline itemFactory: () -> T, noinline itemToConfigurable: (T) -> Configurable<*>, vararg initialItems: T): ConfL<T> = ConfL(ConfList(type(), itemFactory, itemToConfigurable, *initialItems)).nonNull()
inline fun <reified T: Any?> cCheckList(vararg initialItems: T): ConfCheckL<T, Boolean> = ConfCheckL(CheckList.nonNull(type(), initialItems.toList())).nonNull()
inline fun <reified T: Any?, S: Boolean?> cCheckList(checkList: CheckList<T, S>): ConfCheckL<T, S> = ConfCheckL(checkList).nonNull()

/** Adds the specified constraint for this delegated [Config], which allows value restriction and fine-grained behavior. */
fun <T: Any?, C: Conf<T>> C.but(vararg restrictions: Constraint<T>) = apply { constraints += restrictions }

/** Adds the specified constraint for elements of the list of this delegated [Config], which allows value restriction and fine-grained behavior. */
fun <T: Any?, C: ConfL<T>> C.butElement(vararg restrictions: Constraint<T>) = apply { elementConstraints += restrictions }

fun <T: Any?, C: Conf<T>> C.noUi() = but(Constraint.NoUi)
fun <T: Any?, C: Conf<T>> C.noPersist() = but(Constraint.NoPersist)
fun <T: Any?, C: Conf<T>> C.nonNull() = but(Constraint.ObjectNonNull)
fun <T: String, C: Conf<T>> C.nonEmpty() = but(Constraint.StringNonEmpty())
fun <T: Number, C: Conf<T>> C.min(min: T) = but(Constraint.NumberMinMax(min.toDouble(), null))
fun <T: Number, C: Conf<T>> C.max(max: T) = but(Constraint.NumberMinMax(null, max.toDouble()))
fun <T: Number, C: Conf<T>> C.between(min: T, max: T) = but(Constraint.NumberMinMax(min.toDouble(), max.toDouble()))
fun <T: File?, C: Conf<T>> C.uiOut() = but(Constraint.FileOut)
fun <T: File?, C: Conf<T>> C.only(type: FileType) = but(Constraint.FileActor(type))
fun <T: File?, C: Conf<T>> C.only(type: Constraint.FileActor) = but(type)
fun <T: File?, C: Conf<T>> C.relativeTo(relativeTo: File) = but(Constraint.FileRelative(relativeTo))
fun <T: File?, C: ConfL<T>> C.only(type: FileType) = butElement(Constraint.FileActor(type))
fun <T: File?, C: ConfL<T>> C.only(type: Constraint.FileActor) = butElement(type)
fun <T: File?, C: ConfL<T>> C.relativeTo(relativeTo: File) = butElement(Constraint.FileRelative(relativeTo))
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

/** @return action created from this function into the specified configurable or throws if this method is not an action */
fun KFunction<*>.getDelegateAction(within: Configurable<*>): Action = within.getConfig(findAnnotation<IsAction>()!!.name).asIs()

/** @return config created from this property or throws if this property is not delegated configurable property */
fun KProperty0<*>.getDelegateConfig(): Config<*> {
   isAccessible = true
   return getDelegate().asIs()
}

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
 * even if this config is nullable it may not give user the option to select null if the enumerator does not contain it.
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

/** Allows defining delegated configurable properties. */
interface ConfigDelegator {
   /** Group prefix shared by all configs of this configurable. */
   val configurableGroupPrefix: String?
      get() = null

   /** Config register and value provider. By default common value store. */
   val configurableValueSource: ConfigValueSource
}

/** Implementation of [ConfigDelegator] with [MainConfiguration] as [configurableValueSource]. */
interface GlobalConfigDelegator: ConfigDelegator {
   override val configurableValueSource
      get() = configuration
}

/** [GlobalConfigDelegator] with [ConfigDelegator.configurableGroupPrefix] supplied at creation time. */
open class GlobalSubConfigDelegator(override val configurableGroupPrefix: String? = null): GlobalConfigDelegator

interface ConfigValueSource {
   fun register(config: Config<*>)
   fun initialize(config: Config<*>)

   companion object {

      fun empty() = EmptyConfigValueSource

      fun <T> simple() = SimpleConfigValueStore<T>()

      object EmptyConfigValueSource: ConfigValueSource {
         override fun initialize(config: Config<*>) {}
         override fun register(config: Config<*>) {}
      }

      open class SimpleConfigValueStore<T>: ConfigValueSource, Configurable<T> {
         private val configs = ArrayList<Config<T>>()

         override fun getConfig(name: String): Config<T>? = configs.find { it.name==name }

         override fun getConfigs() = configs

         override fun initialize(config: Config<*>) {}

         @Suppress("UNCHECKED_CAST")
         override fun register(config: Config<*>) = configs.add(config as Config<T>).toUnit()

      }
   }
}

interface ConfigPropertyDelegator<REF: ConfigDelegator, D: Any> {
   operator fun provideDelegate(ref: REF, property: KProperty<*>): D
}

abstract class Conf<T: Any?> {
   protected var refSubstitute: Any? = null
   val constraints = HashSet<Constraint<T>>()
   var def: ConfigDefinition? = null

   protected fun KProperty<*>.obtainConfigMetadata() = null
      ?: def
      ?: findAnnotation<IsConfig>()?.toDef()
      ?: ConfigDef(name)

   protected fun validateValue(v: T) {
      constraints.forEach { it.validate(v).ifError { failIf(true) { "Value $v doesn't conform to: $it" } } }
   }

   protected fun KCallable<*>.makeAccessible() = apply {
      isAccessible = true
      if (this is KProperty<*>) javaField?.isAccessible = true
      if (this is KFunction<*>) javaMethod?.isAccessible = true
   }

   protected fun <CFGT, CFG: Config<CFGT>> CFG.registerConfig(ref: ConfigDelegator) = apply {
      ref.configurableValueSource.register(this)
   }
}

class ConfR(private val action: () -> Unit): Conf<Action>() {
   operator fun provideDelegate(ref: ConfigDelegator, property: KCallable<*>): RoProperty<ConfigDelegator, Action> {
      property.makeAccessible()
      val info = def ?: property.findAnnotation<IsConfig>()?.toDef()
      val infoExt = property.findAnnotation<IsAction>()
      val group = info.computeConfigGroup(ref)

      val isFinal = property !is KMutableProperty
      failIf(!isFinal) { "Property must be immutable" }

      fun String.orNull() = takeIf { it.isNotBlank() }
      val name = infoExt?.name?.orNull() ?: info?.name?.orNull() ?: property.name
      val desc = infoExt?.info?.orNull() ?: info?.info?.orNull() ?: ""
      val keys = infoExt?.keys ?: ""
      val isGlobal = infoExt?.global ?: false
      val isContinuous = infoExt?.repeat ?: false

      val c = ValueConfig(type(), name, name, Action.Data(isGlobal, keys), group, desc, EditMode.USER)
      ref.configurableValueSource.initialize(c)
      val cv = c.value

      return object: Action(name, Runnable { action() }, desc, group, cv.keys, cv.isGlobal, isContinuous, *constraints.toTypedArray()), RoProperty<ConfigDelegator, Action> {
         override fun getValue(thisRef: ConfigDelegator, property: KProperty<*>) = this
      }.registerConfig(ref)
   }
}

class ConfL<T: Any?>(val list: ConfList<T>): Conf<ObservableList<T>>() {
   val elementConstraints = HashSet<Constraint<T>>()

   operator fun provideDelegate(ref: ConfigDelegator, property: KProperty<*>): RoProperty<ConfigDelegator, ObservableList<T>> {
      property.makeAccessible()
      val info = property.obtainConfigMetadata()
      val group = info.computeConfigGroup(ref)

      val isFinal = property !is KMutableProperty
      failIf(!isFinal) { "Property must be immutable" }

      val c = ListConfig(property.name, info, list, group, constraints, elementConstraints)
      ref.configurableValueSource.initialize(c)

      return object: ListConfig<T>(property.name, info, list, group, constraints, elementConstraints), RoProperty<ConfigDelegator, ObservableList<T>> {
         override fun getValue(thisRef: ConfigDelegator, property: KProperty<*>) = list.list
      }.registerConfig(ref)
   }
}

class ConfCheckL<T: Any?, S: Boolean?>(val list: CheckList<T, S>): Conf<CheckList<T, S>>() {

   operator fun provideDelegate(ref: ConfigDelegator, property: KProperty<*>): RoProperty<ConfigDelegator, CheckList<T, S>> {
      property.makeAccessible()
      val info = property.obtainConfigMetadata()
      val group = info.computeConfigGroup(ref)

      val isFinal = property !is KMutableProperty
      failIf(!isFinal) { "Property must be immutable" }

      val c = CheckListConfig(property.name, info, list, group, constraints)
      ref.configurableValueSource.initialize(c)

      return CheckListConfig(property.name, info, list, group, constraints).registerConfig(ref)
   }
}

class ConfS<T: Any?>(private val initialValue: T): Conf<T>() {

   operator fun provideDelegate(ref: ConfigDelegator, property: KProperty<*>): RwProperty<ConfigDelegator, T> {
      property.makeAccessible()
      val info = property.obtainConfigMetadata()
      val type = VType<T>(property.returnType)
      val group = info.computeConfigGroup(ref)

      val isFinal = property !is KMutableProperty
      failIf(isFinal xor (info.editable===EditMode.NONE)) { "Property mutability does not correspond to specified editability=${info.editable}" }

      val c = ValueConfig(type, property.name, "", initialValue, group, "", info.editable).addConstraints(constraints)
      ref.configurableValueSource.initialize(c)
      validateValue(c.value)

      return object: PropertyConfig<T>(type, property.name, info, constraints, vx(c.value), initialValue, group), RwProperty<ConfigDelegator, T> {
         @Suppress("UsePropertyAccessSyntax")
         override fun getValue(thisRef: ConfigDelegator, property: KProperty<*>) = getValue()
         override fun setValue(thisRef: ConfigDelegator, property: KProperty<*>, value: T) = setValue(value)
      }.registerConfig(ref)
   }
}

class ConfV<T: Any?, W: WritableValue<T>>: Conf<T>, ConfigPropertyDelegator<ConfigDelegator, RoProperty<ConfigDelegator, W>> {
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

   override operator fun provideDelegate(ref: ConfigDelegator, property: KProperty<*>): RoProperty<ConfigDelegator, W> {
      property.makeAccessible()
      val info = property.obtainConfigMetadata()
      val type = VType<T>(property.returnType.argOf(WritableValue::class, 0).typeResolved)
      val group = info.computeConfigGroup(ref)

      val isFinal = property !is KMutableProperty
      failIf(!isFinal) { "Property must be immutable" }

      val c = ValueConfig(type, property.name, "", initialValue, group, "", info.editable).addConstraints(constraints)
      ref.configurableValueSource.initialize(c)
      validateValue(c.value)

      return object: PropertyConfig<T>(type, property.name, info, constraints, v(c.value), initialValue, group), RoProperty<ConfigDelegator, W> {
         @Suppress("UNCHECKED_CAST")
         override fun getValue(thisRef: ConfigDelegator, property: KProperty<*>): W = this.property as W
      }.registerConfig(ref)
   }

}

@Suppress("UNCHECKED_CAST")
class ConfVRO<T: Any?, W: ObservableValue<T>>: Conf<T>, ConfigPropertyDelegator<ConfigDelegator, RoProperty<ConfigDelegator, W>> {
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

   override operator fun provideDelegate(ref: ConfigDelegator, property: KProperty<*>): RoProperty<ConfigDelegator, W> {
      property.makeAccessible()
      val info = property.obtainConfigMetadata()
      val type = VType<T>(property.returnType.argOf(ObservableValue::class, 0).typeResolved)
      val group = info.computeConfigGroup(ref)

      val isFinal = property !is KMutableProperty
      failIf(!isFinal) { "Property must be immutable" }
      failIf(info.editable!==EditMode.NONE) { "Property mutability requires usage of ${EditMode.NONE}" }

      val c = ValueConfig(type, property.name, "", initialValue, group, "", info.editable).addConstraints(constraints)
      validateValue(c.value)

      return object: PropertyConfigRO<T>(type, property.name, info, constraints, v(c.value), group), RoProperty<ConfigDelegator, W> {
         override fun getValue(thisRef: ConfigDelegator, property: KProperty<*>): W = this.property as W
      }.registerConfig(ref)
   }

}