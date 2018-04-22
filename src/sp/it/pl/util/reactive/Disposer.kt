package sp.it.pl.util.reactive

import org.reactfx.Subscription

class Disposer: () -> Unit {
    private val disposers = ArrayList<() -> Unit>()

    override fun invoke() {
        disposers.forEach { it() }
        disposers.clear()
    }

    operator fun plusAssign(disposer: () -> Unit) {
        disposers += disposer
    }

    operator fun plusAssign(disposer: Subscription) {
        disposers += { disposer.unsubscribe() }
    }

}