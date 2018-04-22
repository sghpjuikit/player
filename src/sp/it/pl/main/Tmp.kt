package sp.it.pl.main

import javafx.beans.value.WritableValue
import javafx.collections.ObservableList
import org.reactfx.Subscription
import sp.it.pl.gui.pane.ActionPane
import sp.it.pl.layout.widget.controller.ClassController
import sp.it.pl.layout.widget.controller.FXMLController
import sp.it.pl.util.access.V
import sp.it.pl.util.access.VarEnum
import sp.it.pl.util.access.v
import sp.it.pl.util.action.Action
import sp.it.pl.util.action.IsAction
import sp.it.pl.util.async.future.Fut
import sp.it.pl.util.conf.Config
import sp.it.pl.util.conf.Config.PropertyConfig
import sp.it.pl.util.conf.Config.VarList
import sp.it.pl.util.conf.EditMode
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.MainConfiguration
import sp.it.pl.util.conf.ValueConfig
import sp.it.pl.util.conf.computeConfigGroup
import sp.it.pl.util.conf.obtainConfigConstraints
import sp.it.pl.util.dev.throwIf
import sp.it.pl.util.functional.ifNull
import sp.it.pl.util.type.Util.getGenericPropertyType
import sp.it.pl.util.validation.Constraint
import sp.it.pl.util.validation.Constraint.NumberMinMax
import java.io.File
import java.util.function.Consumer
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

fun futureWrap(data: Any?): Fut<*> = data as? Fut<*> ?: Fut.fut(data)!!

inline fun <reified T> ActionPane.register(vararg actions: ActionPane.ActionData<T, *>) = register(T::class.java, *actions)

infix fun FXMLController.onDispose(s: () -> Unit) = d({ s() })

infix fun FXMLController.initClose(s: () -> Subscription) = d(s())

fun <T: Any> ClassController.v(initialValue: T, onChange: (T) -> Unit) = VLate(initialValue, onChange)

class VLate<T>(private val initialValue: T, private val onChange: (T) -> Unit) {
    operator fun provideDelegate(ref: ClassController, property: KProperty<*>): ReadOnlyProperty<ClassController, V<T>> {
        val v = v(initialValue) { if (ref.isInitialized) onChange(it) }
        return object: ReadOnlyProperty<ClassController, V<T>> {
            override fun getValue(thisRef: ClassController, property: KProperty<*>): V<T> {
                return v
            }
        }
    }
}

fun <T> Config<T>.isEditableByUser() = isEditable.isByUser && constraints.asSequence().none { it is Constraint.ReadOnlyIf && it.condition()}

fun <T: Any> c(initialValue: T): ConfS<T> = ConfS(initialValue).but(Constraint.ObjectNonNull())
fun <T: Any?> cn(initialValue: T?): ConfS<T?> = ConfS(initialValue)
fun <T: Any> cv(initialValue: T): ConfV<T, V<T>> = ConfV<T,V<T>>(initialValue).but(Constraint.ObjectNonNull())
fun <T: Any, W: WritableValue<T>> cv(initialValue: T, valueSupplier: (T) -> W): ConfV<T, W> = ConfV(initialValue, valueSupplier).but(Constraint.ObjectNonNull())
fun <T: Any?> cvn(initialValue: T?): ConfV<T?, V<T?>> = ConfV(initialValue)
fun <T: Any?, W: WritableValue<T?>> cvn(initialValue: T?, valueSupplier: (T?) -> W): ConfV<T?, W> = ConfV(initialValue, valueSupplier)
fun <T: () -> Unit> cr(action: T): ConfR<T> = ConfR(action)
inline fun <reified T: Any> cList(): ConfL<T> = ConfL(T::class.java)

fun <T: Any?, C: Conf<T>> C.but(vararg restrictions: Constraint<T>) = apply { constraints += restrictions }
fun <T: Number, C: Conf<T>> C.min(min: T) = but(NumberMinMax(min.toDouble(), Double.MAX_VALUE))
fun <T: Number, C: Conf<T>> C.max(max: T) = but(NumberMinMax(Double.MIN_VALUE, max.toDouble()))
fun <T: Number, C: Conf<T>> C.between(min: T, max: T) = but(NumberMinMax(min.toDouble(), max.toDouble()))
fun <T: File, C: Conf<T>> C.only(type: Constraint.FileActor) = but(type)
fun <T: Any, C: Conf<T>> C.readOnlyIf(condition: () -> Boolean) = but(Constraint.ReadOnlyIf(condition))
fun <T: Any, C: Conf<T>> C.readOnlyUnless(condition: () -> Boolean) = but(Constraint.ReadOnlyIf { !condition() })
fun <T: Any, W: VarEnum<T>> ConfV<T,W>.preserveOrder() = but(Constraint.PreserveOrder())

/** Singleton configuration used by delegated configurable properties. */
private val configuration = MainConfiguration

