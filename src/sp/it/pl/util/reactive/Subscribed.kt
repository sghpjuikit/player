package sp.it.pl.util.reactive

import org.reactfx.Subscription

/** Convenience class for creating toggleable features. */
class Subscribed(private val subscribing: () -> Subscription, private var s: Subscription? = null) {

    val isSubscribed get() = s!=null

    fun subscribe(on: Boolean = true) {
        when(on) {
            true -> if (s==null) s = subscribing()
            false -> s?.unsubscribe()
        }
    }

}