package sp.it.util.access.fieldvalue

import sp.it.util.dev.failCase
import kotlin.reflect.KClass

class ColumnField: ObjectFieldBase<Any, Int> {

   internal constructor(type: KClass<Int>, extractor: (Any) -> Int?, name: String, description: String): super(type, extractor, name, description) {
      FIELDS_IMPL += this
   }

   override fun toS(o: Int?, substitute: String): String = ""

   companion object {

      private val FIELDS_IMPL = HashSet<ColumnField>()
      @JvmField val FIELDS: Set<ColumnField> = FIELDS_IMPL
      @JvmField val INDEX = ColumnField(Int::class, { null }, "#", "Index of the item in the list")

      fun valueOf(s: String) = when (s) {
         INDEX.name() -> INDEX
         else -> failCase(s)
      }

   }

}