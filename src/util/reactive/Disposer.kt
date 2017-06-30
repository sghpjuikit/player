package util.reactive

import org.reactfx.Subscription

class Disposer {
    private val disposers = ArrayList<() -> Unit>()

    operator fun invoke() {
        disposers.forEach { it() }
        disposers.clear()
    }

    operator fun plusAssign(disposer: () -> Unit): Unit {
        disposers += disposer
    }

    operator fun plusAssign(disposer: Subscription): Unit {
        disposers += { disposer.unsubscribe() }
    }
}