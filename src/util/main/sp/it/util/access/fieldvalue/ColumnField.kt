package sp.it.util.access.fieldvalue

import sp.it.util.type.VType
import sp.it.util.type.type

sealed class ColumnField: ObjectFieldBase<Any, Int> {

   constructor(type: VType<Int>, extractor: (Any) -> Int, name: String, description: String, toUi: (Any?, String) -> String): super(type, extractor, name, description, toUi)

   object INDEX: ColumnField(type(), { -1 }, "#", "Index of the item in the list", { o, or -> o?.let { "" } ?: or })

   companion object: ObjectFieldRegistry<Any, ColumnField>(Any::class) {
      init { register(INDEX) }
   }
}