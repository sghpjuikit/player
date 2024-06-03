@file:Suppress("FINAL_UPPER_BOUND")

package sp.it.util.conf

import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import sp.it.util.conf.Constraint.FileActor
import sp.it.util.conf.Constraint.FileOut
import sp.it.util.conf.Constraint.FileRelative
import sp.it.util.conf.Constraint.Multiline
import sp.it.util.conf.Constraint.MultilineRows
import sp.it.util.conf.Constraint.MultilineScrollToBottom
import sp.it.util.conf.Constraint.NoPersist
import sp.it.util.conf.Constraint.NoUi
import sp.it.util.conf.Constraint.NoUiDefaultButton
import sp.it.util.conf.Constraint.NumberMinMax
import sp.it.util.conf.Constraint.BigDecimalMinMax
import sp.it.util.conf.Constraint.BigIntegerMinMax
import sp.it.util.conf.Constraint.ObjectNonNull
import sp.it.util.conf.Constraint.Password
import sp.it.util.conf.Constraint.PreserveOrder
import sp.it.util.conf.Constraint.ReadOnlyIf
import sp.it.util.conf.Constraint.StringLength
import sp.it.util.conf.Constraint.StringNonBlank
import sp.it.util.conf.Constraint.StringNonEmpty
import sp.it.util.conf.Constraint.UiConverter
import sp.it.util.conf.Constraint.UiGeneral
import sp.it.util.conf.Constraint.UiInfoConverter
import sp.it.util.conf.Constraint.UiNoCustomUnsealedValue
import sp.it.util.conf.Constraint.UiPaginated
import sp.it.util.conf.Constraint.UiSingleton
import sp.it.util.conf.Constraint.ValueSealedRadio
import sp.it.util.conf.Constraint.ValueSealedSet
import sp.it.util.conf.Constraint.ValueSealedToggle
import sp.it.util.conf.Constraint.ValueUnsealedSet
import sp.it.util.file.FileType
import sp.it.util.functional.toUnit
import sp.it.util.type.InstanceMap

interface Constrained<T, THIS: Any> {
   fun addConstraint(constraint: Constraint<T>): THIS
   fun constrain(block: ConstrainedDsl<T>.() -> Unit): THIS {
      ConstrainedDsl { constraint -> addConstraint(constraint).toUnit() }.block()
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
/** Adds [Multiline] and [MultilineRows] */
fun <T: Any?, C: ConstrainedDsl<T>> C.multiline(rows: Int? = null) = but(Multiline).but(MultilineRows(rows))
/** Adds [Password] */
fun <T: Any?, C: ConstrainedDsl<T>> C.password() = but(Password)
/** Adds [Multiline] and [MultilineRows] and [MultilineScrollToBottom] */
fun <T: Any?, C: ConstrainedDsl<T>> C.multilineToBottom(rows: Int? = null) = but(Multiline).but(MultilineRows(rows)).but(MultilineScrollToBottom)
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
fun <T: String?, TN: T?, C: ConstrainedDsl<TN>> C.nonEmpty() = but(StringNonEmpty())
/** Adds [StringNonBlank] */
fun <T: String?, TN: T?, C: ConstrainedDsl<TN>> C.nonBlank() = but(StringNonBlank())
/** Adds [StringLength] with min filled */
fun <T: String?, TN: T?, C: ConstrainedDsl<TN>> C.lengthMin(min: Int) = but(StringLength(min, Integer.MAX_VALUE))
/** Adds [StringLength] with max filled */
fun <T: String?, TN: T?, C: ConstrainedDsl<TN>> C.lengthMax(max: Int) = but(StringLength(0, max))
/** Adds [StringLength] with min and max filled */
fun <T: String?, TN: T?, C: ConstrainedDsl<TN>> C.lengthBetween(min: Int, max: Int) = but(StringLength(min, max))
/** Adds [NumberMinMax] with min filled */
fun <T: Number, TN: T?, C: ConstrainedDsl<TN>> C.min(min: T) = but(NumberMinMax.Min(min.toDouble()))
/** Adds [NumberMinMax] with max filled */
fun <T: Number, TN: T?, C: ConstrainedDsl<TN>> C.max(max: T) = but(NumberMinMax.Max(max.toDouble()))
/** Adds [NumberMinMax] with min and max filled */
fun <T: Number, TN: T?, C: ConstrainedDsl<TN>> C.between(min: T, max: T) = but(NumberMinMax.Between(min.toDouble(), max.toDouble()))
/** Adds [BigIntegerMinMax] with min filled */
fun <T: BigInteger, TN: T?, C: ConstrainedDsl<TN>> C.min(min: T) = but(BigIntegerMinMax.Min(min))
/** Adds [BigIntegerMinMax] with max filled */
fun <T: BigInteger, TN: T?, C: ConstrainedDsl<TN>> C.max(max: T) = but(BigIntegerMinMax.Max(max))
/** Adds [BigIntegerMinMax] with min and max filled */
fun <T: BigInteger, TN: T?, C: ConstrainedDsl<TN>> C.between(min: T, max: T) = but(BigIntegerMinMax.Between(min, max))
/** Adds [BigDecimalMinMax] with min filled */
fun <T: BigDecimal, TN: T?, C: ConstrainedDsl<TN>> C.min(min: T) = but(BigDecimalMinMax.Min(min))
/** Adds [BigDecimalMinMax] with max filled */
fun <T: BigDecimal, TN: T?, C: ConstrainedDsl<TN>> C.max(max: T) = but(BigDecimalMinMax.Max(max))
/** Adds [BigDecimalMinMax] with min and max filled */
fun <T: BigDecimal, TN: T?, C: ConstrainedDsl<TN>> C.between(min: T, max: T) = but(BigDecimalMinMax.Between(min, max))
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
/** Adds [PreserveOrder] */
fun <T: Any?, C: ConstrainedDsl<T>> C.uiNoOrder() = but(PreserveOrder)
/** Adds [UiNoCustomUnsealedValue] */
fun <T: Any?, C: ConstrainedDsl<T>> C.uiNoCustomUnsealedValue() = but(UiNoCustomUnsealedValue)
/** Adds [Constraint.UiPaginated] */
fun                <T: Any?, C: ConstrainedDsl<ObservableList<T>>> C.uiPaginated(value: Boolean) = but(UiPaginated(value))
/** Adds [Constraint.UiGeneral] */
fun                <T: Any?, C: ConstrainedDsl<T>> C.uiGeneral() = but(UiGeneral)
/** Adds [Constraint.ValueSealedSet] using the specified enumerator */
fun                <T: Any?, C: ConstrainedDsl<T>> C.values(enumerator: () -> Collection<T>) = but(ValueSealedSet { enumerator() })
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
/** Adds [Constraint.ValueSealedToggle] */
fun                <T: Any?, C: ConstrainedDsl<T>> C.uiToggle() = but(ValueSealedToggle)
/** Adds [Constraint.ValueSealedRadio] */
fun                <T: Any?, C: ConstrainedDsl<T>> C.uiRadio() = but(ValueSealedRadio)
