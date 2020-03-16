package sp.it.util.reactive

import sp.it.util.type.nullify

interface Subscription {

   fun unsubscribe()

   companion object {

      operator fun invoke() = Empty as Subscription

      operator fun invoke(s1: Subscription) = s1

      operator fun invoke(block: () -> Unit) = Uni(block) as Subscription

      operator fun invoke(s1: Subscription, s2: Subscription) = Bi(s1, s2) as Subscription

      operator fun invoke(s1: () -> Unit, s2: () -> Unit) = Bi(Subscription(s1), Subscription(s2)) as Subscription

      operator fun invoke(vararg subs: Subscription) = Multi(subs) as Subscription

      operator fun invoke(vararg subs: () -> Unit) = Multi(subs.map { Subscription(it) }.toTypedArray()) as Subscription

   }

   private object Empty: Subscription {
      override fun unsubscribe() {}
   }

   private class Uni(private val s: () -> Unit): Subscription {
      private var invoked = false

      override fun unsubscribe() {
         if (!invoked) s()
         invoked = true
         nullify(::s)
      }
   }

   private class Bi(private val s1: Subscription, private val s2: Subscription): Subscription {
      private var invoked = false

      override fun unsubscribe() {
         if (!invoked) s1.unsubscribe()
         if (!invoked) s2.unsubscribe()
         invoked = true
         nullify(::s1)
         nullify(::s2)
      }
   }

   private class Multi(private val subscriptions: Array<out Subscription>): Subscription {
      private var invoked = false

      override fun unsubscribe() {
         if (!invoked) subscriptions.forEach { it.unsubscribe() }
         invoked = true
         nullify(::subscriptions)
      }
   }

}

/** @return subscription that unsubscribes both this and the specified subscription */
infix operator fun Subscription.plus(subscription: Subscription) = Subscription(this, subscription)

/** @return subscription that unsubscribes this subscription and calls the specified block */
infix operator fun Subscription.plus(subscription: () -> Unit) = Subscription(this, Subscription(subscription))

/** Convenience method for adding this subscription to a disposer. Equivalent to: this.apply(disposerRegister) */
infix fun Subscription.on(disposer: DisposeOn) = apply(disposer)

/** @return this or empty subscription if null */
fun Subscription?.orEmpty() = this ?: Subscription()

typealias DisposeOn = (Subscription) -> Unit