package sp.it.util.conf

import javafx.beans.value.WritableValue
import sp.it.util.access.OrV
import sp.it.util.conf.ConfigImpl.ConfigBase
import sp.it.util.conf.OrPropertyConfig.OrValue
import sp.it.util.file.properties.PropVal
import sp.it.util.parsing.Parsers


/** [Config] wrapping a value. Not observable. */
class ValueConfig<V>: ConfigBase<V> {

   private var value: V

   constructor(type: Class<V>, name: String, uiName: String, value: V, category: String, info: String, editable: EditMode): super(type, name, uiName, value, category, info, editable) {
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
      type: Class<T>, name: String, gui_name: String, setter: (T) -> Unit, getter: () -> T, category: String, info: String, editable: EditMode
   ): super(type, name, gui_name, getter(), category, info, editable) {
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

/** [Config] backed by [OrV] and of [OrValue] type. Observable. */
class OrPropertyConfig<T>: ConfigBase<OrValue<T>> {
   val property: OrV<T>
   val valueType: Class<T>

   @Suppress("UNCHECKED_CAST")
   constructor(
      valueType: Class<T>, name: String, c: IsConfig, constraints: Set<Constraint<OrValue<T>>>, property: OrV<T>, category: String
   ): super(
      OrValue::class.java as Class<OrValue<T>>, name, c, constraints, OrValue(property.override.value, property.real.value), category
   ) {
      this.property = property
      this.valueType = valueType
   }

   constructor(valueType: Class<T>, name: String, property: OrV<T>): this(valueType, name, name, property, "", "", EditMode.USER)

   @Suppress("UNCHECKED_CAST")
   constructor(
      valueType: Class<T>, name: String, gui_name: String, property: OrV<T>, category: String, info: String, editable: EditMode
   ): super(
      OrValue::class.java as Class<OrValue<T>>, name, gui_name, OrValue(property.override.value, property.real.value), category, info, editable
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