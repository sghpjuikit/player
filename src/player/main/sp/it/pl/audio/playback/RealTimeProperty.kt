package sp.it.pl.audio.playback

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.util.Duration
import javafx.util.Duration.ZERO
import sp.it.util.reactive.attach
import sp.it.util.units.minus
import sp.it.util.units.plus

class RealTimeProperty {
   private val totalTime: ObjectProperty<Duration>
   private val currentTime: ObjectProperty<Duration>
   private val realTime: ObjectProperty<Duration>
   @JvmField var currentSeek: Duration
   @JvmField var realSeek: Duration

   constructor(totalTime: ObjectProperty<Duration>, currentTime: ObjectProperty<Duration>) {
      this.totalTime = totalTime
      this.currentTime = currentTime
      this.realTime = SimpleObjectProperty(ZERO)
      this.currentSeek = ZERO!!
      this.realSeek = ZERO!!
   }

   fun initialize() {
      currentTime attach { realTime.value = realSeek + currentTime.get() - currentSeek }
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
      realSeek = realTime.get()
   }

   fun syncRealTimeOnPostSeek(duration: Duration) {
      currentSeek = duration
   }

   fun get() = realTime.get()!!

   fun realTimeProperty() = realTime

}