package sp.it.pl.audio.playback

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.util.Duration
import javafx.util.Duration.ZERO
import sp.it.util.reactive.attach
import sp.it.util.units.minus
import sp.it.util.units.plus
import sp.it.util.units.seconds

class RealTimeProperty {
   private val totalTime: ObjectProperty<Duration>
   private val currentTime: ObjectProperty<Duration>
   private val realTime: ObjectProperty<Duration>
   var currentSeek: Duration
   var realSeek: Duration

   constructor(totalTime: ObjectProperty<Duration>, currentTime: ObjectProperty<Duration>) {
      this.totalTime = totalTime
      this.currentTime = currentTime
      this.realTime = SimpleObjectProperty(ZERO)
      this.currentSeek = ZERO!!
      this.realSeek = ZERO!!
   }

   fun initialize() {
      currentTime attach { realTime.value = realSeek + currentTime.value - currentSeek }
   }

   fun syncRealTimeOnPlay() {
      realSeek = ZERO
      currentSeek = ZERO
   }

   fun syncRealTimeOnStop() {
      realSeek = ZERO
      currentSeek = ZERO
   }

   fun syncRealTimeOnPreSeek() {
      realSeek = realTime.value.clipSubSeconds()
   }

   fun syncRealTimeOnPostSeek(duration: Duration) {
      currentSeek = duration.clipSubSeconds()
   }

   val value: Duration
      get() = realTime.value

   fun realTimeProperty() = realTime

   companion object {
      fun Duration.clipSubSeconds() = toSeconds().toInt().seconds
   }
}