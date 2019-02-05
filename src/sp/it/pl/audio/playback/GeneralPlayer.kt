package sp.it.pl.audio.playback

import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.scene.media.MediaPlayer.Status.STOPPED
import javafx.util.Duration
import javafx.util.Duration.ZERO
import mu.KLogging
import sp.it.pl.audio.Item
import sp.it.pl.audio.Player
import sp.it.pl.audio.PlayerState
import sp.it.pl.audio.playlist.PlaylistItem
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.util.animation.Anim
import sp.it.pl.util.animation.Anim.Companion.anim
import sp.it.pl.util.async.runFX
import sp.it.pl.util.async.runOn
import sp.it.pl.util.file.AudioFileFormat
import sp.it.pl.util.math.millis
import sp.it.pl.util.reactive.attach1If
import java.lang.Math.pow

/** Audio player which abstracts away from the implementation. */
class GeneralPlayer {

    private val state: PlayerState
    private var p: Play? = null
    private val _pInfo = ReadOnlyObjectWrapper<String>("<none>")
    val pInfo = _pInfo.readOnlyProperty!!
    private var i: Item? = null
    val realTime: RealTimeProperty    // TODO: move to state
    private var seekDone = true
    private var lastValidVolume = -1.0
    private var volumeAnim: Anim? = null

    constructor(state: PlayerState) {
        this.state = state
        this.realTime = RealTimeProperty(state.playback.duration, state.playback.currentTime)
    }

    @Synchronized fun play(item: PlaylistItem) {
        // Do not recreate player if same song plays again
        // 1) improves performance
        // 2) avoids firing some playback events
        if (p!=null && item.same(i)) {
            seek(ZERO)
            return
        }

        i = item
        p?.disposePlayback()
        p = computePlayer(item)

        _pInfo.value = when (p) {
            null -> "<none>"
            is VlcPlayer -> "VlcPlayer"
            is JavaFxPlayer -> "JavaFX"
            else -> "Unknown"
        }

        val onUnableToPlay = { it: PlaylistItem ->
            runFX {
                it.playbackError = true
                // TODO: handle within playlist
                PlaylistManager.use { it.playNextItem() }
            }
        }
        val player = p
        if (player==null) {
            logger.info("Player {} can not play item {}", player, item)
            onUnableToPlay(item)
        } else {
            runOn(Player.IO_THREAD) {
                if (item.isCorrupt(AudioFileFormat.Use.PLAYBACK)) {
                    onUnableToPlay(item)
                } else {
                    runFX {
                        player.createPlayback(
                                item,
                                state.playback,
                                {
                                    realTime.realSeek = state.playback.realTime.get()
                                    realTime.currentSeek = ZERO
                                    player.play()

                                    realTime.syncRealTimeOnPlay()
                                    // throw item change event
                                    Player.playingItem.itemChanged(item)
                                    Player.suspension_flag = false
                                    // fire other events (may rely on the above)
                                    Player.onPlaybackStart()
                                    if (Player.post_activating_1st || !Player.post_activating)
                                    // bug fix, not updated playlist items can get here, but should not!
                                        if (item.timeMs>0)
                                            Player.onPlaybackAt.forEach { t -> t.restart(item.time) }
                                    Player.post_activating = false
                                    Player.post_activating_1st = false
                                },
                                { unableToPlayAny ->
                                    logger.info("Player {} can not play item {}", p, item)
                                    if (unableToPlayAny) {
                                        stop()
                                        // TODO: notify user
                                    } else {
                                        onUnableToPlay(item)
                                    }
                                }
                        )
                    }
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun computePlayer(i: Item): Play = p ?: VlcPlayer()

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
                Player.playingItem.itemChanged(Metadata.EMPTY)
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

    fun dispose() {
        p?.let {
            it.disposePlayback()
            it.dispose()
        }
        p = null
    }

    companion object: KLogging()

    interface Play {
        fun play()

        fun pause()

        fun resume()

        fun seek(duration: Duration)

        fun stop()

        fun createPlayback(item: Item, state: PlaybackState, onOK: () -> Unit, onFail: (Boolean) -> Unit)

        /** Stops playback if any and disposes of the player resources. */
        fun disposePlayback()

        fun dispose()
    }

}