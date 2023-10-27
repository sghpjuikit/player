package sp.it.util.reactive

class Disposer: () -> Unit, Unsubscriber {

   private val disposers = ArrayList<() -> Unit>()

   fun isEmpty() = disposers.isEmpty()

   override fun invoke() {
      disposers.forEach { it() }
      disposers.clear()
   }

   override fun invoke(subscribtion: Unsubscribable) {
      this += subscribtion
   }

   operator fun plusAssign(disposer: () -> Unit) {
      disposers += disposer
   }

   operator fun plusAssign(disposer: Disposer) {
      disposers += disposer
   }

   operator fun plusAssign(disposer: Unsubscribable) {
      disposers += { disposer.unsubscribe() }
   }

   operator fun plusAssign(disposers: Iterable<() -> Unit>) {
      disposers.forEach { this += it }
   }

   @JvmName("plusAssignSubs")
   operator fun plusAssign(disposers: Iterable<Unsubscribable>) {
      disposers.forEach { this += it }
   }

}

fun Handler0.asDisposer(): Disposer = Disposer().apply { this@asDisposer += this }