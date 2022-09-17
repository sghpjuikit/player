package sp.it.util.conf

import kotlin.properties.ReadOnlyProperty as RoProperty
import kotlin.properties.ReadWriteProperty as RwProperty
import javafx.beans.property.Property
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod
import sp.it.util.access.OrV
import sp.it.util.access.OrV.OrValue
import sp.it.util.access.OrV.OrValue.Initial.Inherit
import sp.it.util.access.V
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.access.vx
import sp.it.util.action.Action
import sp.it.util.action.ActionDb
import sp.it.util.action.IsAction
import sp.it.util.dev.failIf
import sp.it.util.functional.Try
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.toUnit
import sp.it.util.reactive.Unsubscriber
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncBiFromWithOverride
import sp.it.util.type.VType
import sp.it.util.type.argOf
import sp.it.util.type.type
import sp.it.util.type.typeOrNothing

/** Non-observable non-null configurable value. Backed by [PropertyConfig]. */
fun <T: Any> c(initialValue: T): ConfS<T> = ConfS(initialValue).nonNull()
/** Non-observable nullable configurable value. Backed by [PropertyConfig]. */
fun <T: Any> cn(initialValue: T?): ConfS<T?> = ConfS(initialValue)
/** Writable observable non-null configurable value. Backed by [PropertyConfig]. */
fun <T: Any> cv(initialValue: T): ConfV<T, V<T>> = ConfV(initialValue, { v(it) }).nonNull()
/** Writable observable non-null configurable value supplied by the specified [valueFactory]. Backed by [PropertyConfig]. */
fun <T: Any, W: WritableValue<T>> cv(initialValue: T, valueFactory: (T) -> W): ConfV<T, W> = ConfV(initialValue, valueFactory).nonNull()
/** Writable observable non-null configurable value supplied by the specified [value]. Backed by [PropertyConfig]. */
fun <T: Any, W: WritableValue<T>> cv(value: W): ConfV<T, W> = cv(value.value, { value }).nonNull()
/** Read-only observable non-null configurable value supplied by the specified [valueFactory]. Backed by [PropertyConfigRO]. */
fun <T: Any, W: ObservableValue<T>> cvro(initialValue: T, valueFactory: (T) -> W): ConfVRO<T, W> = ConfVRO(initialValue, valueFactory).nonNull()
/** Read-only observable non-null configurable value supplied by the specified [value]. Backed by [PropertyConfigRO]. */
fun <T: Any, W: ObservableValue<T>> cvro(value: W): ConfVRO<T, W> = cvro(value.value, { value }).nonNull()
/** Writable observable nullable configurable value. Backed by [PropertyConfig]. */
fun <T: Any> cvn(initialValue: T?): ConfV<T?, V<T?>> = ConfV(initialValue, { vn(it) })
/** Writable observable nullable configurable value supplied by the specified [valueFactory]. Backed by [PropertyConfig]. */
fun <T: Any, W: WritableValue<T?>> cvn(initialValue: T?, valueFactory: (T?) -> W): ConfV<T?, W> = ConfV(initialValue, valueFactory)
/** Writable observable nullable configurable value supplied by the specified [value]. Backed by [PropertyConfig]. */
fun <T: Any, W: WritableValue<T?>> cvn(value: W): ConfV<T?, W> = cvn(value.value, { value })
/** Read-only observable nullable configurable value supplied by the specified [valueFactory]. Backed by [PropertyConfigRO]. */
fun <T: Any, W: ObservableValue<T?>> cvnro(initialValue: T?, valueFactory: (T?) -> W): ConfVRO<T?, W> = ConfVRO(initialValue, valueFactory)
/** Read-only observable nullable configurable value supplied by the specified [value]. Backed by [PropertyConfigRO]. */
fun <T: Any, W: ObservableValue<T?>> cvnro(value: W): ConfVRO<T?, W> = cvnro(value.value, { value })
/** Configurable action. Backed by [Action]. */
fun <T: () -> Unit> cr(action: T): ConfR = ConfR(action).nonNull()
/** Inheritable observable non-null configurable value. Subscribed to the specified [parent] until the specified [unsubscriber] is called. Backed by [OrPropertyConfig]. */
fun <T: Any> cOr(parent: KProperty0<Property<T>>, initialValue: OrValue.Initial<T> = Inherit(), unsubscriber: Unsubscriber): ConfVOr<T, OrV<T>> = ConfVOr { OrV(parent.call(), initialValue) on unsubscriber }.nonNull()
/** Inheritable observable nullable configurable value. Subscribed to the specified [parent] until the specified [unsubscriber] is called. Backed by [OrPropertyConfig]. */
fun <T: Any?> cnOr(parent: KProperty0<Property<T>>, initialValue: OrValue.Initial<T> = Inherit(), unsubscriber: Unsubscriber): ConfVOr<T, OrV<T>> = ConfVOr { OrV(parent.call(), initialValue) on unsubscriber }
/** Inheritable observable non-null configurable value. Subscribed to the specified [parent] and [syncBiFromWithOverride]d to the specified [child] value, until the specified [unsubscriber] is called. Backed by [OrPropertyConfig]. */
fun <T: Any> cOr(parent: KProperty0<Property<T>>, child: Property<T>, initialValue: OrValue.Initial<T> = Inherit(), unsubscriber: Unsubscriber): ConfVOr<T, OrV<T>> = ConfVOr { OrV(parent.call(), initialValue).apply { child syncBiFromWithOverride this on unsubscriber } on unsubscriber }.nonNull()
/** Inheritable observable nullable configurable value. Subscribed to the specified [parent] and [syncBiFromWithOverride]d to the specified [child] value, until the specified [unsubscriber] is called. Backed by [OrPropertyConfig]. */
fun <T: Any?> cnOr(parent: KProperty0<Property<T>>, child: Property<T>, initialValue: OrValue.Initial<T> = Inherit(), unsubscriber: Unsubscriber): ConfVOr<T, OrV<T>> = ConfVOr { OrV(parent.call(), initialValue).apply { child syncBiFromWithOverride this on unsubscriber } on unsubscriber }
/** Observable reified configurable list. Backed by [ListConfig]. */
inline fun <reified T: Any?> cList(vararg initialItems: T): ConfL<T> = ConfL(ConfList(type(), observableArrayList(*initialItems))).nonNull()
/** Observable reified configurable list. Backed by [ListConfig]. */
inline fun <reified T: Any?> cList(noinline itemFactory: () -> T, noinline itemToConfigurable: (T) -> Configurable<*>, vararg initialItems: T): ConfL<T> = ConfL(ConfList(type(), itemFactory, itemToConfigurable, *initialItems)).nonNull()
/** Observable reified configurable checked list. Backed by [CheckListConfig]. */
inline fun <reified T: Any?> cCheckList(vararg initialItems: T): ConfCheckL<T, Boolean> = ConfCheckL(CheckList.nonNull(type(), initialItems.toList())).nonNull()
/** Observable reified configurable checked list. Backed by [CheckListConfig]. */
inline fun <reified T: Any?, S: Boolean?> cCheckList(checkList: CheckList<T, S>): ConfCheckL<T, S> = ConfCheckL(checkList).nonNull()

