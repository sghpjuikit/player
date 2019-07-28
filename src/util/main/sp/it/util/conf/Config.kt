package sp.it.util.conf

import javafx.beans.binding.BooleanBinding
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.ObservableList
import mu.KLogging
import sp.it.util.access.EnumerableValue
import sp.it.util.access.OrV
import sp.it.util.access.TypedValue
import sp.it.util.access.V
import sp.it.util.access.vAlways
import sp.it.util.conf.ConfigImpl.ListConfig
import sp.it.util.conf.ConfigImpl.PropertyConfig
import sp.it.util.conf.ConfigImpl.ReadOnlyPropertyConfig
import sp.it.util.dev.Experimental
import sp.it.util.dev.fail
import sp.it.util.file.properties.PropVal
import sp.it.util.file.properties.PropVal.PropVal1
import sp.it.util.functional.Try
import sp.it.util.functional.invoke
import sp.it.util.parsing.Parsers
import sp.it.util.type.Util.getEnumConstants
import sp.it.util.type.Util.isEnum
import sp.it.util.type.toRaw
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.function.Supplier

private typealias Enumerator<T> = Supplier<Collection<T>>

/**
 * Represents of a configurable value.
 *
 * Config abstracts away access and provides description of a value. Composes into [Configurable].
 *
 * @param <T> type of value of this config
 */
abstract class Config<T>: WritableValue<T>, Configurable<T>, TypedValue<T>, EnumerableValue<T> {

   abstract override fun getValue(): T

   abstract override fun setValue(value: T)

   abstract override fun getType(): Class<T>

   /** Name of this config. */
   abstract val name: String

   /** Semantic category/group path using '.' separator. */
   abstract val group: String

   /** Human readable [name]. Short, single line. */
   abstract val guiName: String

   /** Human readable description. Potentially multiple lines. */
   abstract val info: String

   /** Editability. This can be further restricted with constraints. */
   abstract val isEditable: EditMode

   fun isNotEditableRightNow() = constraints.asSequence().any { it is Constraint.ReadOnlyIf && it.condition.value }

   fun isEditableByUserRightNow() = isEditable.isByUser && !isNotEditableRightNow()

   fun isEditableByUserRightNowProperty(): ObservableValue<Boolean> {
      return if (!isEditable.isByUser) {
         vAlways(false)
      } else {
         val readOnlyIfs = findConstraints<Constraint.ReadOnlyIf>().map { it.condition }.toList()
         if (readOnlyIfs.isEmpty())
            vAlways(true)
         else
            object: BooleanBinding() {
               init {
                  bind(*readOnlyIfs.toTypedArray())
               }
               override fun computeValue() = readOnlyIfs.none { it.value }
            }
      }
   }

   /** Limits put on this value or markers that signify certain treatment of it. */
   abstract val constraints: Set<Constraint<T>>

   @Experimental("Expert API, mutates state")
   abstract fun addConstraints(vararg constraints: Constraint<T>): Config<T>

   @Experimental("Expert API, mutates state")
   fun addConstraints(constraints: Collection<Constraint<T>>): Config<T> = addConstraints(*constraints.toTypedArray())

   inline fun <reified T> findConstraint(): T? = findConstraints<T>().firstOrNull()

   inline fun <reified T> findConstraints(): Sequence<T> = constraints.asSequence().filterIsInstance<T>()

   /** Initial value. Sensible value and potential fallback when user provided value is invalid. */
   abstract val defaultValue: T

   open fun setValueToDefault() {
      value = defaultValue
   }

   open var valueAsProperty: PropVal
      get() {
         return PropVal1(Parsers.DEFAULT.toS(value))
      }
      set(property) {
         val s = property.val1
         if (s!=null)
            convertValueFromString(this, s)
               .ifOk { value = it }
               .ifError { logger.warn(it) { "Unable to set config=$name value from text=$s" } }
      }

   protected var valueEnumerator2nd: Enumerator<T>? = null

   protected val valueEnumerator: Enumerator<T>? by lazy {
      null
         ?: findConstraint<Constraint.ValueSet<T>>()?.let { values -> Enumerator { values.enumerator() } }
         ?: valueEnumerator2nd
         ?: if (!isEnum(type)) null else Enumerator { getEnumConstants<T>(type).toList() }
   }

   val isTypeEnumerable: Boolean
      get() = valueEnumerator!=null

   override fun enumerateValues(): Collection<T> = valueEnumerator?.invoke() ?: fail {
      "Config $name is not enumerable, because $type not enumerable."
   }

   override fun getField(name: String) = takeIf { it.name==name }

   override fun getFields() = listOf(this)

   companion object: KLogging() {

      /** Helper method. Expert API. */
      @JvmStatic
      fun <T> convertValueFromString(config: Config<T>, s: String): Try<T, String> {
         if (config.isTypeEnumerable) {
            for (v in config.enumerateValues())
               if (Parsers.DEFAULT.toS(v).equals(s, true)) return Try.ok(v)

            return Try.error("Value does not correspond to any value of the enumeration.")
         } else {
            return Parsers.DEFAULT.ofS(config.type, s)
         }
      }

      /**
       * Creates config for value. Attempts in order:
       *  *  [forProperty]
       *  *  create [ListConfig] if the value is [javafx.collections.ObservableList]
       *  *  wraps the value in [sp.it.util.access.V] and calls [forProperty]
       */
      @Suppress("UNCHECKED_CAST")
      @JvmStatic
      fun <T> forValue(type: Type, name: String, value: Any?): Config<T> = null
         ?: forPropertyImpl(type.toRaw(), name, value) as Config<T>?
         ?: run {
            when (value) {
               value is ObservableList<*> -> {
                  val itemType: Class<*> = when (type) {
                     is ParameterizedType -> type.actualTypeArguments.firstOrNull()?.toRaw() ?: Any::class.java
                     else -> Any::class.java
                  }
                  ListConfig(name, ConfList(itemType as Class<T>, true, value as ObservableList<T>)) as Config<T>
               }
               else -> forProperty(type.toRaw(), name, V(value)) as Config<T>
            }
         }

      /**
       * Creates config for property. Te property will become the underlying data
       * of the config and thus reflect any value changes and vice versa. If
       * the property is read only, config will also be read only (its set()
       * methods will not do anything). If the property already is config, it is
       * returned.
       *
       * @param name of of the config, will be used as gui name
       * @param property underlying property for the config. The property must be instance of any of:
       *  *  [Config]
       *  *  [ConfList]
       *  *  [WritableValue]
       *  *  [ObservableValue]
       *
       * so standard javafx properties will all work. If not instance of any of the above, runtime exception will be thrown.
       */
      @JvmStatic
      fun <T> forProperty(type: Class<T>, name: String, property: Any?): Config<T> = null
         ?: forPropertyImpl(type, name, property)
         ?: fail { "Property $name must be WritableValue or ReadOnlyValue, but is ${property?.javaClass}" }

      @Suppress("UNCHECKED_CAST")
      private fun <T> forPropertyImpl(type: Class<T>, name: String, property: Any?): Config<T>? = when (property) {
         is Config<*> -> property as Config<T>
         is ConfList<*> -> ListConfig(name, property) as Config<T>
         is OrV<*> -> OrPropertyConfig(type, name, property as OrV<T>) as Config<T>
         is WritableValue<*> -> PropertyConfig<T>(type, name, property as WritableValue<T>, "")
         is ObservableValue<*> -> ReadOnlyPropertyConfig<T>(type, name, property as ObservableValue<T>, "")
         else -> null
      }
   }
}