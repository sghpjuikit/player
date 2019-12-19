@file:Suppress("unused")

package sp.it.util.conf

import javafx.beans.binding.BooleanBinding
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections.singletonObservableList
import javafx.util.Duration
import sp.it.util.access.vAlways
import sp.it.util.dev.failIfNot
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
   }

   class FileRelative(val to: File): MarkerConstraint()

   class NumberMinMax(val min: Double, val max: Double): Constraint<Number> {

      init {
         failIfNot(max>min) { "Max value must be greater than min value" }
      }

      override fun isValid(value: Number?) = value==null || value.toDouble() in min..max
      override fun message() = "Number must be in range $min - $max"
   }

   class StringNonEmpty: Constraint<String> {
      override fun isValid(value: String?) = value!=null && value.isNotEmpty()
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

   object ObjectNonNull: Constraint<Any> {
      override fun isValid(value: Any?) = value!=null
      override fun message() = "Value must not be null"
   }

   object PreserveOrder: MarkerConstraint()

   /** Avoid showing the config in ui. */
   object NoUi: MarkerConstraint()

   /** Avoid persisting the config. Use for 'computed' configs. Configs with [Config.isEditable]==[EditMode.NONE] are not persistent by default. */
   object NoPersist: MarkerConstraint()

   class ValueSet<T>(val enumerator: () -> Collection<T>): MarkerConstraint()

   class UiConverter<T>(val converter: (T) -> String): MarkerConstraint()

   class ReadOnlyIf(val condition: ObservableValue<Boolean>): Constraint<Any> {
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