/** Adds the specified constraint for this delegated [Config], which allows value restriction and fine-grained behavior. */
fun <T: Any?, C: Conf<T>> C.but(vararg restrictions: Constraint<T>) = apply { constraints += restrictions }
/** Adds the specified constraint for this delegated [Config], which allows value restriction and fine-grained behavior. */
fun <T: Any?, C: Conf<T>> C.but(block: ConstrainedDsl<T>.() -> Unit) = apply { ConstrainedDsl(constraints::add).block() }

/** Adds the specified constraint for elements of the list of this delegated [Config], which allows value restriction and fine-grained behavior. */
fun <T: Any?, C: ConfL<T>> C.butElement(vararg restrictions: Constraint<T>) = apply { elementConstraints += restrictions }
/** Adds the specified constraint for elements of the list of this delegated [Config], which allows value restriction and fine-grained behavior. */
fun <T: Any?, C: ConfL<T>> C.butElement(block: ConstrainedDsl<T>.() -> Unit) = apply { ConstrainedDsl(elementConstraints::add).block() }

/** Adds the specified constraint for overridden value this delegated [Config], which allows value restriction and fine-grained behavior. */
fun <T: Any?, W: OrV<T>, C: ConfVOr<T, W>> C.butOverridden(vararg restrictions: Constraint<T>) = apply { elementConstraints += restrictions }
/** Adds the specified constraint for overridden value this delegated [Config], which allows value restriction and fine-grained behavior. */
fun <T: Any?, W: OrV<T>, C: ConfVOr<T, W>> C.butOverridden(block: ConstrainedDsl<T>.() -> Unit) = apply { ConstrainedDsl(elementConstraints::add).block() }

