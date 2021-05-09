@file:Suppress("FINAL_UPPER_BOUND")

package sp.it.util.conf

import java.io.File
import javafx.beans.value.ObservableValue
import sp.it.util.file.FileType
import sp.it.util.functional.toUnit
import sp.it.util.type.InstanceMap

interface Constrained<T, THIS: Any> {
   fun addConstraint(constraint: Constraint<T>): THIS
   fun constrain(block: ConstrainedDsl<T>.() -> Unit): THIS {
      ConstrainedDsl<T> { constraint -> addConstraint(constraint).toUnit() }.block()
      @Suppress("UNCHECKED_CAST") return this as THIS
   }
}

fun interface ConstrainedDsl<T> {
   fun addConstraint(constraint: Constraint<T>): Any?
}

fun <T: Any?, C: ConstrainedDsl<T>> C.but(constraint: Constraint<T>) = apply { addConstraint(constraint) }
fun <T: Any?, C: ConstrainedDsl<T>> C.but(vararg constraints: Constraint<T>) = apply { constraints.forEach { addConstraint(it) } }
fun <T: Any?, C: ConstrainedDsl<T>> C.but(constraints: Collection<Constraint<T>>) = apply { constraints.forEach { addConstraint(it) } }
fun <T: Any?, C: ConstrainedDsl<T>> C.but(constraints: Sequence<Constraint<T>>) = apply { constraints.forEach { addConstraint(it) } }

fun <T: Any?, C: ConstrainedDsl<T>> C.noUi() = but(Constraint.NoUi)
fun <T: Any?, C: ConstrainedDsl<T>> C.noUiDefaultButton() = but(Constraint.NoUiDefaultButton)
fun <T: Any?, C: ConstrainedDsl<T>> C.noPersist() = but(Constraint.NoPersist)
fun <T: Any?, C: ConstrainedDsl<T>> C.singleton() = noUiDefaultButton().noPersist()
fun <T: Any?, C: ConstrainedDsl<T>> C.nonNull() = but(Constraint.ObjectNonNull)
fun <T: String, TN: T?, C: ConstrainedDsl<TN>> C.nonEmpty() = but(Constraint.StringNonEmpty())
fun <T: String, TN: T?, C: ConstrainedDsl<TN>> C.nonBlank() = but(Constraint.StringNonBlank())
fun <T: Number, TN: T?, C: ConstrainedDsl<TN>> C.min(min: T) = but(Constraint.NumberMinMax(min.toDouble(), null))
fun <T: Number, TN: T?, C: ConstrainedDsl<TN>> C.max(max: T) = but(Constraint.NumberMinMax(null, max.toDouble()))
fun <T: Number, TN: T?, C: ConstrainedDsl<TN>> C.between(min: T, max: T) = but(Constraint.NumberMinMax(min.toDouble(), max.toDouble()))
fun <T: File?, C: ConstrainedDsl<T>> C.uiOut() = but(Constraint.FileOut)
fun <T: File?, C: ConstrainedDsl<T>> C.only(type: FileType) = but(Constraint.FileActor(type))
fun <T: File?, C: ConstrainedDsl<T>> C.only(type: Constraint.FileActor) = but(type)
fun <T: File?, C: ConstrainedDsl<T>> C.relativeTo(relativeTo: File) = but(Constraint.FileRelative(relativeTo))
fun <T: Any?, C: ConstrainedDsl<T>> C.readOnly() = readOnlyIf(true)
fun <T: Any?, C: ConstrainedDsl<T>> C.readOnlyIf(condition: Boolean) = but(Constraint.ReadOnlyIf(condition))
fun <T: Any?, C: ConstrainedDsl<T>> C.readOnlyIf(condition: ObservableValue<Boolean>) = but(Constraint.ReadOnlyIf(condition, false))
fun <T: Any?, C: ConstrainedDsl<T>> C.readOnlyUnless(condition: Boolean) = but(Constraint.ReadOnlyIf(!condition))
fun <T: Any?, C: ConstrainedDsl<T>> C.readOnlyUnless(condition: ObservableValue<Boolean>) = but(Constraint.ReadOnlyIf(condition, true))

/** Hints ui editor to use the specified to-string converter instead of the default one. */
fun <T: Any?, C: ConstrainedDsl<T>> C.uiConverter(converter: (T) -> String) = but(Constraint.UiConverter(converter))

///** Hints ui editor to use the specified to-string converter for elements instead of the default one. */
//fun <T: Any?, S: Boolean?, C: ConfCheckL<T,S>> C.uiConverterElement(converter: (T) -> String) = but(Constraint.UiElementConverter(converter))

/** Hints ui editor to use the specified to-info-string converter to display extended information about the value. */
fun <T: Any?, C: ConstrainedDsl<T>> C.uiInfoConverter(converter: (T) -> String) = but(Constraint.UiInfoConverter(converter))

/** Hints ui editor for [Config.isEnumerable] to use original order of the enumeration, i.e. no sort will be applied. */
fun <T: Any?, C: ConstrainedDsl<T>> C.uiNoOrder() = but(Constraint.PreserveOrder)

/** [Constraint.ValueSealedSet] using the specified collection as enumerator */
fun                <T: Any?, C: ConstrainedDsl<T>> C.values(enumerator: Collection<T>) = but(Constraint.ValueSealedSet { enumerator })

/** [Constraint.ValueSealedSet] using the specified enumerator */
fun                <T: Any?, C: ConstrainedDsl<T>> C.valuesIn(enumerator: () -> Sequence<T>) = but(Constraint.ValueSealedSet { enumerator().toList() })

/** [Constraint.ValueSealedSet] using the instance map for type specified by the reified generic type argument as enumerator */
inline fun <reified T: Any?, C: ConstrainedDsl<T>> C.valuesIn(instanceSource: InstanceMap) = values(instanceSource.getInstances())

/** [Constraint.ValueUnsealedSet] using the specified enumerator */
fun                <T: Any?, C: ConstrainedDsl<T>> C.valuesUnsealed(enumerator: () -> Collection<T>) = but(Constraint.ValueUnsealedSet { enumerator().toList() })

/** [Constraint.ValueUnsealedSet] using the specified collection as enumerator */
fun                <T: Any?, C: ConstrainedDsl<T>> C.valuesUnsealed(enumerator: Collection<T>) = valuesUnsealed { enumerator }

/** [Constraint.ValueUnsealedSet] using the instance map for type specified by the reified generic type argument as enumerator */
inline fun <reified T: Any?, C: ConstrainedDsl<T>> C.valuesUnsealed(instanceSource: InstanceMap) = valuesUnsealed(instanceSource.getInstances())