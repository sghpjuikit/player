package sp.it.pl.audio.playback

import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Side
import javafx.scene.media.MediaPlayer.Status.PAUSED
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.scene.media.MediaPlayer.Status.STOPPED
import javafx.stage.WindowEvent.WINDOW_HIDDEN
import javafx.util.Duration
import mu.KLogging
import sp.it.pl.audio.Song
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.IconFA
import sp.it.pl.main.ifErrorNotify
import sp.it.pl.main.withAppProgress
import sp.it.util.async.FX
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.dev.fail
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.Util.saveFileAs
import sp.it.util.file.div
import sp.it.util.file.unzip
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.system.Os
import sp.it.util.ui.lay
import sp.it.util.ui.text
import sp.it.util.ui.vBox
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
import java.io.IOException
import java.net.URI
import kotlin.math.roundToInt


class VlcPlayer: GeneralPlayer.Play {

   @Volatile private var playerFactory: MediaPlayerFactory? = null
   @Volatile private var player: MediaPlayer? = null
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
      val pf = playerFactory ?: try {
         MediaPlayerFactory(
            discoverVlc(APP.audio.playerVlcLocations),
            "--quiet", "--intf=dummy", "--novideo", "--no-metadata-network-access"
         )
      } catch(e: Throwable) {
         null
      }
      playerFactory = pf

      if (pf==null || APP.audio.playerVlcLocation.value==null) {
         logger.info { "Playback creation failed: Vlc not available" }
         onFail(true)
         VlcSetup.askForDownload()
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

   private object VlcSetup {
      private var askForDownload = true

      // TODO: document
//      - (optionally) set up latest 64-bit `VLC` using one of:
//      - make `app/vlc` contain/link to (optionally portable) installation of `VLC`
//      This is the recommended way, as the application does not depend on external `VLC` version or location
//      - let application discover your `VLC` automatically
//      This requires `VLC` to be installed.
//      Note that updating or changing this `VLC` may interfere with this application
//      - add `VLC` location in application settings in `Settings > Playback > Vlc player locations`
//      This is recommended if you do have `VLC` available, but not installed or integrated in your system
//      Note that updating or changing this `VLC` may interfere with this application
      fun askForDownload() {
         if (!askForDownload) return
         askForDownload = false
         APP.windowManager.showFloating("Vlc Setup") { p ->
            vBox(24.0, CENTER) {
               lay += text("Viable Vlc player has not been found on the system")
               lay += vBox(0.0, CENTER_LEFT) {
                  lay += Icon(IconFA.CIRCLE).apply { isDisable = !Os.WINDOWS.isCurrent }
                     .onClickDo {
                        setup() ui { p.hide() }
                     }
                     .withText(Side.RIGHT, CENTER_LEFT, "I want this application to automatically download and set up private, local vlc (recommended)")
                  lay += Icon(IconFA.CIRCLE)
                     .onClickDo {
                        p.hide()
                     }
                     .withText(Side.RIGHT, CENTER_LEFT, "I will install Vlc on my own")
                  lay += Icon(IconFA.CIRCLE)
                     .onClickDo {
                        p.onEventDown(WINDOW_HIDDEN) {
                           APP.actions.openSettings()
                           // TODO: highlight proper settings
                        }
                        p.hide()
                     }
                     .withText(Side.RIGHT, CENTER_LEFT, "I already have Vlc")
               }
            }
         }
      }

      private val setup by lazy {
         runIO {
            val os = Os.current
            val vlcDir = APP.location/"vlc"
            val vlcZip = vlcDir/"vlc.zip"
            val vlcVersion = "3.0.8"
            val vlcLink = URI(
               when (os) {
                  Os.WINDOWS -> "http://download.videolan.org/pub/videolan/vlc/3.0.8/win64/vlc-$vlcVersion-win64.zip"
                  else -> fail { "Vlc auto-setup is not supported on $os" }
               }
            )

            fun Boolean.orFailIO(message: () -> String) = also { if (!this) throw IOException(message()) }

            if (vlcDir.exists()) vlcDir.deleteRecursively().orFailIO { "Failed to remove Vlc in=$vlcDir" }
            saveFileAs(vlcLink.toString(), vlcZip)
            vlcZip.unzip(vlcDir) { it.substringAfter("vlc-$vlcVersion/") }
            vlcZip.delete().orFailIO { "Failed to clean up downloaded file=$vlcZip" }
         }.withAppProgress("Obtaining Vlc").onDone(FX) {
            it.toTry().ifErrorNotify {
               AppError("Failed to obtain Vlc", "Reason: ${it.stacktraceAsString}")
            }
         }
      }

      fun setup() = setup

   }

   companion object: KLogging() {

      private fun discoverVlc(locations: List<File>) = NativeDiscovery(
         WindowsNativeDiscoveryStrategy().customize(locations),
         LinuxNativeDiscoveryStrategy().customize(locations),
         OsxNativeDiscoveryStrategy().customize(locations),
         WindowsNativeDiscoveryStrategy().wrap(),
         LinuxNativeDiscoveryStrategy().wrap(),
         OsxNativeDiscoveryStrategy().wrap(),
         WindowsNativeDiscoveryStrategy().customize(listOf(APP.location/"vlc")),
         LinuxNativeDiscoveryStrategy().customize(listOf(APP.location/"vlc")),
         OsxNativeDiscoveryStrategy().customize(listOf(APP.location/"vlc"))
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