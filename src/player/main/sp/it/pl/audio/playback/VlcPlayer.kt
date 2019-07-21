package sp.it.pl.audio.playback

import javafx.scene.media.MediaPlayer.Status.PAUSED
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.scene.media.MediaPlayer.Status.STOPPED
import javafx.util.Duration
import mu.KLogging
import sp.it.pl.audio.Song
import sp.it.pl.main.APP
import sp.it.util.async.runFX
import sp.it.util.dev.fail
import sp.it.util.file.div
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.system.Os
import sp.it.util.units.millis
import sp.it.util.units.times
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.factory.discovery.strategy.BaseNativeDiscoveryStrategy
import uk.co.caprica.vlcj.factory.discovery.strategy.LinuxNativeDiscoveryStrategy
import uk.co.caprica.vlcj.factory.discovery.strategy.NativeDiscoveryStrategy
import uk.co.caprica.vlcj.factory.discovery.strategy.OsxNativeDiscoveryStrategy
import uk.co.caprica.vlcj.factory.discovery.strategy.WindowsNativeDiscoveryStrategy
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.io.File
import kotlin.math.roundToInt


class VlcPlayer: GeneralPlayer.Play {

   private var playerFactory: MediaPlayerFactory? = null
   private var player: MediaPlayer? = null
   private val d = Disposer()

   override fun play() {
      player?.controls()?.play()
   }

   override fun pause() {
      player?.controls()?.pause()
   }

   override fun resume() {
      player?.controls()?.play()
   }

   override fun seek(duration: Duration) {
      player?.let {
         val isSeekToZero = duration.toMillis()<=0
         val isSeekImpossible = it.status().length()==-1L
         if (isSeekToZero || isSeekImpossible) {
            it.controls().play()
            it.controls().setPosition(0f)
         } else {
            it.controls().setPosition((duration.toMillis().toFloat()/it.status().length().toFloat()).coerceIn(0f..1f))
         }
      }
   }

   override fun stop() {
      player?.controls()?.stop()
   }

   override fun createPlayback(song: Song, state: PlaybackState, onOK: () -> Unit, onFail: (Boolean) -> Unit) {
      val pf = playerFactory
         ?: MediaPlayerFactory(
            discoverVlc(listOf(APP.location/"vlc") + APP.audio.playerVlcLocations),
            "--quiet", "--intf=dummy", "--novideo", "--no-metadata-network-access"
         )
      playerFactory = pf

      if (APP.audio.playerVlcLocation.value==null) {
         logger.info { "Playback creation failed: No vlc discovered" }
         onFail(true)
         return
      }

      val p = pf.mediaPlayers().newMediaPlayer()
      player = p

      state.volume sync { p.audio().setVolume((100*it.toDouble()).roundToInt()) } on d
      state.mute sync { p.audio().isMute = it } on d
      state.rate sync { p.controls().setRate(it.toFloat()) } on d

      p.media().prepare(song.uriAsVlcPath())

      val playbackListener = createPlaybackListener(state)
      p.events().addMediaPlayerEventListener(playbackListener)
      d += { p.events().removeMediaPlayerEventListener(playbackListener) }

      if (APP.audio.startTime!=null) {
         when (state.status.value) {
            PLAYING -> play()
            else -> Unit
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
            APP.audio.onPlaybackEnd()
         }
      }

      override fun stopped(mediaPlayer: MediaPlayer) {
         runFX {
            if (!APP.audio.isSuspended)
               state.status.value = STOPPED
         }
      }

      override fun paused(mediaPlayer: MediaPlayer) {
         runFX {
            if (!APP.audio.isSuspended)
               state.status.value = PAUSED

            APP.audio.startTime?.let { seek(it) }
            APP.audio.startTime = null
         }
      }

      override fun playing(mediaPlayer: MediaPlayer) {
         runFX {
            if (!APP.audio.isSuspended)
               state.status.value = PLAYING

            APP.audio.startTime?.let { seek(it) }
            APP.audio.startTime = null
         }
      }

   }

   override fun disposePlayback() {
      d()
      player?.controls()?.stop()
      player?.release()
      player = null
   }

   override fun dispose() {
      playerFactory?.release()
      playerFactory = null
   }

   companion object: KLogging() {

      private fun discoverVlc(locations: List<File>) = NativeDiscovery(
         WindowsNativeDiscoveryStrategy().customize(locations),
         LinuxNativeDiscoveryStrategy().customize(locations),
         OsxNativeDiscoveryStrategy().customize(locations),
         WindowsNativeDiscoveryStrategy().wrap(),
         LinuxNativeDiscoveryStrategy().wrap(),
         OsxNativeDiscoveryStrategy().wrap()
      )

      private fun NativeDiscoveryStrategy.wrap() = NativeDiscoveryStrategyWrapper(this)

      @Suppress("SpellCheckingInspection")
      private fun NativeDiscoveryStrategy.customize(locations: List<File>) = object: BaseNativeDiscoveryStrategy(
         when (this@customize) {
            is WindowsNativeDiscoveryStrategy -> arrayOf("libvlc\\.dll", "libvlccore\\.dll")
            is LinuxNativeDiscoveryStrategy -> arrayOf("libvlc\\.so(?:\\.\\d)*", "libvlccore\\.so(?:\\.\\d)*")
            is OsxNativeDiscoveryStrategy -> arrayOf("libvlc\\.dylib", "libvlccore\\.dylib")
            else -> fail { "Invalid discovery strategy" }
         },
         when (this@customize) {
            is WindowsNativeDiscoveryStrategy -> arrayOf("%s\\plugins", "%s\\vlc\\plugins")
            is LinuxNativeDiscoveryStrategy -> arrayOf("%s/plugins", "%s/vlc/plugins")
            is OsxNativeDiscoveryStrategy -> arrayOf("%s/../plugins")
            else -> fail { "Invalid discovery strategy" }
         }
      ) {
         override fun supported() = this@customize.supported()
         override fun setPluginPath(pluginPath: String?) = this@customize.onSetPluginPath(pluginPath)
         override fun discoveryDirectories() = locations.map { APP.audio.playerVlcLocationsRelativeTo.resolve(it).path }

         override fun onFound(path: String?): Boolean {
            APP.audio.playerVlcLocation.value = "Custom: $path"
            return super.onFound(path)
         }
      }

      class NativeDiscoveryStrategyWrapper(private val discoverer: NativeDiscoveryStrategy): NativeDiscoveryStrategy by discoverer {
         override fun onFound(path: String?): Boolean {
            APP.audio.playerVlcLocation.value = "Installed in system (${Os.current}) as $path"
            return discoverer.onFound(path)
         }
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

   }

}