package sp.it.util.collections.map

import java.util.function.Supplier
import sp.it.util.collections.list.PrefList
import sp.it.util.functional.invoke

class PrefListMap<E: Any, K>(keyMapper: (E) -> K): CollectionMap<E, K, PrefList<E>>(Supplier { PrefList() }, keyMapper) {

   fun accumulate(e: E, pref: Boolean) {
      val k = keyMapper(e)
      val c = computeIfAbsent(k) { cacheFactory() }
      c.addPreferred(e, pref)
   }

}