package sp.it.pl.audio.playback

import javafx.scene.media.MediaPlayer.Status
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.util.Duration
import mu.KLogging
import sp.it.pl.audio.Player
import sp.it.pl.audio.Song
import sp.it.pl.main.APP
import sp.it.pl.util.async.runFX
import sp.it.pl.util.async.runNew
import sp.it.pl.util.file.div
import sp.it.pl.util.functional.ifFalse
import sp.it.pl.util.functional.onE
import sp.it.pl.util.functional.runTry
import sp.it.pl.util.reactive.Disposer
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.units.millis
import sp.it.pl.util.units.times
import uk.co.caprica.vlcj.discovery.NativeDiscovery
import uk.co.caprica.vlcj.discovery.linux.DefaultLinuxNativeDiscoveryStrategy
import uk.co.caprica.vlcj.discovery.mac.DefaultMacNativeDiscoveryStrategy
import uk.co.caprica.vlcj.discovery.windows.DefaultWindowsNativeDiscoveryStrategy
import uk.co.caprica.vlcj.player.MediaPlayer
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.MediaPlayerFactory
import uk.co.caprica.vlcj.player.events.MediaPlayerEventType
import uk.co.caprica.vlcj.player.media.simple.SimpleMedia
import java.io.File
import java.lang.Thread.sleep
import javax.print.attribute.standard.PrinterStateReason.PAUSED
import kotlin.math.roundToInt


class VlcPlayer: GeneralPlayer.Play {

    private var initialized = false
    private var discovered = false
    private val mediaPlayerFactory by lazy { MediaPlayerFactory() }
    private var player: MediaPlayer? = null
    private val d = Disposer()

    override fun play() {
        player?.play()
    }

    override fun pause() {
        player?.pause()
    }

    override fun resume() {
        player?.play()
    }

    override fun seek(duration: Duration) {
        player?.let {
            val isSeekToZero = duration.toMillis()<=0
            val isSeekImpossible = it.length==-1L
            when {
                // TODO: fix #38 better than delaying
                isSeekToZero -> runFX(10.millis) {
                    it.play()
                    it.position = 0f
                }
                isSeekImpossible -> {
                    it.play()
                    it.position = 0f
                }
                else -> it.position = (duration.toMillis().toFloat()/it.length.toFloat()).coerceIn(0f..1f)
            }
        }
    }

    override fun stop() {
        player?.stop()
    }

    override fun createPlayback(song: Song, state: PlaybackState, onOK: () -> Unit, onFail: (Boolean) -> Unit) {
        if (!initialized && !discovered) {
            val location = APP.DIR_APP/"vlc"
            discovered = true
            initialized = discoverVlc(location).ifFalse { logger.error { "Failed to initialize vlcj player" } }
        }
        if (!initialized) {
            onFail(true)
            return
        }

        val p = mediaPlayerFactory.newHeadlessMediaPlayer()
        player = p

        d += state.volume sync { p.volume = (100*it.toDouble()).roundToInt() }
        d += state.mute sync { p.mute(it) }
        d += state.balance sync { }         // TODO: implement
        d += state.rate sync { p.rate = it.toFloat() }

        p.prepareMedia(SimpleMedia(song.uriAsVlcPath()))

        val eventMask = sequenceOf(
                MediaPlayerEventType.LENGTH_CHANGED.value(),
                MediaPlayerEventType.POSITION_CHANGED.value(),
                MediaPlayerEventType.FINISHED.value(),
                MediaPlayerEventType.STOPPED.value(),
                MediaPlayerEventType.PAUSED.value(),
                MediaPlayerEventType.PLAYING.value()
        ).fold(0L) { events, event ->
            events or event
        }
        p.enableEvents(eventMask)

        val playbackListener = createPlaybackListener(state)
        p.addMediaPlayerEventListener(playbackListener)
        d += { p.removeMediaPlayerEventListener(playbackListener) }

        if (Player.startTime!=null) {
            when (state.status.value) {
                PLAYING -> play()
                PAUSED -> pause()
                else -> {}
            }
        }

        onOK()
    }

    private fun createPlaybackListener(state: PlaybackState) = object: MediaPlayerEventAdapter() {

        override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
            runFX {
                state.duration.value = newLength.millis
            }
        }

        override fun positionChanged(mediaPlayer: MediaPlayer, newPosition: Float) {
            runFX {
                state.currentTime.value = state.duration.value*newPosition
            }
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            runFX {
                Player.onPlaybackEnd()
            }
        }

        override fun stopped(mediaPlayer: MediaPlayer) {
            runFX {
                if (!Player.suspension_flag)
                    state.status.value = Status.STOPPED
            }
        }

        override fun paused(mediaPlayer: MediaPlayer) {
            runFX {
                if (!Player.suspension_flag)
                    state.status.value = Status.PAUSED

                if (Player.startTime!=null) {
                    seek(Player.startTime)
                    Player.startTime = null
                }
            }
        }

        override fun playing(mediaPlayer: MediaPlayer) {
            runFX {
                if (!Player.suspension_flag)
                    state.status.value = PLAYING

                if (Player.startTime!=null) {
                    seek(Player.startTime)
                    Player.startTime = null
                }
            }
        }

    }

    override fun disposePlayback() {
        d()
        player?.stop()
        player?.let { p ->
            runTry {
                p.release()
            } onE {
                logger.warn(it) { "Failed to dispose of the player. Will retry after 1 second." }
                // https://stackoverflow.com/questions/43312679/jvm-crashes-when-releasing-a-vlcj-mediaplayer
                runNew {
                    sleep(1000)
                    runTry {
                        p.release()
                    } onE {
                        logger.error(it) { "Failed to dispose of the player" }
                    }
                }
            }
        }
        player = null
    }

    override fun dispose() {}

    companion object: KLogging() {

        private fun discoverVlc(location: File): Boolean {
            val loc = location.absolutePath
            return NativeDiscovery(WindowsDiscoverer(loc), LinuxDiscoverer(loc), MacDiscoverer(loc)).discover()
        }

        private fun Song.uriAsVlcPath() = uri.toASCIIString().let {
            // vlcj bug
            // https://github.com/sghpjuikit/player/issues/40
            // https://github.com/caprica/vlcj/issues/173
            // https://github.com/caprica/vlcj/issues/424
            if (it.startsWith("file:/") && it[6]!='/')
                it.replaceFirst("file:/", "file:///")
            else it
        }

        class WindowsDiscoverer(vararg val locations: String): DefaultWindowsNativeDiscoveryStrategy() {
            override fun onGetDirectoryNames(directoryNames: MutableList<String>) {
                super.onGetDirectoryNames(directoryNames)
                directoryNames += locations
            }
        }

        class LinuxDiscoverer(vararg val locations: String): DefaultLinuxNativeDiscoveryStrategy() {
            override fun onGetDirectoryNames(directoryNames: MutableList<String>) {
                super.onGetDirectoryNames(directoryNames)
                directoryNames += locations
            }
        }

        class MacDiscoverer(vararg val locations: String): DefaultMacNativeDiscoveryStrategy() {
            override fun onGetDirectoryNames(directoryNames: MutableList<String>) {
                super.onGetDirectoryNames(directoryNames)
                directoryNames += locations
            }
        }

    }

}