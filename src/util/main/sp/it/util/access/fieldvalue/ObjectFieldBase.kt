package sp.it.util.access.fieldvalue

import sp.it.util.type.VType

abstract class ObjectFieldBase<V, T>(
   override val type: VType<T>,
   private val extractor: (V) -> T,
   private val name: String,
   private val description: String,
   private val toUi: (T?, String) -> String
): ObjectField<V, T> {
   override fun name() = name
   override fun description() = description
   override fun getOf(value: V) = extractor(value)
   override fun toString() = name
   override fun toS(o: T?, substitute: String) = toUi(o, substitute)
}