@file:Suppress("unused")

package sp.it.util.conf

import de.jensd.fx.glyphs.GlyphIcons
import javafx.beans.binding.BooleanBinding
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections.singletonObservableList
import javafx.util.Duration
import sp.it.util.access.vAlways
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
import sp.it.util.dev.failIfNot
import sp.it.util.file.FileType
import sp.it.util.functional.Try
import java.io.File

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

   class NumberMinMax(val min: Double?, val max: Double?): Constraint<Number> {

      init {
         failIf(min==null && max==null) { "Min and max can not both be null" }
         failIf(min!=null && max!=null && max<min) { "Max value must be greater than or equal to min value" }
      }

      fun isClosed() = min!=null && max!=null

      override fun isValid(value: Number?) = value==null || ((min==null || value.toDouble()>=min) && (max==null || value.toDouble()<=max))
      override fun message() = when {
         isClosed() -> "Number must be in range $min - $max"
         min!=null -> "Number must be at least $min"
         max!=null -> "Number must be at most $max"
         else -> fail()
      }
   }

   class StringNonEmpty: Constraint<String> {
      override fun isValid(value: String?) = value==null || value.isNotEmpty()
      override fun message() = "String must not be empty"
   }

   class StringLength(val min: Int, val max: Int): Constraint<String> {

      init {
         failIfNot(max>min) { "Max value must be greater than min value" }
      }

      override fun isValid(value: String?) = value==null || value.length in min..max
      override fun message() = "Text must be at least $min and at most$max characters long"
   }

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
      override fun message() = "All items of the list must be non null"
   }

   object ObjectNonNull: Constraint<Any?> {
      override fun isValid(value: Any?) = value!=null
      override fun message() = "Value must not be null"
   }

   object PreserveOrder: MarkerConstraint()

   /** Avoid showing the config in ui. */
   object NoUi: MarkerConstraint()

   /** Avoid showing the set-to-default button for the config in ui. Use for 'computed' configs, like singletons. */
   object NoUiDefaultButton: MarkerConstraint()

   /** Avoid persisting the config. Use for 'computed' configs. Configs with [Config.isEditable]==[EditMode.NONE] are not persistent by default. */
   object NoPersist: MarkerConstraint()

   /** Use save file chooser in ui, allowing to define files that do not exist. */
   object FileOut: MarkerConstraint()

   /** Use single icon mode for boolean config editor and disabled style for false. */
   class IconConstraint(val icon: GlyphIcons): MarkerConstraint()

   /** Constrain value to those specified in the collection. May be mutable (see [ValueSetNotContainsThen]). */
   class ValueSet<T>(val enumerator: () -> Collection<T>): MarkerConstraint()

   /** Strategy for dealing with value outside specified set in [ValueSet]. Default is [Strategy.USE_DEFAULT]. */
   class ValueSetNotContainsThen(val strategy: Strategy): MarkerConstraint() {
      enum class Strategy {
         USE_AND_ADD,
         USE,
         USE_DEFAULT
      }
   }

   class UiConverter<T>(val converter: (T) -> String): MarkerConstraint()

   class UiElementConverter<T>(val converter: (T) -> String): MarkerConstraint()

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