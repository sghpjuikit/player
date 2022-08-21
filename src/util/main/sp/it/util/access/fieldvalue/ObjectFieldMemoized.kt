package sp.it.util.access.fieldvalue

import java.util.IdentityHashMap

class ObjectFieldMemoized<V, R>(val from: ObjectField<V, R>): ObjectField<V, R> {
   private val cache = IdentityHashMap<V, R>()
   override val type = from.type
   override fun description() = from.description()
   override fun name() = from.name()
   override fun toS(o: R?, substitute: String) = from.toS(o, substitute)
   override fun getOf(value: V): R = cache.computeIfAbsent(value, from::getOf)
}