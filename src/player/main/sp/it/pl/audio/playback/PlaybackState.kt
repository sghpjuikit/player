package sp.it.pl.audio.playback

import javafx.scene.media.MediaPlayer.Status
import javafx.util.Duration
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode
import sp.it.util.access.v
import sp.it.util.units.millis
import sp.it.util.units.minus

/** State of playback. */
class PlaybackState {
   val volume = VolumeProperty(VolumeProperty.AVG)
   val loopMode = v(LoopMode.PLAYLIST)
   val status = v(Status.UNKNOWN)
   val duration = v(0.millis)
   val currentTime = v(0.millis)
   val realTime = v(0.millis)
   val realTimeImpl = RealTimeProperty(duration, currentTime)
   val mute = v(false)
   val rate = v(1.0)
   val remainingTime: Duration
      get() = duration.value - currentTime.value

   companion object {
      fun default(): PlaybackState = PlaybackState()
   }

}