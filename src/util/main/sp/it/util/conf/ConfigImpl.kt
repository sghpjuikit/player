package sp.it.util.conf

import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import sp.it.util.access.EnumerableValue
import sp.it.util.access.OrV
import sp.it.util.conf.ConfigImpl.ConfigBase
import sp.it.util.conf.OrPropertyConfig.OrValue
import sp.it.util.file.properties.PropVal
import sp.it.util.functional.runTry
import sp.it.util.parsing.Parsers
import java.lang.reflect.Field
import java.util.function.Supplier


/** [Config] wrapping a value. Not observable. */
class ValueConfig<V>: ConfigBase<V> {

   private var value: V

   constructor(type: Class<V>, name: String, nameUi: String, value: V, group: String, info: String, editable: EditMode): super(type, name, nameUi, value, group, info, editable) {
      this.value = value
   }

   constructor(type: Class<V>, name: String, value: V, info: String): super(type, name, name, value, "", info, EditMode.USER) {
      this.value = value
   }

   @Suppress("UNCHECKED_CAST")
   override fun getValue(): V {
      return value
   }

   override fun setValue(value: V) {
      this.value = value
   }

}

/** [Config] that does not store value, instead uses the getter and setter which provide/accept it. Not observable. */
class AccessConfig<T>: ConfigBase<T>, WritableValue<T> {

   private val setter: (T) -> Unit
   private val getter: () -> T

   /**
    * @param setter defines [setValue]
    * @param getter defines [getValue]
    */
   constructor(
      type: Class<T>, name: String, gui_name: String, setter: (T) -> Unit, getter: () -> T, group: String, info: String, editable: EditMode
   ): super(type, name, gui_name, getter(), group, info, editable) {
      this.getter = getter
      this.setter = setter
   }

   /**
    * @param setter defines [setValue]
    * @param getter defines [getValue]
    */
   constructor(
      type: Class<T>, name: String, description: String, setter: (T) -> Unit, getter: () -> T
   ): super(type, name, name, getter(), "", description, EditMode.USER) {
      this.getter = getter
      this.setter = setter
   }

   override fun getValue() = getter()

   override fun setValue(value: T) {
      setter(value)
   }

}

/** [Config] backed by [Field] and an object instance. Can wrap both static or instance fields. */
@Suppress("UNCHECKED_CAST")
open class FieldConfig<T> (
   name: String, c: ConfigDefinition, constraints: Set<Constraint<T>>, private val instance: Any?, group: String, private val field: Field
): ConfigBase<T>(field.type as Class<T>, name, c, constraints, field.value<T>(instance), group) {

   override fun getValue(): T = field.value(instance)

   override fun setValue(value: T) {
      runTry {
         field.isAccessible = true
         field.set(instance, value)
      }.mapError {
         RuntimeException("Error setting config field $name to $field", it)
      }.orThrow
   }

   companion object {
      private fun <T> Field.value(instance: Any?): T = runTry {
         isAccessible = true
         get(instance) as T
      }.mapError {
         RuntimeException("Error getting config field $name from $this", it)
      }.orThrow
   }
}

@Suppress("UNCHECKED_CAST")
open class PropertyConfig<T> @JvmOverloads constructor(
   valueType: Class<T>, name: String, c: ConfigDefinition, constraints: Set<Constraint<T>>, val property: WritableValue<T>, defaultValue: T = property.value, group: String
): ConfigBase<T>(valueType, name, c, constraints, defaultValue, group) {

   init {
      if (this.property is EnumerableValue<*>)
         valueEnumerator2nd = Supplier {
            (this.property as EnumerableValue<T>).enumerateValues()
         }
   }

   override fun getValue(): T = property.value

   override fun setValue(value: T) {
      property.value = value
   }

}

@Suppress("UNCHECKED_CAST")
open class ReadOnlyPropertyConfig<T>(
   valueType: Class<T>, name: String, c: ConfigDefinition, constraints: Set<Constraint<T>>, val property: ObservableValue<T>, group: String
): ConfigBase<T>(valueType, name, c, constraints, property.value, group) {

   init {
      if (property is EnumerableValue<*>)
         valueEnumerator2nd = Supplier {
            (property as EnumerableValue<T>).enumerateValues()
         }
   }

   override fun getValue(): T = property.value

   override fun setValue(value: T) {}

}

/** [Config] backed by [OrV] and of [OrValue] type. Observable. */
open class OrPropertyConfig<T>: ConfigBase<OrValue<T>> {
   val property: OrV<T>
   val valueType: Class<T>

   @Suppress("UNCHECKED_CAST")
   constructor(
      valueType: Class<T>, name: String, c: ConfigDefinition, constraints: Set<Constraint<OrValue<T>>>, property: OrV<T>, group: String
   ): super(
      OrValue::class.java as Class<OrValue<T>>, name, c, constraints, OrValue(property.override.value, property.real.value), group
   ) {
      this.property = property
      this.valueType = valueType
   }

   override fun getValue() = OrValue(property.override.value, property.real.value)

   override fun setValue(value: OrValue<T>) {
      property.real.value = value.value
      property.override.value = value.override
   }

   override var valueAsProperty: PropVal
      get() = PropVal.PropValN(
         listOf(
            Parsers.DEFAULT.toS(property.override.value),
            Parsers.DEFAULT.toS(property.real.value)
         )
      )
      set(v) {
         val s = v.valN
         if (s.size!=2) {
            Parsers.DEFAULT.ofS(Boolean::class.javaObjectType, s[0])
               .ifOk { property.override.value = it }
               .ifError { logger.warn(it) { "Unable to set config=$name override value (Boolean.class) from text='${s[0]}'" } }
            Parsers.DEFAULT.ofS(valueType, s[1])
               .ifOk { property.real.value = it }
               .ifError { logger.warn(it) { "Unable to set config=$name real value ($valueType) from text='${s[0]}'" } }
         } else {
            logger.warn { "Unable to set config=$name value from property='$s', must have 2 values" }
         }
      }

   data class OrValue<T>(val override: Boolean, val value: T)
}