package sp.it.util.access.fieldvalue

import kotlin.reflect.full.withNullability
import sp.it.util.type.VType

// TODO: if `from` is nullable, the end result is nullable and may not conform to compile-time type!
@Suppress("UNCHECKED_CAST")
data class ObjectFieldFlatMapped<V, M, R>(val from: ObjectField<V, M>, val by: ObjectField<M, R>): ObjectFieldBase<V, R>(
   type = VType(by.type.type.withNullability(from.type.isNullable || by.type.isNullable)),
   extractor = if (from.type.isNullable) { it: V -> from.getOf(it)?.let(by::getOf) as R } else { it: V -> by.getOf(from.getOf(it)) },
   name = from.name() + "." + by.name(),
   description = "Combination of " + from.name() + " -> " + by.name(),
   toUi = { o, or -> by.toS(o, or) }
) {
   override fun cVisible() = from.cVisible() && by.cVisible()
   override fun cName() = by.cName()
   override fun toString() = name()
}