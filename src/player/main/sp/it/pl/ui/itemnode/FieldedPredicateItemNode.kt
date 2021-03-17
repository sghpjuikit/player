package sp.it.pl.ui.itemnode

import java.util.function.Predicate
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.functional.TypeAwareF
import sp.it.util.functional.Util.IS
import sp.it.util.functional.Util.IS0
import sp.it.util.functional.Util.ISNT
import sp.it.util.functional.Util.ISNT0

object FieldedPredicateItemNodeCompanion {

   fun <V, T> predicate(field: ObjectField<V, T>, f: (T) -> Boolean): Predicate<V> = when {
      f===IS0 || f===ISNT0 || f===IS || f===ISNT || !field.type.isNullable || (f is TypeAwareF<*, *> && f.typeIn.isNullable) -> Predicate { f(field.getOf(it)) }
      else -> Predicate { element: V ->
         val o = field.getOf(element)
         o!=null && f(o)
      }
   }

}