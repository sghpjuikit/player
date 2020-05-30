package sp.it.util.reactive

import javafx.beans.value.ObservableValue
import javafx.util.Duration
import sp.it.util.animation.Anim.Companion.anim

/** Lazy subscribing. Convenient for creating toggleable features. */
class Subscribed: Subscription {
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

   /** Equivalent to [subscribeToggle]. */
   operator fun not() = subscribeToggle()

   /** Equivalent to [subscribe] using false. */
   override fun unsubscribe() = subscribe(false)

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
   }
}
