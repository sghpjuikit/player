package sp.it.pl.audio.playback

import javafx.scene.media.MediaPlayer.Status
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.util.Duration
import mu.KLogging
import sp.it.pl.audio.Item
import sp.it.pl.audio.Player
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.async.runAfter
import sp.it.pl.util.async.runFX
import sp.it.pl.util.file.childOf
import sp.it.pl.util.functional.ifFalse
import sp.it.pl.util.functional.onE
import sp.it.pl.util.functional.runTry
import sp.it.pl.util.math.millis
import sp.it.pl.util.reactive.Disposer
import sp.it.pl.util.reactive.sync
import uk.co.caprica.vlcj.discovery.NativeDiscovery
import uk.co.caprica.vlcj.discovery.linux.DefaultLinuxNativeDiscoveryStrategy
import uk.co.caprica.vlcj.discovery.mac.DefaultMacNativeDiscoveryStrategy
import uk.co.caprica.vlcj.discovery.windows.DefaultWindowsNativeDiscoveryStrategy
import uk.co.caprica.vlcj.player.MediaPlayer
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.MediaPlayerFactory
import uk.co.caprica.vlcj.player.media.simple.SimpleMedia
import java.io.File
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
                isSeekToZero -> runAfter(millis(10)) {
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

    override fun createPlayback(item: Item, state: PlaybackState, onOK: () -> Unit, onFail: () -> Unit) {
        if (!initialized && !discovered) {
            val location = APP.DIR_APP.childOf("vlc")
            discovered = true
            initialized = discoverVlc(location).ifFalse { logger.error { "Failed to initialize vlcj player" } }
        }
        if (!initialized) {
            onFail()
            return
        }

        val p = mediaPlayerFactory.newHeadlessMediaPlayer()
        player = p

        d += state.volume sync { p.volume = (100*it.toDouble()).roundToInt() }
        d += state.mute sync { p.mute(it) }
        d += state.balance sync { }         // TODO: implement
        d += state.rate sync { p.rate = it.toFloat() }

        p.prepareMedia(SimpleMedia(item.uriAsVlcPath()))
        p.addMediaPlayerEventListener(object: MediaPlayerEventAdapter() {

            override fun lengthChanged(mediaPlayer: MediaPlayer?, newLength: Long) {
                runFX {
                    state.duration.value = millis(newLength.toDouble())
                }
            }

            override fun positionChanged(mediaPlayer: MediaPlayer, newPosition: Float) {
                runFX {
                    state.currentTime.value = millis(state.duration.value.toMillis()*newPosition.toDouble())
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
                        Player.state.playback.status.set(Status.STOPPED)
                }
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                runFX {
                    if (!Player.suspension_flag)
                        Player.state.playback.status.set(Status.PAUSED)

                    if (Player.startTime!=null) {
                        seek(Player.startTime)
                        Player.startTime = null
                    }
                }
            }

            override fun playing(mediaPlayer: MediaPlayer) {
                runFX {
                    if (!Player.suspension_flag)
                        Player.state.playback.status.set(Status.PLAYING)

                    if (Player.startTime!=null) {
                        seek(Player.startTime)
                        Player.startTime = null
                    }
                }
            }

        })

        val s = state.status.get()
        if (Player.startTime!=null) {
            if (s==PLAYING) play()
            else if (s==PAUSED) pause()
        }

        onOK()
    }

    override fun disposePlayback() {
        d()
        runTry { player?.release() } onE { logger.error(it) { "Failed to dispose of the player" } }
        player = null
    }

    override fun dispose() {}

    companion object: KLogging() {

        private fun discoverVlc(location: File): Boolean {
            val loc = location.absolutePath
            return NativeDiscovery(WindowsDiscoverer(loc), LinuxDiscoverer(loc), MacDiscoverer(loc)).discover()
        }

        private fun Item.uriAsVlcPath() = uri.toASCIIString().let {
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
                directoryNames.addAll(locations)
            }
        }

        class LinuxDiscoverer(vararg val locations: String): DefaultLinuxNativeDiscoveryStrategy() {
            override fun onGetDirectoryNames(directoryNames: MutableList<String>) {
                super.onGetDirectoryNames(directoryNames)
                directoryNames.addAll(locations)
                // temporary fix until https://github.com/caprica/vlcj/pull/612 is merged
                directoryNames.addAll(arrayOf("/usr/lib64", "/usr/local/lib64", "/usr/lib/x86_64-linux-gnu", "/usr/lib/i386-linux-gnu"))
            }
        }

        class MacDiscoverer(vararg val locations: String): DefaultMacNativeDiscoveryStrategy() {
            override fun onGetDirectoryNames(directoryNames: MutableList<String>) {
                super.onGetDirectoryNames(directoryNames)
                directoryNames.addAll(locations)
            }
        }

    }

}