package sp.it.pl.audio.playback

import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.scene.media.MediaPlayer.Status.STOPPED
import javafx.util.Duration
import javafx.util.Duration.ZERO
import mu.KLogging
import sp.it.pl.audio.Player
import sp.it.pl.audio.PlayerState
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.util.animation.Anim
import sp.it.pl.util.animation.Anim.Companion.anim
import sp.it.pl.util.async.runFX
import sp.it.pl.util.async.runOn
import sp.it.pl.util.file.AudioFileFormat
import sp.it.pl.util.reactive.attach1If
import sp.it.pl.util.units.millis
import java.lang.Math.pow

/** Audio player which abstracts away from the implementation. */
class GeneralPlayer {

    private val state: PlayerState
    private var p: Play? = null
    private val _pInfo = ReadOnlyObjectWrapper<String>("<none>")
    val pInfo = _pInfo.readOnlyProperty!!
    private var i: Song? = null
    val realTime: RealTimeProperty    // TODO: move to state
    private var seekDone = true
    private var lastValidVolume = -1.0
    private var volumeAnim: Anim? = null

    constructor(state: PlayerState) {
        this.state = state
        this.realTime = RealTimeProperty(state.playback.duration, state.playback.currentTime)
    }

    @Synchronized fun play(song: PlaylistSong) {
        // Do not recreate player if same song plays again
        // 1) improves performance
        // 2) avoids firing some playback events
        if (p!=null && song.same(i)) {
            seek(ZERO)
            return
        }

        i = song
        p?.disposePlayback()
        p = p ?: computePlayer()

        _pInfo.value = when (p) {
            null -> "<none>"
            is VlcPlayer -> "VlcPlayer"
            is JavaFxPlayer -> "JavaFX"
            else -> "Unknown"
        }

        val onUnableToPlay = { _: PlaylistSong -> runFX { PlaylistManager.use { it.playNextItem() } } }
        val player = p
        if (player==null) {
            logger.info { "Player=$player can not play song=$song{}" }
            onUnableToPlay(song)
        } else {
            runOn(Player.IO_THREAD) {
                if (song.isCorrupt(AudioFileFormat.Use.PLAYBACK)) {
                    onUnableToPlay(song)
                } else {
                    runFX {
                        player.createPlayback(
                                song,
                                state.playback,
                                {
                                    realTime.realSeek = state.playback.realTime.get()
                                    realTime.currentSeek = ZERO
                                    player.play()

                                    realTime.syncRealTimeOnPlay()
                                    // throw song change event
                                    Player.playingSong.songChanged(song)
                                    Player.suspension_flag = false
                                    // fire other events (may rely on the above)
                                    Player.onPlaybackStart()
                                    if (Player.post_activating_1st || !Player.post_activating)
                                    // bug fix, not updated playlist songs can get here, but should not!
                                        if (song.timeMs>0)
                                            Player.onPlaybackAt.forEach { t -> t.restart(song.time) }
                                    Player.post_activating = false
                                    Player.post_activating_1st = false
                                },
                                { unableToPlayAny ->
                                    logger.info { "Player=$p can not play song=$song" }
                                    if (unableToPlayAny) {
                                        stop()
                                        // TODO: notify user
                                    } else {
                                        onUnableToPlay(song)
                                    }
                                }
                        )
                    }
                }
            }
        }
    }

    private fun computePlayer(): Play = VlcPlayer()

    fun resume() {
        p?.let {
            it.resume()
            Player.onPlaybackAt.forEach { it.unpause() }
        }
    }

    fun pause() {
        p?.let {
            it.pause()
            Player.onPlaybackAt.forEach { it.pause() }
        }
    }

    fun pauseResume() {
        p?.let {
            if (state.playback.status.get()==PLAYING) pause()
            else resume()
        }
    }

    fun stop() {
        p?.let {
            it.stop()
            runFX {
                Player.playingSong.songChanged(Metadata.EMPTY)
                realTime.syncRealTimeOnStop()
                Player.onPlaybackAt.forEach { it.stop() }
                PlaylistManager.playlists.forEach { it.updatePlayingItem(-1) }
                PlaylistManager.active = null
            }
        }
    }

    fun seek(duration: Duration) {
        p?.let {
            val s = state.playback.status.get()

            if (s==STOPPED) pause()

            doSeek(duration)

            // TODO: enable volume fading on seek
            if (false) {
                val currentVolume = state.playback.volume.get()
                if (seekDone)
                    lastValidVolume = currentVolume
                else {
                    if (volumeAnim!=null) volumeAnim!!.pause()
                }
                seekDone = false
                anim(150.millis) { state.playback.volume.set(currentVolume*pow(1-it, 2.0)) }
                        .then {
                            doSeek(duration)
                            volumeAnim = anim(150.millis) { state.playback.volume.set(lastValidVolume*pow(it, 2.0)) }
                                    .then { seekDone = true }
                                    .apply { playOpen() }
                        }
                        .playOpen()
            }
        }
    }

    private fun doSeek(duration: Duration) {
        realTime.syncRealTimeOnPreSeek()
        state.playback.currentTime.value = duration // allow next doSeek() target correct value even if this has not finished
        state.playback.currentTime.attach1If({ it!=null }) { Player.onSeekDone.run() }
        p?.seek(duration)
        realTime.syncRealTimeOnPostSeek(duration)
    }

    fun disposePlayback() {
        p?.disposePlayback()
        p = null
    }

    fun dispose() {
        p?.dispose()
        p = null
    }

    companion object: KLogging()

    interface Play {
        fun play()

        fun pause()

        fun resume()

        fun seek(duration: Duration)

        fun stop()

        fun createPlayback(song: Song, state: PlaybackState, onOK: () -> Unit, onFail: (Boolean) -> Unit)

        /** Stops playback if any and disposes of the player resources. */
        fun disposePlayback()

        fun dispose()
    }

}