package sp.it.util.reactive

import javafx.beans.value.ObservableValue
import javafx.event.EventType
import javafx.stage.Window
import javafx.stage.WindowEvent
import sp.it.util.type.nullify

interface Subscription: Unsubscribable {

   override fun unsubscribe()

   companion object {

      operator fun invoke(): Subscription = Empty

      operator fun invoke(s1: Subscription): Subscription = s1

      operator fun invoke(block: () -> Unit): Subscription = Uni(block)

      operator fun invoke(s1: Subscription, s2: Subscription): Subscription = Bi(s1, s2)

      operator fun invoke(s1: () -> Unit, s2: () -> Unit): Subscription = Bi(Subscription(s1), Subscription(s2))

      operator fun invoke(vararg subs: Subscription): Subscription = Multi(subs)

      operator fun invoke(vararg subs: () -> Unit): Subscription = Multi(subs.map { Subscription(it) }.toTypedArray())

   }

   private object Empty: Subscription {
      override fun unsubscribe() {}
   }

   private class Uni(private val s: () -> Unit): Subscription {
      private var invoked = false

      override fun unsubscribe() {
         if (!invoked) s()
         invoked = true
         nullify(::s)
      }
   }

   private class Bi(private val s1: Subscription, private val s2: Subscription): Subscription {
      private var invoked = false

      override fun unsubscribe() {
         if (!invoked) s1.unsubscribe()
         if (!invoked) s2.unsubscribe()
         invoked = true
         nullify(::s1)
         nullify(::s2)
      }
   }

   private class Multi(private val subscriptions: Array<out Subscription>): Subscription {
      private var invoked = false

      override fun unsubscribe() {
         if (!invoked) subscriptions.forEach { it.unsubscribe() }
         invoked = true
         nullify(::subscriptions)
      }
   }

}

/** @return subscription that unsubscribes both this and the specified subscription */
infix operator fun Subscription.plus(subscription: Subscription) = Subscription(this, subscription)

/** @return subscription that unsubscribes this subscription and calls the specified block */
infix operator fun Subscription.plus(subscription: () -> Unit) = Subscription(this, Subscription(subscription))

/** @return this or empty subscription if null */
fun Subscription?.orEmpty() = this ?: Subscription()

/** @return [Unsubscribable] that unsubscribes subscription on [onEventDown1] using the specified event type */
infix fun Window.fires(eventType: EventType<WindowEvent>): Unsubscriber = { s -> onEventDown1(eventType) { s.unsubscribe() } }

/** @return [Unsubscribable] that unsubscribes subscription on [sync1If] using the specified value condition */
infix fun <T> ObservableValue<T>.fires(condition: (T) -> Boolean): Unsubscriber = { s -> sync1If(condition) { s.unsubscribe() } }

/** @return [Unsubscribable] that unsubscribes subscription on [sync1If] using the condition matching [Any.equals] to the specified value */
infix fun <T> ObservableValue<T>.fires(value: T): Unsubscriber = fires(condition = { it==value })