package sp.it.util.reactive

import javafx.beans.value.ObservableValue
import javafx.util.Duration
import sp.it.util.access.V
import sp.it.util.access.v
import sp.it.util.animation.Anim.Companion.anim

/** Lazy subscribing. Convenient for creating toggleable features. */
class Subscribed: Unsubscribable {
   private val subscribing: (Subscribed) -> Subscription
   private var s: Subscription? = null
   val isSubscribed get() = s!=null

   constructor(subscribing: (Subscribed) -> Subscription) {
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

   /** Equivalent to calling subscribe(false) and then subscribe(true). */
   fun resubscribe() {
      subscribe(false)
      subscribe(true)
   }

   /** Equivalent to [subscribeToggle]. */
   operator fun not() = subscribeToggle()

   /** Equivalent to [subscribe] using false. */
   override fun unsubscribe() = subscribe(false)

   /**
    * Returns value that [subscribe]s this on/off.
    * This subscribed should only be controlled through the value, as it does not reflect changes through this subscribed.
    */
   fun toV(on: Boolean = isSubscribed): V<Boolean> = v(on).apply { sync(::subscribe) }

   companion object {

      /**
       * Create [Subscribed] that [Subscribed.subscribe]s with a specified delay. Unsubscribing has no delay. Fx thread only.
       *
       * @return subscription that will unsubscribe the subscribed and prevent future subscribing
       */
      fun delayedFx(delay: Duration, block: (Boolean) -> Unit) = Subscribed {
         val a = anim(delay) { }.apply {
            playOpenDo { block(true) }
         }
         Subscription {
            a.stop()
            block(false)
         }
      }

      /**
       * Create [Subscribed] for specified subscribing that will be subscribed exactly when the specified value is true.
       *
       * @return subscription that will unsubscribe the subscribed and prevent future subscribing
       */
      fun subscribedIff(whileIs: ObservableValue<Boolean>, subscribing: (Subscribed) -> Subscription): Subscription {
         val s1 = Subscribed(subscribing)
         val s2 = whileIs sync s1::subscribe
         return Subscription { s1.subscribe(false); s2.unsubscribe() }
      }

      /**
       * Create [Subscribed] for specified subscribing that will be subscribed between next invocations of [from] and [to].
       *
       * @return subscription that will unsubscribe the subscribed and prevent future subscribing
       */
      fun subBetween(from: Handler0, to: Handler0, subscribing: (Subscribed) -> Subscription): Subscription {
         val s = Subscribed(subscribing)
         return from.addRem { s.subscribe() } + to.addRem { s.unsubscribe() } + { s.unsubscribe() }
      }

   }

}
