package sp.it.pl.util.reactive

interface Subscription {

    // TODO: figure out if Subscription should be one-time by design (this would also help with mem leaks)
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
        override fun unsubscribe() {
            s()
        }
    }

    private class Bi(private val s1: Subscription, private val s2: Subscription): Subscription {
        override fun unsubscribe() {
            s1.unsubscribe()
            s2.unsubscribe()
        }
    }

    private class Multi(private val subscriptions: Array<out Subscription>): Subscription {
        override fun unsubscribe() {
            subscriptions.forEach { it.unsubscribe() }
        }
    }

}

/** @return subscription that unsubscribes both this and the specified subscription */
infix operator fun Subscription.plus(subscription: Subscription) = Subscription(this, subscription)

/** @return subscription that unsubscribes this subscription and calls the specified block */
infix operator fun Subscription.plus(subscription: () -> Unit) = Subscription(this, Subscription(subscription))

/** Convenience method for adding this subscription to a disposer. Equivalent to: this.apply(disposerRegister) */
infix fun Subscription.on(disposerRegister: (Subscription) -> Unit) = apply(disposerRegister)

/** @return this or empty subscription if null */
fun Subscription?.orEmpty() = this ?: Subscription()