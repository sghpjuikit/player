package sp.it.util.reactive

import java.util.HashSet

/** Set of functions taking 0 parameters. Use as a collection of handlers. */
class Handler0: HashSet<() -> Unit>(2), () -> Unit {

   /** Invokes all contained functions. */
   override operator fun invoke() {
      forEach { it() }
      removeIf { it is RemovingF }
   }

   /** Adds specified handler to this. Equivalent to [add], not to [addAll] (which would normally be used). */
   operator fun plusAssign(block: Handler0) {
      add { block() }
   }

   /** Adds specified block to this. */
   fun addS(block: () -> Unit): Subscription {
      add(block)
      return Subscription { remove(block) }
   }

   /** Adds specified block to this so it is removed after it runs. */
   fun addSOnetime(block: () -> Unit): Subscription {
      val r = RemovingF(block)
      add(r)
      return Subscription { remove(r) }
   }

}

private class RemovingF(private val block: () -> Unit): () -> Unit by block