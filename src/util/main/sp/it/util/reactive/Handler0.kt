package sp.it.util.reactive

import sp.it.util.functional.Functors.F
import java.util.HashSet

/** Set of functions taking 0 parameters. Use as a collection of handlers. */
class Handler0: HashSet<() -> Unit>(2), F {

   /** Invokes all contained functions. */
   override fun apply() {
      forEach { it() }
      removeIf { it is RemovingF }
   }

   /** Adds specified block to this. Calling the return value will remove it. */
   fun addS(block: () -> Unit): Subscription {
      add(block)
      return Subscription { remove(block) }
   }

   /** Adds specified block to this so it is removed after it runs. Calling the return value will remove it also. */
   fun addSOnetime(block: () -> Unit): Subscription {
      val r = RemovingF(block)
      add(r)
      return Subscription { remove(r) }
   }

}

private class RemovingF(private val block: () -> Unit): () -> Unit by block