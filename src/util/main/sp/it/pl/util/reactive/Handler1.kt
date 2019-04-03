package sp.it.pl.util.reactive

import java.util.HashSet

/** Set of functions taking 1 parameter. Use as a collection of handlers/listeners. */
class Handler1<I>: HashSet<(I) -> Unit>(2), (I) -> Unit {

    /** Invokes all contained functions. */
    override fun invoke(input: I) {
        forEach { it(input) }
    }

    /** Adds specified function to this. Calling the return value will remove it. */
    fun addS(block: (I) -> Unit): Subscription {
        add(block)
        return Subscription { remove(block) }
    }

}