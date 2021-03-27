package sp.it.pl.ui.objects.seeker

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.util.Duration
import javafx.util.Duration.ONE
import javafx.util.Duration.ZERO
import sp.it.pl.audio.playback.PlaybackState
import sp.it.pl.main.APP
import sp.it.util.animation.Loop
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.sync
import sp.it.util.reactive.zip
import sp.it.util.units.divMillis

/** Observes [PlaybackState] durations to update playback position in 0-1 value range. */
fun bindTimeTo(playback: PlaybackState, smooth: Boolean, block: (Double) -> Unit): Subscription =
   if (smooth) bindTimeToSmooth(playback, block)
   else bindTimeToDiscrete(playback, block)

/** Observes [PlaybackState] durations to update playback position in 0-1 value range in regular macro intervals. */
fun bindTimeToDiscrete(playback: PlaybackState, block: (Double) -> Unit): Subscription =
   playback.currentTime zip playback.duration sync { (c, t) -> block(if (t==null) 0.0 else c.divMillis(t)) }

/** Observes [PlaybackState] durations to update playback position in 0-1 value range on each JavaFX UI pulse. */
fun bindTimeToSmooth(playback: PlaybackState, block: (Double) -> Unit): Subscription {
   var timeTot: ObjectProperty<Duration> = playback.duration
   var timeCur: ObjectProperty<Duration> = playback.currentTime
   var posLast = 0.0
   var posLastFrame: Long = 0
   var posUpdateInterval = 20.0
   var posLastUpdate: Long = 0

   fun timeUpdate() {
      if (timeTot.value==null) return  // bug fix
      posLast = timeCur.value.divMillis(timeTot.value)
      posLastFrame = 0
      posUpdateInterval = 0.0
      block(posLast)
   }

   fun timeUpdateDo(frame: Long) {
      if (APP.audio.state.playback.status.value==PLAYING) {
         val dt = if (posLastFrame==0L) 0 else (frame - posLastFrame)/1000000
         val dp = dt/timeTot.get().toMillis()
         posLast += dp
         val now = System.currentTimeMillis()
         if (now - posLastUpdate>posUpdateInterval) {
            posLastUpdate = now
            block(posLast)
         }
      }
      posLastFrame = frame
   }

   val timeUpdater = ChangeListener<Duration> { _: ObservableValue<*>, _: Any, _: Any -> timeUpdate() }
   val timeLoop = Loop { frame: Long -> timeUpdateDo(frame) }

   timeTot.removeListener(timeUpdater)
   timeCur.removeListener(timeUpdater)
   timeTot.addListener(timeUpdater)
   timeCur.addListener(timeUpdater)
   timeUpdater.changed(null, ZERO, ZERO)
   timeLoop.start()
   return Subscription {
      timeLoop.stop()
      timeTot.unbind()
      timeCur.unbind()
      timeTot.removeListener(timeUpdater)
      timeCur.removeListener(timeUpdater)
      timeTot = SimpleObjectProperty(ONE)
      timeCur = SimpleObjectProperty(ONE)
      timeUpdater.changed(null, null, null)
   }

}