package sp.it.pl.util.reactive

class Disposer: () -> Unit, (Subscription) -> Unit {

    private val disposers = ArrayList<() -> Unit>()

    fun isEmpty() = disposers.isEmpty()

    override fun invoke() {
        disposers.forEach { it() }
        disposers.clear()
    }

    override fun invoke(subscribtion: Subscription) {
        this += subscribtion
    }

    operator fun plusAssign(disposer: () -> Unit) {
        disposers += disposer
    }

    operator fun plusAssign(disposer: Disposer) {
        disposers += disposer
    }

    operator fun plusAssign(disposer: Subscription) {
        disposers += { disposer.unsubscribe() }
    }

    operator fun plusAssign(disposers: Iterable<() -> Unit>) {
        disposers.forEach{ this += it }
    }

    @JvmName("plusAssignSubs")
    operator fun plusAssign(disposers: Iterable<Subscription>) {
        disposers.forEach{ this += it }
    }

}