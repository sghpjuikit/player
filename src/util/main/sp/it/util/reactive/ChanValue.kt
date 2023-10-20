package sp.it.util.reactive

import javafx.beans.value.ObservableValue
import javafx.util.Duration
import sp.it.util.async.executor.EventReducer
import sp.it.util.functional.orNull

/** JavaFX channel, i.e., [javafx.beans.value.ObservableValue] without [javafx.beans.value.ObservableValue.getValue] method. */
interface ChanValue<T> {
   infix fun subscribe(listener: (T) -> Unit): Subscription
}


private abstract class MappedChanValue<T>: ChanValue<T> {
   protected val listeners = lazy { Handler1<T>() }

   override fun subscribe(listener: (T) -> Unit): Subscription {
      listeners.value += listener
      updateListening()
      return Subscription {
         listeners.orNull()?.remove(listener)
         updateListening()
      }
   }

   protected abstract fun updateListening()

}

fun <T> ObservableValue<T>.chan(): ChanValue<T> = object: MappedChanValue<T>() {
   private val s = Subscribed {
      this@chan attach { nv -> listeners.orNull()?.invoke(nv) }
   }

   override fun updateListening() = s.subscribe(listeners.orNull()?.isNotEmpty()==true)
}

/** @return chan value that holds last value of this chan value the specified time period after it changes */
infix fun <T> ChanValue<T>.throttleToLast(period: Duration): ChanValue<T> = object: MappedChanValue<T>() {
   private val r = EventReducer.toLast<T>(period.toMillis()) { listeners.orNull()?.invoke(it) }
   private val s = Subscribed {
      this@throttleToLast subscribe r::push
   }

   override fun updateListening() = s.subscribe(listeners.orNull()?.isNotEmpty()==true)
}

infix fun <T, O> ChanValue<T>.map(mapper: (T) -> O): ChanValue<O> = object: MappedChanValue<O>() {
   private val s = Subscribed {
      this@map subscribe { nv -> listeners.orNull()?.invoke(mapper(nv)) }
   }

   override fun updateListening() = s.subscribe(listeners.orNull()?.isNotEmpty()==true)
}

fun <T> ChanValue<T?>.notNull(): ChanValue<T & Any> = object: MappedChanValue<T & Any>() {
   private val s = Subscribed {
      this@notNull subscribe { nv -> if (nv!=null) listeners.orNull()?.invoke(nv) }
   }

   override fun updateListening() = s.subscribe(listeners.orNull()?.isNotEmpty()==true)
}

fun <T> ChanValue<T>.filter(test: (T) -> Boolean): ChanValue<T> = object: MappedChanValue<T>() {
   private val s = Subscribed {
      this@filter subscribe { nv -> if (test(nv)) listeners.orNull()?.invoke(nv) }
   }

   override fun updateListening() = s.subscribe(listeners.orNull()?.isNotEmpty()==true)
}

infix operator fun <R, T: R, O: R> ChanValue<T>.plus(other: ChanValue<O>): ChanValue<R> = object: MappedChanValue<R>() {
   private val s = Subscribed {
      this@plus subscribe { nv -> listeners.orNull()?.invoke(nv) }
      other subscribe { nv -> listeners.orNull()?.invoke(nv) }
   }

   override fun updateListening() = s.subscribe(listeners.orNull()?.isNotEmpty()==true)
}

fun <T> ChanValue<T>.first(): ChanValue<T> = object: ChanValue<T> {
   protected val listeners = lazy { Handler1<T>() }
   private val s = Subscribed {
      this@first subscribe { nv -> listeners.orNull()?.invoke(nv); it.unsubscribe() }
   }

   override fun subscribe(listener: (T) -> Unit): Subscription {
      listeners.value += listener
      updateListening()
      return Subscription {
         listeners.orNull()?.remove(listener)
         updateListening()
      }
   }
   private fun updateListening() = s.subscribe(listeners.orNull()?.isNotEmpty()==true)
}