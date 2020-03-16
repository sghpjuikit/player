package sp.it.pl.audio.playback

import javafx.beans.InvalidationListener
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.media.MediaPlayer.Status
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.util.Duration
import javafx.util.Duration.ZERO
import sp.it.util.reactive.attach
import sp.it.util.units.millis

class RealTimeProperty: ObservableValue<Duration> {
   private val realTime = SimpleObjectProperty(ZERO)
   private var realSeekMs: Long = 0L
   private var lastContinueTimeAt: Long = 0L

   constructor(status: ObjectProperty<Status>, currentTime: ObjectProperty<Duration>) {
      realSeekMs = 0
      realTime.value = ZERO

      status attach {
         when (it) {
            PLAYING -> lastContinueTimeAt = System.currentTimeMillis()
            else -> realSeekMs += (System.currentTimeMillis() - lastContinueTimeAt)
         }
      }
      currentTime attach {
         realTime.value = (realSeekMs + System.currentTimeMillis() - lastContinueTimeAt).millis
      }
   }

   fun syncRealTimeOnPlay() {
      realSeekMs = 0
      lastContinueTimeAt = System.currentTimeMillis()
      realTime.value = ZERO
   }

   override fun removeListener(listener: ChangeListener<in Duration>) = realTime.removeListener(listener)

   override fun removeListener(listener: InvalidationListener) = realTime.removeListener(listener)

   override fun addListener(listener: ChangeListener<in Duration>) = realTime.addListener(listener)

   override fun addListener(listener: InvalidationListener) = realTime.addListener(listener)

   override fun getValue(): Duration = realTime.value

}