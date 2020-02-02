package sp.it.util.access.fieldvalue

import sp.it.util.type.VType
import sp.it.util.type.type

class ColumnField: ObjectFieldBase<Any, Int> {

   private constructor(type: VType<Int>, extractor: (Any) -> Int, name: String, description: String): super(type, extractor, name, description)

   override fun toS(o: Int?, substitute: String): String = ""

   companion object: ObjectFieldRegistry<Any, ColumnField>(Any::class) {
      @JvmField val INDEX = this + ColumnField(type(), { -1 }, "#", "Index of the item in the list")
   }

}