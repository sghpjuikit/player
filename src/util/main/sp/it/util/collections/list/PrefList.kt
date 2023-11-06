package sp.it.util.collections.list

import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.UnaryOperator

/** List with a preferred item. The item is always contained within list. */
class PrefList<E: Any> @JvmOverloads constructor(elements: List<E> = listOf(), preferred: E? = null): ArrayList<E>() {

   private var pref: E? = null

   var preferred: E?
      /** @return preferred item or null if none preferred or list empty */
      get() = pref
      set(e) {
         if (e==null) {
            pref = null
         } else {
            val i = indexOf(e)
            if (i>=0) pref = get(i)
         }
      }

   init {
      this += elements
      this.preferred = preferred
   }

   /** @return preferred item or first item or null if none preferred or list empty */
   val preferredOrFirst: E?
      get() = pref ?: firstOrNull()

   infix fun has(element: E?): Boolean =
      element!=null && super.contains(element)

   fun addPreferred(e: E) {
      pref = e
      super.add(e)
   }

   fun addPreferred(index: Int, element: E) {
      pref = element
      super.add(index, element)
   }

   fun addPreferred(e: E, preferred: Boolean) {
      if (preferred) addPreferred(e) else add(e)
   }

   override fun remove(element: E): Boolean {
      if (pref!=null && pref==element) pref = null
      return super.remove(element)
   }

   override fun removeAt(at: Int): E {
      if (pref!=null && pref==get(at)) pref = null
      return super.removeAt(at)
   }

   override fun removeIf(filter: Predicate<in E>): Boolean {
      if (pref!=null && filter.test(pref!!)) pref = null
      return super.removeIf(filter)
   }

   override fun removeAll(elements: Collection<E>): Boolean {
      if (pref!=null && elements.contains(pref)) pref = null
      return super.removeAll(elements)
   }

   override fun replaceAll(operator: UnaryOperator<E>) {
      val i = if (pref==null) -1 else indexOf(pref)
      super.replaceAll(operator)
      if (i!=-1) pref = get(i)
   }

   override fun retainAll(elements: Collection<E>): Boolean {
      if (pref!=null && !elements.contains(pref)) pref = null
      return super.retainAll(elements)
   }

   override fun clear() {
      pref = null
      super.clear()
   }

   override fun removeRange(fromIndex: Int, toIndex: Int) {
      if (pref!=null) {
         val i = indexOf(pref)
         if (i>=fromIndex && i<toIndex) pref = null
      }
      super.removeRange(fromIndex, toIndex)
   }

   fun <T: Any> map(mapper: (E) -> T): PrefList<T> {
      val i = if (pref==null) -1 else indexOf(pref)
      val l = PrefList<T>()
      forEach { l += mapper(it) }
      if (i!=-1) pref = get(i)
      return l
   }

}