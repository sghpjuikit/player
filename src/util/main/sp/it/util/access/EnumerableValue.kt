package sp.it.util.access

/** Value restricted to finite number of specific values that can be enumerated.  */
interface EnumerableValue<T> {

   /** @return all values in an enumeration */
   fun enumerateValues(): Collection<T>

}