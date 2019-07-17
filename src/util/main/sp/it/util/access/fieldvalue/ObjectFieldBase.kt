package sp.it.util.access.fieldvalue

import kotlin.reflect.KClass

abstract class ObjectFieldBase<V, T: Any>: ObjectField<V, T> {

   private val name: String
   private val description: String
   private val type: Class<T>
   private val extractor: (V) -> T?

   constructor(type: KClass<T>, extractor: (V) -> T?, name: String, description: String) {
      this.name = name
      this.description = description
      this.type = type.java
      this.extractor = extractor
   }

   override fun name() = name

   override fun description() = description

   override fun getType() = type

   override fun getOf(value: V) = extractor(value)

   override fun toString() = name

}