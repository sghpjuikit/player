package sp.it.pl.util.reactive

import javafx.beans.value.ObservableValue
import org.reactfx.Subscription

private typealias Subscribing = (Subscribed) -> Subscription

/** Lazy subscribing. Convenient for creating toggleable features. */
class Subscribed: Subscription {
    private val subscribing: Subscribing
    private var s: Subscription? = null
    val isSubscribed get() = s!=null

    constructor(subscribing: Subscribing) {
        this.subscribing = subscribing
    }

    /** Toggles this subscribed to specified value - true subscribes, false unsubscribes. */
    fun subscribe(on: Boolean = true) {
        if (on) {
            if (s==null)
                s = subscribing(this)
        } else {
            s?.unsubscribe()
            s = null
        }
    }

    /** Equivalent to subscribe(!isSubscribed). */
    fun subscribeToggle() = subscribe(!isSubscribed)

    /** Equivalent to [subscribeToggle]. */
    operator fun not() = subscribeToggle()

    /** Equivalent to [subscribe] using false. */
    override fun unsubscribe() = subscribe(false)
}

/**
 * Create [Subscribed] for specified subscribing that will be subscribed exactly when the specified value is true.
 * @return subscription that will unsubscribe the subscribed and prevent future subscribing
 */
fun subscribedIff(whileIs: ObservableValue<Boolean>, subscribing: Subscribing): Subscription {
    val s1 = Subscribed(subscribing)
    val s2 = whileIs sync s1::subscribe
    return Subscription { s1.subscribe(false); s2.unsubscribe() }
}

/** @see [subscribedIff] */
infix fun Subscribing.iff(whileIs: ObservableValue<Boolean>) = subscribedIff(whileIs, this)