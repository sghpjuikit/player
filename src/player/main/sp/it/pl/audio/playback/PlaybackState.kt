package sp.it.pl.audio.playback

import javafx.scene.media.MediaPlayer.Status
import javafx.util.Duration
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode
import sp.it.util.access.v
import sp.it.util.units.millis
import sp.it.util.units.minus

/** State of playback. */
class PlaybackState {
    @JvmField val volume = VolumeProperty(VolumeProperty.AVG)
    @JvmField val loopMode = v(LoopMode.PLAYLIST)
    @JvmField val status = v(Status.UNKNOWN)
    @JvmField val duration = v(0.millis)
    @JvmField val currentTime = v(0.millis)
    @JvmField val realTime = v(0.millis)
    @JvmField val mute = v(false)
    @JvmField val rate = v(1.0)
    val remainingTime: Duration
        get() = duration.value - currentTime.value

    override fun toString() =
        ("Total Time: " + duration.value + ", "
            + "Current Time: " + currentTime.value + ", "
            + "Real Time: " + realTime.value + ", "
            + "Volume: " + volume + ", "
            + "Rate: " + rate + ", "
            + "Mute: " + mute + ", "
            + "Playback Status: " + status.value + ", "
            + "Loop Mode: " + loopMode.value)

    companion object {
        fun default(): PlaybackState = PlaybackState()
    }

}