@file:Suppress("unused")

package sp.it.util.conf

import de.jensd.fx.glyphs.GlyphIcons
import java.io.File
import javafx.beans.binding.BooleanBinding
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections.singletonObservableList
import javafx.util.Duration
import sp.it.util.access.vAlways
import sp.it.util.conf.ConfigurationContext.toUiConverter
import sp.it.util.conf.Constraint.ValueSealedSetIfNotIn.Strategy
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
import sp.it.util.dev.failIfNot
import sp.it.util.file.FileType
import sp.it.util.functional.Try

/** Constrains or validates a value. Use [ConstraintSimple] for simple usage. */
interface Constraint<in T> {

   fun isValid(value: T?): Boolean

   fun message(): String

   fun validate(value: T?): Try<Nothing?, String> = if (isValid(value)) Try.ok() else Try.error(message())


   /** Denotes type of [java.io.File]. For example to decide between file and directory chooser. */
   enum class FileActor constructor(private val condition: (File) -> Boolean, private val message: String): Constraint<File?> {
      FILE({ !it.exists() || it.isFile }, "File must not be directory"),
      DIRECTORY({ !it.exists() || it.isDirectory }, "File must be directory"),
      ANY({ true }, "");

      override fun isValid(value: File?) = value==null || condition(value)
      override fun message() = message

      companion object {
         /** @return constraint to the specified type or [ANY] if null */
         operator fun invoke(type: FileType?): FileActor = when (type) {
            null -> ANY
            FileType.FILE -> FILE
            FileType.DIRECTORY -> DIRECTORY
         }
      }
   }

   class FileRelative(val to: File): MarkerConstraint()

   // TODO: support BigDecimal & BigInteger
   class NumberMinMax(val min: Double?, val max: Double?): Constraint<Number?> {

      init {
         failIf(min==null && max==null) { "Min and max can not both be null" }
         failIf(min!=null && max!=null && max<min) { "Max value must be greater than or equal to min value" }
         failIf(min!=null && min.isNaN()) { "Min can not be NaN" }
         failIf(max!=null && max.isNaN()) { "Max can not be NaN" }
      }

      fun isClosed() = min!=null && max!=null

      override fun isValid(value: Number?) = value==null || ((min==null || value.toDouble()>=min) && (max==null || value.toDouble()<=max))
      override fun message() = when {
         isClosed() -> "Number must be in range $min - $max"
         min!=null -> "Number must be at least ${toUiConverter.toS(min)}"
         max!=null -> "Number must be at most ${toUiConverter.toS(max)}"
         else -> fail()
      }
   }

   class StringNonEmpty: Constraint<String?> {
      override fun isValid(value: String?) = value==null || value.isNotEmpty()
      override fun message() = "String must not be empty"
   }

   class StringNonBlank: Constraint<String?> {
      override fun isValid(value: String?) = value==null || value.isNotBlank()
      override fun message() = "String must not contain only whitespace characters"
   }

   class StringLength(val min: Int, val max: Int): Constraint<String?> {
      init {
         failIfNot(max>min) { "Max value must be greater than min value" }
      }
      override fun isValid(value: String?) = value==null || value.length in min..max
      override fun message() = "Text must be at least ${toUiConverter.toS(min)} and at most ${toUiConverter.toS(max)} characters long"
   }

   /** Use multi-line text area instead of text field as editor. Allowed for non-[String] values. [Collection] and [Map] is multiline by default. */
   object Multiline: MarkerConstraint()

   object DurationNonNegative: Constraint<Duration> {
      override fun isValid(value: Duration?): Boolean {
         return value==null || value.greaterThanOrEqualTo(Duration.ZERO)
      }

      override fun message() = "Duration can not be negative"
   }

   abstract class MarkerConstraint: Constraint<Any?> {
      override fun isValid(value: Any?) = true
      override fun message() = ""
   }

   object HasNonNullElements: Constraint<Collection<*>> {
      override fun isValid(value: Collection<*>?) = value==null || value.all { it!=null }
      override fun message() = "No item may be ${toUiConverter.toS(null)}"
   }

   object ObjectNonNull: Constraint<Any?> {
      override fun isValid(value: Any?) = value!=null
      override fun message() = "Value must not be ${toUiConverter.toS(null)}"
   }

