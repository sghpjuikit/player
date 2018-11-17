package sp.it.pl.util.reactive

import javafx.beans.value.ObservableValue
import org.reactfx.Subscription

/** Means of consuming producer - these are abstracted away, only resulting subscription is necessary. */
private typealias Subscribing = () -> Subscription

/** Lazy subscribing. Convenient for creating toggleable features. */
class Subscribed(private val subscribing: Subscribing) {

    val isSubscribed get() = s!=null
    private var s: Subscription? = null

    fun subscribe(on: Boolean = true) {
        when (on) {
            true -> if (s==null) s = subscribing()
            false -> s?.unsubscribe()
        }
    }

}

/** @return unsubscribed lazy [Subscribed] using the specified subscribing function. */
fun subscribed(subscribing: Subscribing) = Subscribed(subscribing).subscribe()

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