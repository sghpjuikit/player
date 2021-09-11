package sp.it.util.access.fieldvalue

@FunctionalInterface
interface StringGetter<in V> {

   /**
    * Converts the value to a string representation.
    * When the object signifies empty value, a substitute is returned. The semantics of `empty` is left up for
    * implementation.
    *
    * @param value original object
    * @param substitute text used when the original object is 'empty'.
    * @return string representation of the original object
    */
   fun getOfS(value: V, substitute: String): String

   companion object {

      /** @return [StringGetter] implemented as the specified lambda */
      fun <V> of(stringGetter: (V, String) -> String) = object: StringGetter<V> {
         override fun getOfS(value: V, substitute: String) = stringGetter(value, substitute)
      }

   }
}