/**
 * Gives hint to ui editor to use the specified to-string converter for elements instead of the default one.
 * This provides a fine-grained per [Config] control for this behavior.
 */
fun <T: Any?, S: Boolean?, C: ConfCheckL<T,S>> C.uiConverterElement(converter: (T) -> String) = but(Constraint.UiElementConverter(converter)) // TODO: remove

/** @return action created from this function into the specified configurable or throws if this method is not an action */
fun KFunction<*>.getDelegateAction(within: Configurable<*>): Action = within.getConfig(findAnnotation<IsAction>()!!.name).asIs()

/** @return config created from this property or throws if this property is not delegated configurable property */
fun KProperty0<*>.getDelegateConfig(): Config<*> {
   isAccessible = true
   return getDelegate().asIs()
}

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

abstract class Conf<T: Any?>: ConstrainedDsl<T> {
   protected var refSubstitute: Any? = null
   val constraints = HashSet<Constraint<T>>()
   var def: ConfigDefinition? = null

   protected fun KProperty<*>.obtainConfigMetadata() = null
      ?: def
      ?: findAnnotation<IsConfig>()?.toDef()
      ?: ConfigDef(name)

   protected fun validateValue(v: T) = constraints.forEach { it.validate(v).ifError { failIf(true) { "Value $v doesn't conform to: $it" } } }

   protected fun validateValueSoft(v: T): Try<*,*> = if (constraints.any { it.validate(v).isError }) Try.error() else Try.ok()

   protected fun KCallable<*>.makeAccessible() = apply {
      isAccessible = true
      if (this is KProperty<*>) javaField?.isAccessible = true
      if (this is KFunction<*>) javaMethod?.isAccessible = true
   }

   protected fun <CFGT, CFG: Config<CFGT>> CFG.registerConfig(ref: ConfigDelegator) = apply {
      ref.configurableValueSource.register(this)
   }

   override fun addConstraint(constraint: Constraint<T>): Conf<T> = apply { constraints += constraint }

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

      val c = ValueConfig(type(), name, name, ActionDb(isGlobal, keys), group, desc, EditMode.USER)
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
      validateValue(c.value)
      ref.configurableValueSource.initialize(c)
      validateValueSoft(c.value).ifError { c.setValueToDefault() }
      // TODO: validate elements

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
      validateValue(c.value)
      ref.configurableValueSource.initialize(c)
      validateValueSoft(c.value).ifError { c.setValueToDefault() }

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
      validateValue(c.value)
      ref.configurableValueSource.initialize(c)
      validateValueSoft(c.value).ifError { c.setValueToDefault() }

      return object: PropertyConfig<T>(type, property.name, info, constraints, vx(c.value), initialValue, group), RwProperty<ConfigDelegator, T> {
         @Suppress("UsePropertyAccessSyntax")
         override fun getValue(thisRef: ConfigDelegator, property: KProperty<*>) = getValue()
         override fun setValue(thisRef: ConfigDelegator, property: KProperty<*>, value: T) = setValue(value)
      }.registerConfig(ref)
   }
}

class ConfV<T: Any?, W: WritableValue<T>>: Conf<T>, ConfigPropertyDelegator<ConfigDelegator, RoProperty<ConfigDelegator, W>> {
   private val initialValue: T
   private var vFactory: (T) -> W

   constructor(initialValue: T, valueSupplier: (T) -> W): super() {
      this.initialValue = initialValue
      this.vFactory = valueSupplier
   }

