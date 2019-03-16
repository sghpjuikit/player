package sp.it.pl.util.reactive

import org.reactfx.Subscription

/** Convenience method for adding this subscription to a disposer. Equivalent to: this.apply(disposerRegister) */
infix fun Subscription.on(disposerRegister: (Subscription) -> Unit) = apply(disposerRegister)

/** Equivalent to [Subscription.and]. */
operator fun Subscription.plus(subscription: Subscription) = and(subscription)!!

/** Equivalent to [Subscription.and]. */
operator fun Subscription.plus(subscription: () -> Unit) = and(subscription)!!

/** Equivalent to [Subscription.and]. */
operator fun Subscription.plus(subscription: Disposer) = and(subscription)!!

/** @return this or empty subscription if null */
fun Subscription?.orEmpty() = this ?: Subscription.EMPTY!!