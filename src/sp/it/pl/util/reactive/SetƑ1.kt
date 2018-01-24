package sp.it.pl.util.reactive

import org.reactfx.Subscription
import sp.it.pl.util.functional.Functors.Ƒ1
import sp.it.pl.util.functional.invoke
import java.util.HashSet
import java.util.function.Consumer

/** Set of actions taking 1 parameter. Use as a collection of handlers/listeners. */
class SetƑ1<I>: HashSet<Consumer<I>>(2), Ƒ1<I, Unit> {

    override fun apply(input: I) {
        forEach { it(input) }
    }

    fun addS(r: Consumer<I>): Subscription {
        add(r)
        return Subscription { remove(r) }
    }

}