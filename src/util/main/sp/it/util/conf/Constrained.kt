@file:Suppress("FINAL_UPPER_BOUND")

package sp.it.util.conf

import java.io.File
import javafx.beans.value.ObservableValue
import sp.it.util.conf.Constraint.*
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

/** Adds [NoUi] */
fun <T: Any?, C: ConstrainedDsl<T>> C.noUi() = but(NoUi)
/** Adds [Multiline] */
fun <T: Any?, C: ConstrainedDsl<T>> C.multiline() = but(Multiline)
/** Adds [NoUiDefaultButton] */
fun <T: Any?, C: ConstrainedDsl<T>> C.noUiDefaultButton() = but(NoUiDefaultButton)
/** Adds [NoPersist] */
fun <T: Any?, C: ConstrainedDsl<T>> C.noPersist() = but(NoPersist)
/** Adds [noUiDefaultButton] and [noPersist] */
fun <T: Any?, C: ConstrainedDsl<T>> C.singleton() = noUiDefaultButton().noPersist()
/** Adds [UiSingleton] */
fun <T: Any?, C: ConstrainedDsl<T>> C.uiSingleton() = but(UiSingleton)
/** Adds [UiConverter] with the specified converter */
fun <T: Any?, C: ConstrainedDsl<T>> C.uiConverter(converter: (T) -> String) = but(UiConverter(converter))
/** Adds [UiInfoConverter] with the specified converter */
fun <T: Any?, C: ConstrainedDsl<T>> C.uiInfoConverter(converter: (T) -> String) = but(UiInfoConverter(converter))
/** Adds [ObjectNonNull] */
fun <T: Any?, C: ConstrainedDsl<T>> C.nonNull() = but(ObjectNonNull)
/** Adds [StringNonEmpty] */
fun <T: String, TN: T?, C: ConstrainedDsl<TN>> C.nonEmpty() = but(StringNonEmpty())
/** Adds [StringNonBlank] */
fun <T: String, TN: T?, C: ConstrainedDsl<TN>> C.nonBlank() = but(StringNonBlank())
/** Adds [StringLength] with min filled */
fun <T: String, TN: T?, C: ConstrainedDsl<TN>> C.lengthMin(min: Int) = but(StringLength(min, Integer.MAX_VALUE))
/** Adds [StringLength] with max filled */
fun <T: String, TN: T?, C: ConstrainedDsl<TN>> C.lengthMax(max: Int) = but(StringLength(0, max))
/** Adds [StringLength] with min and max filled */
fun <T: String, TN: T?, C: ConstrainedDsl<TN>> C.lengthBetween(min: Int, max: Int) = but(StringLength(min, max))
/** Adds [NumberMinMax] with min filled */
fun <T: Number, TN: T?, C: ConstrainedDsl<TN>> C.min(min: T) = but(NumberMinMax(min.toDouble(), null))
/** Adds [NumberMinMax] with max filled */
fun <T: Number, TN: T?, C: ConstrainedDsl<TN>> C.max(max: T) = but(NumberMinMax(null, max.toDouble()))
/** Adds [NumberMinMax] with min and max filled */
fun <T: Number, TN: T?, C: ConstrainedDsl<TN>> C.between(min: T, max: T) = but(NumberMinMax(min.toDouble(), max.toDouble()))
/** Adds [FileOut] */
fun <T: File?, C: ConstrainedDsl<T>> C.uiOut() = but(FileOut)
/** Adds [FileActor] with specified type */
fun <T: File?, C: ConstrainedDsl<T>> C.only(type: FileType) = but(FileActor(type))
/** Adds [FileActor] with specified type */
fun <T: File?, C: ConstrainedDsl<T>> C.only(type: FileActor) = but(type)
/** Adds [FileRelative] with specified file */
fun <T: File?, C: ConstrainedDsl<T>> C.relativeTo(relativeTo: File) = but(FileRelative(relativeTo))
/** Adds [ReadOnlyIf] with true */
fun <T: Any?, C: ConstrainedDsl<T>> C.readOnly() = readOnlyIf(true)
/** Adds [ReadOnlyIf] with specified value */
fun <T: Any?, C: ConstrainedDsl<T>> C.readOnlyIf(condition: Boolean) = but(ReadOnlyIf(condition))
/** Adds [ReadOnlyIf] with specified observable value */
fun <T: Any?, C: ConstrainedDsl<T>> C.readOnlyIf(condition: ObservableValue<Boolean>) = but(ReadOnlyIf(condition, false))
/** Adds [ReadOnlyIf] with negated specified value */
fun <T: Any?, C: ConstrainedDsl<T>> C.readOnlyUnless(condition: Boolean) = but(ReadOnlyIf(!condition))
/** Adds [ReadOnlyIf] with negated specified observable value */
fun <T: Any?, C: ConstrainedDsl<T>> C.readOnlyUnless(condition: ObservableValue<Boolean>) = but(ReadOnlyIf(condition, true))
// TODO: remove
///** Hints ui editor to use the specified to-string converter for elements instead of the default one. */
//fun <T: Any?, S: Boolean?, C: ConfCheckL<T,S>> C.uiConverterElement(converter: (T) -> String) = but(Constraint.UiElementConverter(converter))
/** Adds [PreserveOrder] */
fun <T: Any?, C: ConstrainedDsl<T>> C.uiNoOrder() = but(PreserveOrder)
/** Adds [Constraint.ValueSealedSet] using the specified collection as enumerator */
fun                <T: Any?, C: ConstrainedDsl<T>> C.values(enumerator: Collection<T>) = but(ValueSealedSet { enumerator })
/** Adds [Constraint.ValueSealedSet] using the specified enumerator */
fun                <T: Any?, C: ConstrainedDsl<T>> C.valuesIn(enumerator: () -> Sequence<T>) = but(ValueSealedSet { enumerator().toList() })
/** Adds [Constraint.ValueSealedSet] using the instance map for type specified by the reified generic type argument as enumerator */
inline fun <reified T: Any?, C: ConstrainedDsl<T>> C.valuesIn(instanceSource: InstanceMap) = values(instanceSource.getInstances())
/** Adds [Constraint.ValueUnsealedSet] using the specified enumerator */
fun                <T: Any?, C: ConstrainedDsl<T>> C.valuesUnsealed(enumerator: () -> Collection<T>) = but(ValueUnsealedSet { enumerator().toList() })
/** Adds [Constraint.ValueUnsealedSet] using the specified collection as enumerator */
fun                <T: Any?, C: ConstrainedDsl<T>> C.valuesUnsealed(enumerator: Collection<T>) = valuesUnsealed { enumerator }
/** Adds [Constraint.ValueUnsealedSet] using the instance map for type specified by the reified generic type argument as enumerator */
inline fun <reified T: Any?, C: ConstrainedDsl<T>> C.valuesUnsealed(instanceSource: InstanceMap) = valuesUnsealed(instanceSource.getInstances())