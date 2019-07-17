package sp.it.util.access.fieldvalue

import kotlin.reflect.KClass

class ColumnField: ObjectFieldBase<Any, Int> {

   private constructor(type: KClass<Int>, extractor: (Any) -> Int?, name: String, description: String): super(type, extractor, name, description)

   override fun toS(o: Int?, substitute: String): String = ""

   companion object: ObjectFieldRegistry<Any, ColumnField>(Any::class) {
      @JvmField val INDEX = this + ColumnField(Int::class, { null }, "#", "Index of the item in the list")
   }

}