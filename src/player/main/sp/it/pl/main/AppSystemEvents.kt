package sp.it.pl.main

import sp.it.util.async.runFX
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.system.Os

/** Listener for system events, such as USB add/remove. Only active when subscribed. Supported only on [Os.WINDOWS]. */
class AppSystemEvents {

   fun subscribe(block: (Event) -> Unit): Subscription {
      if (subs.isEmpty()) sysListener.subscribe()
      subs += block
      return Subscription {
         subs -= block
         if (subs.isEmpty()) sysListener.unsubscribe()
      }
   }

   private val subs = Handler1<Event>()
   private var sysListener = Subscribed {
      val l = when (Os.current) {
         Os.WINDOWS -> AppSystemEventsWinListener { runFX { subs(it) } }
         else -> null
      }
      Subscription { l?.dispose() }
   }

   interface SysListener {
      fun dispose()
   }

   sealed interface Event {
      data class FileVolumeAdded(val letter: Char): Event
      data class FileVolumeRemoved(val letter: Char): Event
   }
}