   /** Invokes [attach] with the specified block on the observable value that will be created and returns this. */
   infix fun attach(block: (T) -> Unit) = apply {
      val s = vFactory
      vFactory = { s(it).apply { asIf<ObservableValue<T>>()?.attach(block) } }
   }

   /** Invokes [sync] with the specified block on the observable value that will be created and returns this. */
   infix fun sync(block: (T) -> Unit) = apply {
      val s = vFactory
      vFactory = { s(it).apply { asIf<ObservableValue<T>>()?.sync(block) } }
   }

   override operator fun provideDelegate(ref: ConfigDelegator, property: KProperty<*>): RoProperty<ConfigDelegator, W> {
      property.makeAccessible()
      val info = property.obtainConfigMetadata()
      val type = VType<T>(property.returnType.argOf(WritableValue::class, 0).typeOrNothing)
      val group = info.computeConfigGroup(ref)

      val isFinal = property !is KMutableProperty
      failIf(!isFinal) { "Property must be immutable" }

      val c = ValueConfig(type, property.name, "", initialValue, group, "", info.editable).addConstraints(constraints)
      validateValue(c.value)
      ref.configurableValueSource.initialize(c)
      validateValueSoft(c.value).ifError { c.setValueToDefault() }

      return object: PropertyConfig<T>(type, property.name, info, constraints, vFactory(c.value), initialValue, group), RoProperty<ConfigDelegator, W> {
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
      val type = VType<T>(property.returnType.argOf(ObservableValue::class, 0).typeOrNothing)
      val group = info.computeConfigGroup(ref)

      val isFinal = property !is KMutableProperty
      failIf(!isFinal) { "Property must be immutable" }
      failIf(info.editable == EditMode.USER) { "Property read-only status forbids usage of ${EditMode.USER}" }

      val c = ValueConfig(type, property.name, "", initialValue, group, "", info.editable).addConstraints(constraints)
      validateValue(c.value)

      return object: PropertyConfigRO<T>(type, property.name, info, constraints, v(c.value), group), RoProperty<ConfigDelegator, W> {
         override fun getValue(thisRef: ConfigDelegator, property: KProperty<*>): W = this.property as W
      }.registerConfig(ref)
   }

}

class ConfVOr<T: Any?, W: OrV<T>>: Conf<OrValue<T>>, ConfigPropertyDelegator<ConfigDelegator, RoProperty<ConfigDelegator, W>> {
   private var vFactory: () -> W
   val elementConstraints = HashSet<Constraint<T>>()

   constructor(valueSupplier: () -> W): super() {
      this.vFactory = valueSupplier
   }

   /** Invokes [attach] with the specified block on the observable value that will be created and returns this. */
   infix fun attach(block: (T) -> Unit) = apply {
      val s = vFactory
      vFactory = { s().apply { asIf<ObservableValue<T>>()?.attach(block) } }
   }

   /** Invokes [sync] with the specified block on the observable value that will be created and returns this. */
   infix fun sync(block: (T) -> Unit) = apply {
      val s = vFactory
      vFactory = { s().apply { asIf<ObservableValue<T>>()?.sync(block) } }
   }

   override operator fun provideDelegate(ref: ConfigDelegator, property: KProperty<*>): RoProperty<ConfigDelegator, W> {
      property.makeAccessible()
      val info = property.obtainConfigMetadata()
      val type = VType<T>(property.returnType.argOf(WritableValue::class, 0).typeOrNothing)
      val group = info.computeConfigGroup(ref)

      val isFinal = property !is KMutableProperty
      failIf(!isFinal) { "Property must be immutable" }

      return object: OrPropertyConfig<T>(type, property.name, info, constraints, elementConstraints, vFactory(), group), RoProperty<ConfigDelegator, W> {
         init {
            // TODO: validate elements
            validateValue(this.value)
            ref.configurableValueSource.initialize(this)
            validateValueSoft(this.value).ifError { this.setValueToDefault() }
         }
         @Suppress("UNCHECKED_CAST")
         override fun getValue(thisRef: ConfigDelegator, property: KProperty<*>): W = this.property as W
      }.registerConfig(ref)
   }

}