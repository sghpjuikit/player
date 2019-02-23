package sp.it.pl.audio.playback

import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.media.MediaPlayer.Status
import javafx.util.Duration
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode
import sp.it.pl.util.units.minus
import java.util.Objects
import java.util.UUID

/** State of playback. */
class PlaybackState(id: UUID) {
    var id: UUID            // TODO: this should be immutable
        internal set
    @JvmField val volume: VolumeProperty
    @JvmField val balance: BalanceProperty
    @JvmField val loopMode: ObjectProperty<LoopMode>
    @JvmField val status: ObjectProperty<Status>
    @JvmField val duration: ObjectProperty<Duration>
    @JvmField val currentTime: ObjectProperty<Duration>
    @JvmField val realTime: ObjectProperty<Duration>
    @JvmField val mute: BooleanProperty
    @JvmField val rate: DoubleProperty
    val remainingTime: Duration
        get() = duration.get()-currentTime.get()

    init {
        this.id = id
        volume = VolumeProperty(VolumeProperty.AVG)
        balance = BalanceProperty(0.0)
        loopMode = SimpleObjectProperty<LoopMode>(LoopMode.PLAYLIST)
        status = SimpleObjectProperty<Status>(Status.UNKNOWN)
        duration = SimpleObjectProperty(Duration.ZERO)
        currentTime = SimpleObjectProperty(Duration.ZERO)
        realTime = SimpleObjectProperty(Duration.ZERO)
        mute = SimpleBooleanProperty(false)
        rate = SimpleDoubleProperty(1.0)
    }

    private constructor(): this(UUID.randomUUID())

    /** Changes this state's property values to that of another state. Use this to switch between multiple states. */
    fun change(to: PlaybackState?) {
        if (to!=null) {
            id = to.id                          // TODO: not good
            volume.set(to.volume.get())
            balance.set(to.balance.get())
            loopMode.set(to.loopMode.get())
            status.unbind()                      // TODO: this is wrong
            status.set(to.status.get())
            duration.unbind()
            duration.set(to.duration.get())
            currentTime.unbind()
            currentTime.set(to.currentTime.get())
            realTime.set(to.realTime.get())
            mute.set(to.mute.get())
            rate.set(to.rate.get())
        }
    }

    override fun equals(other: Any?) = if (this===other) true else other is PlaybackState && id==other.id

    override fun hashCode() = 43*3+Objects.hashCode(id)

    override fun toString() =
            ("Id: "+id+", "
            +"Total Time: "+duration.value+", "
            +"Current Time: "+currentTime.value+", "
            +"Real Time: "+realTime.value+", "
            +"Volume: "+volume+", "
            +"Balance: "+balance+", "
            +"Rate: "+rate+", "
            +"Mute: "+mute+", "
            +"Playback Status: "+status.value+", "
            +"Loop Mode: "+loopMode.value)

    companion object {
        fun default(): PlaybackState = PlaybackState()
    }

}