/** Contract to specify per-instance configurable group (discriminant) for delegated config properties. */
interface MultiConfigurable {
    val configurableDiscriminant: String
    val configurableGroup get() = computeConfigGroup(this)
}

/** Simple non-constant implementation of [MultiConfigurable] wit the discriminant supplied at creation time. */
open class MultiConfigurableBase(override val configurableDiscriminant: String): MultiConfigurable

interface Delegator<REF: Any?, D: Any> {
    operator fun provideDelegate(ref: REF, property: KProperty<*>): D
}

fun <REF: Any, DP: Any> Delegator<REF,DP>.forInstance(instance: REF): Delegator<Nothing?, DP> = object: Delegator<Nothing?,DP> {
    override fun provideDelegate(ref: Nothing?, property: KProperty<*>): DP = this@forInstance.provideDelegate(instance, property)
}

abstract class Conf<T: Any?> {
    protected var refSubstitute: Any? = null
    val constraints = HashSet<Constraint<T>>()

    protected fun addAnnotationConstraints(type: Class<T>, property: KProperty<*>) {
        constraints += obtainConfigConstraints(type, property.annotations)
    }
    protected fun validateValue(v: T) {
        constraints.forEach { it.validate(v).ifError { throwIf(true, it) } }
    }

    protected fun KProperty<*>.makeAccessible() = apply {
        isAccessible = true
        javaField?.isAccessible = true
    }

    protected fun <CFGT, CFG: Config<CFGT>> CFG.registerConfig() = apply {
        configuration.collect(this)
    }
}

class ConfR<T: () -> Unit>(private val action: T) : Conf<T>() {
    operator fun provideDelegate(ref: Any, property: KProperty<*>): ReadOnlyProperty<Any, T> {
        property.makeAccessible()
        val info = property.obtainConfigMetadata()
        val infoExt = property.findAnnotation<IsAction>()
        val group = info.computeConfigGroup(ref)

        val isFinal = property !is KMutableProperty
        throwIf(!isFinal, "Property must be immutable")

        val name = info.name.takeIf { it.isNotBlank() } ?: property.name
        val keys = infoExt?.keys ?: ""
        val isGlobal = infoExt?.global ?: false
        val isContinuous = infoExt?.repeat ?: false

        return object: Action(name, Runnable { action() }, info.info, group, keys, isGlobal, isContinuous), ReadOnlyProperty<Any, T> {
            override fun getValue(thisRef: Any, property: KProperty<*>) = action
        }.registerConfig()
    }
}

class ConfL<T: Any?>(val type: Class<T>): Conf<T>() {
    operator fun provideDelegate(ref: Any, property: KProperty<*>): ReadOnlyProperty<Any, ObservableList<T>> {
        property.makeAccessible()
        val info = property.obtainConfigMetadata()
        val group = info.computeConfigGroup(ref)
        addAnnotationConstraints(type, property)

        val isFinal = property !is KMutableProperty
        throwIf(!isFinal, "Property must be immutable")

        val list = VarList<T>(type, VarList.Elements.NOT_NULL)
        val c = Config.ListConfig(property.name, info, list, group, constraints)
        configuration.rawSet(c)

        return object: Config.ListConfig<T>(property.name, info, list, group, constraints), ReadOnlyProperty<Any, ObservableList<T>> {
            override fun getValue(thisRef: Any, property: KProperty<*>) = list.list
        }.registerConfig()
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
        throwIf(isFinal xor (info.editable===EditMode.NONE), "Property mutability does not correspond to specified editability=${info.editable}")

        val c = ValueConfig(type, property.name, "", initialValue, group,"", info.editable, Consumer {}).constraints(constraints)
        configuration.rawSet(c)
        validateValue(c.value)

        return object: PropertyConfig<T>(type, property.name, info, constraints, V<T>(c.value), initialValue, group), ReadOnlyProperty<Any, T>, ReadWriteProperty<Any, T> {
            override fun getValue(thisRef: Any, property: KProperty<*>) = getValue()
            override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
                setValue(value)
            }
        }.registerConfig()
    }
}

@Suppress("UNCHECKED_CAST")
class ConfV<T: Any?, W: WritableValue<T>>: Conf<T>, Delegator<Any, ReadOnlyProperty<Any?, W>> {
    private val initialValue: T
    private val v: (T) -> WritableValue<T>

    constructor(initialValue: T): super() {
        this.initialValue = initialValue
        this.v = { V(it) }
    }
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
        throwIf(!isFinal, "Property must be immutable")

        val c = ValueConfig(type, property.name, "", initialValue, group,"", info.editable, Consumer {}).constraints(constraints)
        configuration.rawSet(c)
        validateValue(c.value)

        return object: PropertyConfig<T>(type, property.name, info, constraints, v(c.value), initialValue, group), ReadOnlyProperty<Any?, W> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): W = this.property as W
        }.registerConfig()
    }

}


private fun KProperty<*>.obtainConfigMetadata() = findAnnotation<IsConfig>().ifNull { throwIf(true, "$this is missing metadata, please provide @IsConfig annotation") }!!