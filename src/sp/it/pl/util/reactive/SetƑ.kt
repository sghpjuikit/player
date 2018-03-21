package sp.it.pl.util.reactive

import org.reactfx.Subscription
import sp.it.pl.util.functional.Functors.Ƒ
import sp.it.pl.util.functional.invoke
import java.util.HashSet

/** Set of actions taking 0 parameters. Use as a collection of handlers. */
class SetƑ: HashSet<Runnable>(2), Ƒ {

    override fun apply() {
        forEach { it() }
    }

    fun addS(r: Runnable): Subscription {
        add(r)
        return Subscription { remove(r) }
    }

    fun addS(r: () -> Unit) = addS(Runnable { r() })

}