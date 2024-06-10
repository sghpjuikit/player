package sp.it.util.async.executor

import sp.it.util.async.executor.EventReducer.HandlerEvery as HandlerEvery1
import java.util.function.Consumer
import javafx.util.Duration
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.functional.Functors.F2
import sp.it.util.functional.Option
import sp.it.util.functional.Option.None
import sp.it.util.functional.Option.Some
import sp.it.util.functional.asIs
import sp.it.util.functional.runnable
import sp.it.util.units.millis

/**
 * Event frequency reducer. Consumes events and reduces close temporal successions into (exactly)
 * single event.
 *
 * The reducing can work in two ways:
 *
 *  * Firing the first event will
 * be instantaneous (as soon as it arrives) and will in effect ignore all future events of the
 * succession.
 *
 *  * Firing the final event will cause all previous events to be accumulated into one event using
 * a reduction function (by default it simply ignores the events until the last one). It is then
 * fired, when the succession ends. Note the delay between last consumed event of the succession and
 * the succession ending. It only ends when the timer runs out and future events will start a new
 * succession. Even if the succession has only 1 event, there will still be delay between consuming
 * it and firing it as a reduced event.
 */
fun interface EventReducer<E> {
   fun push(event: E)

   abstract class EventReducerBase<E>(
      protected val inter_period: Double,
      protected val r: ((E, E) -> E)?,
      protected val action: (E) -> Unit
   ): EventReducer<E> {
      protected var e: Option<E> = None

      public override fun push(event: E) {
         val ee = e
         val rr = r
         e = Some(if (rr!=null && ee is Some) rr(ee.value, event) else event)
         handle()
      }

      protected abstract fun handle()
   }

   class HandlerLast<E>(inter_period: Double, reduction: ((E, E) -> E)?, handler: (E) -> Unit): EventReducerBase<E>(inter_period, reduction, handler) {
      private val t = fxTimer(inter_period.millis, 1) {
         action(e.asIs<Some<E>>().value)
      }

      protected override fun handle() =
         t.start(inter_period.millis)

      fun hasEventsQueued(): Boolean =
         t.isRunning
   }

   class HandlerEvery<E>(inter_period: Double, reduction: (E, E) -> E, handler: (E) -> Unit): EventReducerBase<E>(inter_period, reduction, handler) {
      private val t = fxTimer(inter_period.millis, 1) { handleTimer() }
      private var last = 0L
      private var fired = false

      private fun handleTimer() {
         if (e is Some) action(e.asIs<Some<E>>().value)
         e = None
         if (fired) t.start()
         fired = false
      }

      protected override fun handle() {
         var now = System.currentTimeMillis()
         if (last==0L) last = now
         var diff = now - last
         last = now

         if (diff>inter_period) {
            if (!t.isRunning) t.start()
            fired = false
            action(e.asIs<Some<E>>().value)
            e = None
         } else {
            if (!t.isRunning) t.start()
            fired = true
         }
      }
   }

   class HandlerFirst<E>(inter_period: Double, handler: (E) -> Unit): EventReducerBase<E>(inter_period, null, handler) {
      private var last: Long = 0

      protected override fun handle() {
         var now = System.currentTimeMillis()
         if (last==0L) last = now
         var diff = now - last
         last = now
         if (diff>inter_period) action(e.asIs<Some<E>>().value)
      }
   }

   class HandlerFirstDelayed<E>(inter_period: Double, handler: (E) -> Unit): EventReducerBase<E>(inter_period, null, handler) {
      private var last: Long = 0
      private val t = fxTimer(inter_period.millis, 1, { action(e.asIs<Some<E>>().value) })

      protected override fun handle() {
         var now = System.currentTimeMillis()
         if (last==0L) last = now
         var diff = now - last
         var isFirst: Boolean = diff>=inter_period
         if (isFirst && !t.isRunning) t.start()

         last = now
      }
   }

   class HandlerFirstOfAtLeast<E>(inter_period: Double, private val atLeast: Double, handler: (E) -> Unit): EventReducerBase<E>(inter_period, null, handler) {
      private var first: Long = 0
      private var last: Long = 0
      private var ran = false

      protected override fun handle() {
         var now = System.currentTimeMillis()
         if (last==0L) last = now
         if (first==0L) last = now
         var diff = now - last
         var isFirst = diff>=inter_period

         if (isFirst) {
            first = now
            ran = false
         }

         var isLongEnough = now - first>=atLeast
         if (isLongEnough && !ran) {
            action(e.asIs<Some<E>>().value)
            ran = true
         }

         last = now
      }
   }

   companion object {
      
      @JvmStatic fun <E> thatConsumes(handler: (E) -> Unit): EventReducer<E> = EventReducer { handler(it) }

      @JvmStatic fun <E> toFirst(inter_period: Double, handler: (E) -> Unit): EventReducer<E> = HandlerFirst(inter_period, handler)

      @JvmStatic fun <E> toFirstDelayed(inter_period: Double, handler: (E) -> Unit): EventReducer<E> = HandlerFirstDelayed(inter_period, handler)

      @JvmStatic fun <E> toLast(inter_period: Double, handler: (E) -> Unit): HandlerLast<E> = HandlerLast(inter_period, null, handler)

      @JvmStatic fun <E> toLast(inter_period: Double, reduction: (E, E) -> E, handler: (E) -> Unit): HandlerLast<E> = HandlerLast(inter_period, reduction, handler)

      @JvmStatic fun <E> toEvery(inter_period: Double, handler: (E) -> Unit): EventReducer<E> = HandlerEvery1(inter_period, { _, b -> b }, handler)

      @JvmStatic fun <E> toEvery(inter_period: Double, reduction: (E, E) -> E, handler: (E) -> Unit): EventReducer<E> = HandlerEvery1(inter_period, reduction, handler)

      @JvmStatic fun <E> toFirstOfAtLeast(inter_period: Double, atLeast: Double, handler: (E) -> Unit): EventReducer<E> = HandlerFirstOfAtLeast(inter_period, atLeast, handler)

   }

}