   class CollectionSize(val min: Int?, val max: Int?): Constraint<Collection<Any?>?> {
      init {
         failIf(min==null && max==null) { "Min and max can not both be null" }
         failIf(min!=null && max!=null && max<min) { "Max value must be greater than or equal to min value" }
      }
      override fun isValid(value: Collection<Any?>?) = value==null || ((min==null || min<=value.size) && (max==null || max>=value.size))
      override fun message() = "Collection size must be must be at least ${toUiConverter.toS(min)} and at most ${toUiConverter.toS(max)}"
   }

   /** Hints ui editor for [Config.isEnumerable] to use original order of the enumeration, i.e. no sort will be applied. */
   object PreserveOrder: MarkerConstraint()

   /** Avoid showing the config in ui. */
   object NoUi: MarkerConstraint()

   /** Avoid showing the set-to-default button for the config in ui. Use for 'computed' configs, like singletons. */
   object NoUiDefaultButton: MarkerConstraint()

   /** Avoid persisting the config. Use for 'computed' configs. Configs with [Config.isEditable]==[EditMode.NONE] are not persistent by default. */
   object NoPersist: MarkerConstraint()

   /** Use so editor span entire available space. Applied if the editor is the only config in its group. */
   object UiSingleton: MarkerConstraint()

   /** Use save file chooser in ui, allowing to define files that do not exist. */
   object FileOut: MarkerConstraint()

   /** Use single icon mode for boolean config editor and disabled style for false. */
   class IconConstraint(val icon: GlyphIcons): MarkerConstraint()

   /** Constrain value to those specified in the collection. May be mutable (see [ValueSealedSetIfNotIn], [SealedEnumerator]). */
   class ValueSealedSet<T>(private val enumerator: () -> Collection<T>): MarkerConstraint(), SealedEnumerator<T> {
      override fun enumerateSealed() = enumerator()
   }

   /** Hint for ui to use toggle button. Only affects [ValueSealedSet]. */
   object ValueSealedToggle: MarkerConstraint()

   /** Hint for ui to use radio button. Only affects [ValueSealedSet]. */
   object ValueSealedRadio: MarkerConstraint()

   /** Strategy for dealing with value outside specified set in [ValueSealedSet]. Default is [Strategy.USE_DEFAULT]. */
   class ValueSealedSetIfNotIn(val strategy: Strategy): MarkerConstraint() {
      enum class Strategy {
         USE_AND_ADD,
         USE,
         USE_DEFAULT
      }
   }

   /** Similar to [ValueSealedSet], but as a [UnsealedEnumerator], does not constrain value in any way. Basically autocompletion. */
   class ValueUnsealedSet<T>(private val enumerator: () -> Collection<T>): MarkerConstraint(), UnsealedEnumerator<T> {
      override fun enumerateUnsealed() = enumerator()
   }

   /** Hints ui editor to use the specified to-string converter instead of the default one. */
   class UiConverter<T>(val converter: (T) -> String): MarkerConstraint()

   class UiElementConverter<T>(val converter: (T) -> String): MarkerConstraint()

   /** Hints ui editor to use the specified to-info-string converter to display extended information about the value. */
   class UiInfoConverter<T>(val converter: (T) -> String): MarkerConstraint()

   class ReadOnlyIf(val condition: ObservableValue<Boolean>): Constraint<Any?> {
      constructor(condition: ObservableValue<Boolean>, unless: Boolean): this(
         object: BooleanBinding() {
            init {
               super.bind(condition)
            }

            override fun dispose() = super.unbind(condition)
            override fun computeValue() = if (unless) !condition.value else condition.value
            override fun getDependencies() = singletonObservableList(condition)
         }
      )

      constructor(condition: Boolean): this(vAlways(condition))

      override fun isValid(value: Any?) = true
      override fun message() = "Is disabled"
   }

}

/** Simple [Constraint] that acts as [FunctionalInterface] */
class ConstraintSimple<T>(val message: String, val test: (T?) -> Boolean): Constraint<T> {
   override fun isValid(value: T?): Boolean = test(value)
   override fun message(): String = message
}