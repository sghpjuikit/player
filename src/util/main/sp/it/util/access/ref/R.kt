package sp.it.util.access.ref

/**
 * Reference - object property.
 *
 * @param <V> type of value
 */
class R<V>(var value: V) {

   fun get(): V {
      return value
   }

   fun set(value: V) {
      this.value = value
   }

   fun setOf(op: (V) -> V) {
      this.value = op(this.value)
   }

   fun setOf(value: V, op: (V, V) -> V) {
      this.value = op(this.value, value)
   }

}