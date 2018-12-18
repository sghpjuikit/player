package sp.it.pl.util.conf

import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.ObservableList
import sp.it.pl.util.access.V
import sp.it.pl.util.access.VarEnum
import sp.it.pl.util.access.v
import sp.it.pl.util.access.vn
import sp.it.pl.util.action.Action
import sp.it.pl.util.action.IsAction
import sp.it.pl.util.dev.noNull
import sp.it.pl.util.dev.throwIf
import sp.it.pl.util.type.Util.getGenericPropertyType
import sp.it.pl.util.validation.Constraint
import java.io.File
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

fun <T: Any> c(initialValue: T): ConfS<T> = ConfS(initialValue).but(Constraint.ObjectNonNull())
fun <T: Any?> cn(initialValue: T?): ConfS<T?> = ConfS(initialValue)
fun <T: Any> cv(initialValue: T): ConfV<T, V<T>> = ConfV<T, V<T>>(initialValue, { v(it) }).but(Constraint.ObjectNonNull())
fun <T: Any, W: WritableValue<T>> cv(initialValue: T, valueSupplier: (T) -> W): ConfV<T, W> = ConfV(initialValue, valueSupplier).but(Constraint.ObjectNonNull())
fun <T: Any, W: ObservableValue<T>> cvro(initialValue: T, valueSupplier: (T) -> W): ConfVRO<T, W> = ConfVRO(initialValue, valueSupplier).but(Constraint.ObjectNonNull())
fun <T: Any?> cvn(initialValue: T?): ConfV<T?, V<T?>> = ConfV(initialValue, { vn(it) })
fun <T: Any?, W: WritableValue<T?>> cvn(initialValue: T?, valueSupplier: (T?) -> W): ConfV<T?, W> = ConfV(initialValue, valueSupplier)
fun <T: Any?, W: ObservableValue<T?>> cvnro(initialValue: T?, valueSupplier: (T?) -> W): ConfVRO<T?, W> = ConfVRO(initialValue, valueSupplier)
fun <T: () -> Unit> cr(action: T): ConfR<T> = ConfR(action)
inline fun <reified T: Any> cList(): ConfL<T> = ConfL(T::class.java)

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
fun <T: Any, W: VarEnum<T>> ConfV<T, W>.preserveOrder() = but(Constraint.PreserveOrder())

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
    val configurableGroup get() = computeConfigGroup(this)
    /** Config register and value provider. By default common value store. */
    @JvmDefault
    val configurableValueStore: ConfigValueSource get() = configuration
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

    protected fun KProperty<*>.obtainConfigMetadata() = findAnnotation<IsConfig>().noNull { "${IsConfig::class} annotation required for $this" }

    protected fun validateValue(v: T) {
        constraints.forEach { it.validate(v).ifError { throwIf(true) { "Value $v doesn't conform to: $it" } } }
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
        throwIf(!isFinal) { "Property must be immutable" }

        fun String.orNull() = takeIf { it.isNotBlank() }
        val name = infoExt?.name?.orNull() ?:  info?.name?.orNull() ?: property.name
        val desc = infoExt?.desc?.orNull() ?:  info?.info?.orNull()
        val keys = infoExt?.keys ?: ""
        val isGlobal = infoExt?.global ?: false
        val isContinuous = infoExt?.repeat ?: false

        return object: Action(name, Runnable { action() }, desc, group, keys, isGlobal, isContinuous), ReadOnlyProperty<Any, T> {
            override fun getValue(thisRef: Any, property: KProperty<*>) = action
        }.registerConfig(ref)
    }
}

class ConfL<T: Any?>(val type: Class<T>): Conf<T>() {
    operator fun provideDelegate(ref: Any, property: KProperty<*>): ReadOnlyProperty<Any, ObservableList<T>> {
        property.makeAccessible()
        val info = property.obtainConfigMetadata()
        val group = info.computeConfigGroup(ref)
        addAnnotationConstraints(type, property)

        val isFinal = property !is KMutableProperty
        throwIf(!isFinal) { "Property must be immutable" }

        val list = Config.VarList<T>(type, Config.VarList.Elements.NOT_NULL)
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
        throwIf(isFinal xor (info.editable===EditMode.NONE)) { "Property mutability does not correspond to specified editability=${info.editable}" }

        val c = ValueConfig(type, property.name, "", initialValue, group, "", info.editable, {}).constraints(constraints)
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
    private val v: (T) -> W

    constructor(initialValue: T, valueSupplier: (T) -> W): super() {
        this.initialValue = initialValue
        this.v = valueSupplier
    }

    override operator fun provideDelegate(ref: Any, property: KProperty<*>): ReadOnlyProperty<Any?, W> {
        property.makeAccessible()
        val info = property.obtainConfigMetadata()
        val type = getGenericPropertyType(property.returnType.javaType) as Class<T>
        val group = info.computeConfigGroup(ref)
        addAnnotationConstraints(type, property)

        val isFinal = property !is KMutableProperty
        throwIf(!isFinal) { "Property must be immutable" }

        val c = ValueConfig(type, property.name, "", initialValue, group, "", info.editable, {}).constraints(constraints)
        obtainConfigValueStore(ref).initialize(c)
        validateValue(c.value)

        return object: Config.PropertyConfig<T>(type, property.name, info, constraints, v(c.value) as WritableValue<T>, initialValue, group), ReadOnlyProperty<Any?, W> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): W = this.property as W
        }.registerConfig(ref)
    }

}

@Suppress("UNCHECKED_CAST")
class ConfVRO<T: Any?, W: ObservableValue<T>>: Conf<T>, Delegator<Any, ReadOnlyProperty<Any?, W>> {
    private val initialValue: T
    private val v: (T) -> W

    constructor(initialValue: T, valueSupplier: (T) -> W): super() {
        this.initialValue = initialValue
        this.v = valueSupplier
    }

    override operator fun provideDelegate(ref: Any, property: KProperty<*>): ReadOnlyProperty<Any?, W> {
        property.makeAccessible()
        val info = property.obtainConfigMetadata()
        val type = getGenericPropertyType(property.returnType.javaType) as Class<T>
        val group = info.computeConfigGroup(ref)
        addAnnotationConstraints(type, property)

        val isFinal = property !is KMutableProperty
        throwIf(!isFinal) { "Property must be immutable" }
        throwIf(info.editable!==EditMode.NONE) { "Property mutability requires usage of ${EditMode.NONE}" }

        val c = ValueConfig(type, property.name, "", initialValue, group, "", info.editable, {}).constraints(constraints)
        validateValue(c.value)

        return object: Config.ReadOnlyPropertyConfig<T>(type, property.name, info, constraints, v(c.value), group), ReadOnlyProperty<Any?, W> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): W = this.property as W
        }.registerConfig(ref)
    }

}