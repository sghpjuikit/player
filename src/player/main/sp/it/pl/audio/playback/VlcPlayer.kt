package sp.it.pl.audio.playback

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.net.URI
import javafx.geometry.Pos.TOP_CENTER
import javafx.scene.media.MediaPlayer.Status.PAUSED
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.scene.media.MediaPlayer.Status.STOPPED
import javafx.util.Duration
import kotlin.math.roundToInt
import kotlinx.coroutines.invoke
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode.OFF
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode.PLAYLIST
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode.SONG
import sp.it.pl.main.APP
import sp.it.pl.main.Actions.APP_SEARCH
import sp.it.pl.main.AppError
import sp.it.pl.main.AppProgress
import sp.it.pl.main.bullet
import sp.it.pl.main.configure
import sp.it.pl.main.downloadFile
import sp.it.pl.main.ifErrorNotify
import sp.it.pl.main.reportFor
import sp.it.pl.main.showFloating
import sp.it.pl.main.toUi
import sp.it.util.action.ActionRegistrar
import sp.it.util.async.coroutine.VT
import sp.it.util.async.coroutine.runSuspendingFx
import sp.it.util.async.runFX
import sp.it.util.conf.getDelegateConfig
import sp.it.util.dev.fail
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.del
import sp.it.util.file.deleteRecursivelyOrThrow
import sp.it.util.file.div
import sp.it.util.file.unzip
import sp.it.util.functional.ifNotNull
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.reactive.zip
import sp.it.util.system.Os
import sp.it.util.ui.label
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
import uk.co.caprica.vlcj.media.MediaRef
import uk.co.caprica.vlcj.media.TrackType
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener

class VlcPlayer: GeneralPlayer.Play {

   @Volatile var playerFactory: MediaPlayerFactory? = null
   @Volatile private var player: MediaPlayer? = null
   private val d = Disposer()

   override val canDoSongRepeat = true

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
            ).apply {
               playerFactory = this
            }
         } catch (e: Throwable) {
            null
         }

         if (pf==null || APP.audio.playerVlcLocation.value==null) {
            logger.info { "Playback creation failed: Vlc not available" }
            onFail(true)
            VlcSetup.askForDownload()
            return
         }

         val p = pf.mediaPlayers().newMediaPlayer().apply {
            player = this
         }

         // set volume
         state.volume zip state.volumeFadeMultiplier sync { (v, vm) -> p.audio().setVolume((100*v.toDouble()*vm.toDouble()).roundToInt()) } on d
         state.mute sync { p.audio().isMute = it } on d
         // set rate
         state.rate sync { p.controls().setRate(it.toFloat()) } on d
         // set channel, can be done during playback
         APP.audio.audioChannel sync { p.audio().setChannel(it) } on d
         // set output, can only be done before playback
         APP.audio.audioDevice.value.ifNotNull { device ->
            val o = pf.audio().audioOutputs().find { it.name==device.name }
            val d = o?.devices?.find { it.deviceId==device.deviceId }
            if (o!=null && d!=null) p.audio().setOutput(device.name)
            if (o!=null && d!=null) p.audio().setOutputDevice(device.name, device.deviceId)
         }
         // set loop mode - an optimization, as application could do it instead
         state.loopMode sync { p.controls().repeat = it==SONG } on d

         p.media().prepare(song.uriAsVlcPath())

         val playbackListener = createPlaybackListener(state)
         p.events().addMediaPlayerEventListener(PlaybackLogger)
         p.events().addMediaPlayerEventListener(playbackListener)
         d += { p.events().removeMediaPlayerEventListener(playbackListener) }
         d += { p.events().removeMediaPlayerEventListener(PlaybackLogger) }

         if (APP.audio.startTime!=null) {
            when (state.status.value) {
               PLAYING -> play()
               else -> Unit
            }
         }

         onOK()
   }

   private object PlaybackLogger: MediaPlayerEventListener {
      override fun audioDeviceChanged(mediaPlayer: MediaPlayer?, audioDevice: String?) = logger.debug { "Playback event: audioDeviceChanged to $audioDevice" }
      override fun volumeChanged(mediaPlayer: MediaPlayer?, volume: Float) = logger.debug { "Playback event: volumeChanged to $volume" }
      override fun scrambledChanged(mediaPlayer: MediaPlayer?, newScrambled: Int) = logger.debug { "Playback event: scrambledChanged to $newScrambled" }
      override fun positionChanged(mediaPlayer: MediaPlayer?, newPosition: Float) = logger.trace { "Playback event: positionChanged to $newPosition" }
      override fun elementaryStreamSelected(mediaPlayer: MediaPlayer?, type: TrackType?, id: Int) = logger.debug { "Playback event: elementaryStreamSelected $type $id" }
      override fun seekableChanged(mediaPlayer: MediaPlayer?, newSeekable: Int) = logger.debug { "Playback event: seekableChanged to $newSeekable" }
      override fun stopped(mediaPlayer: MediaPlayer?) = logger.debug { "Playback event: stopped" }
      override fun snapshotTaken(mediaPlayer: MediaPlayer?, filename: String?) = logger.debug { "Playback event: snapshotTaken of $filename" }
      override fun muted(mediaPlayer: MediaPlayer?, muted: Boolean) = logger.debug { "Playback event: muted $muted" }
      override fun forward(mediaPlayer: MediaPlayer?) = logger.debug { "Playback event: forward" }
      override fun pausableChanged(mediaPlayer: MediaPlayer?, newPausable: Int) = logger.debug { "Playback event: pausableChanged to $newPausable" }
      override fun playing(mediaPlayer: MediaPlayer?) = logger.debug { "Playback event: playing" }
      override fun titleChanged(mediaPlayer: MediaPlayer?, newTitle: Int) = logger.debug { "Playback event: titleChanged to $newTitle" }
      override fun corked(mediaPlayer: MediaPlayer?, corked: Boolean) = logger.debug { "Playback event: corked to $corked" }
      override fun chapterChanged(mediaPlayer: MediaPlayer?, newChapter: Int) = logger.debug { "Playback event: chapterChanged to $newChapter" }
      override fun elementaryStreamDeleted(mediaPlayer: MediaPlayer?, type: TrackType?, id: Int) = logger.debug { "Playback event: elementaryStreamDeleted $type $id" }
      override fun opening(mediaPlayer: MediaPlayer?) = logger.debug { "Playback event: opening" }
      override fun backward(mediaPlayer: MediaPlayer?) = logger.debug { "Playback event: backward" }
      override fun elementaryStreamAdded(mediaPlayer: MediaPlayer?, type: TrackType?, id: Int) = logger.debug { "Playback event: elementaryStreamAdded $type $id" }
      override fun mediaPlayerReady(mediaPlayer: MediaPlayer?) = logger.debug { "Playback event: mediaPlayerReady" }
      override fun videoOutput(mediaPlayer: MediaPlayer?, newCount: Int) = logger.debug { "Playback event: videoOutput to $newCount" }
      override fun error(mediaPlayer: MediaPlayer?) = logger.debug { "Playback event: error" }
      override fun mediaChanged(mediaPlayer: MediaPlayer?, media: MediaRef?) = logger.debug { "Playback event: mediaChanged to ${media?.mediaInstance()}" }
      override fun finished(mediaPlayer: MediaPlayer?) = logger.debug { "Playback event: finished" }
      override fun paused(mediaPlayer: MediaPlayer?) = logger.debug { "Playback event: paused" }
      override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) = logger.trace { "Playback event: timeChanged to $newTime" }
      override fun buffering(mediaPlayer: MediaPlayer?, newCache: Float) = logger.trace { "Playback event: buffering to $newCache" }
      override fun lengthChanged(mediaPlayer: MediaPlayer?, newLength: Long) = logger.debug { "Playback event: lengthChanged to $newLength" }
   }

   private fun createPlaybackListener(state: PlaybackState) = object: MediaPlayerEventAdapter() {

      override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
         runFX {
            val t = newLength.millis
            state.duration.value = t
            PlaylistManager.use { it.playing?.updateTime(t) }
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
            when (state.loopMode.value) {
               OFF -> stop()
               PLAYLIST -> PlaylistManager.playNextItem()
               SONG -> Unit // handled automatically due to ControlsApi.repeat
               else -> Unit
            }
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

   object VlcSetup {
      private var askForDownload = true
      private val vlcConfig = APP.audio::playerVlcLocations.getDelegateConfig()
      private val vlcSetupAction = APP.audio.playerVlcShowSetup

      fun askForDownload() {
         if (!askForDownload) return
         askForDownload = false
         configureSetup()
      }

      fun configureSetup() {
         showFloating("Vlc Setup") { p ->
            vBox(0.0, TOP_CENTER) {
               val dl = label("\n\n\n\n") {
                  isWrapText = true
               }
               lay += text("Vlc player needs to be set up before playback is possible")
               lay += bullet("Let application download and set up its own private Vlc instance (recommended)", dl) {
                  description = "This application will download and set up a portable version of the Vlc player in" +
                     " ${APP.location.vlc.absolutePath}. This is the recommended way, as the application does not" +
                     " depend on external program."
                  isReadOnly = !Os.WINDOWS.isCurrent
                  onClick = {
                     setup() ui { p.hide() }
                  }
               }
               lay += bullet("I have a custom Vlc available", dl) {
                  description = "Add 64-bit Vlc location in application settings in `${vlcConfig.groupUi}`." +
                     " Use this option, if you do have Vlc available, but not installed or integrated in your system." +
                     " This is not recommended, as application playback functionality would depend on or share the Vlc" +
                     " application, which could cause problems.\n" +
                     "Note that moving, updating or changing this Vlc may interfere with this application."
                  onClick = {
                     p.hide()
                     p.onHidden += {
                        vlcConfig.configure("Vlc Setup") {}
                     }
                  }
               }
               lay += bullet("I already have Vlc installed", dl) {
                  description = "This requires 64-bit `VLC` to be properly installed. The installation will be discovered automatically." +
                     " This is not recommended, as application playback functionality would depend on or share the Vlc" +
                     " application, which could cause problems.\n" +
                     "Note that moving, updating or changing this Vlc instance may interfere with this application.\n\n"
                  onClick = {
                     p.hide()
                  }
               }
               lay += bullet("Not now...", dl) {
                  description = "You can open this setup again anytime using ${ActionRegistrar[APP_SEARCH].toUi()} " +
                     "and invoking ${vlcSetupAction.toUi()}"
                  onClick = {
                     p.hide()
                  }
               }
               lay += dl
            }
         }
      }

      private val setup by lazy {
         runSuspendingFx {
            AppProgress.start("Obtaining Vlc").reportFor { task ->
               VT {
                  val os = Os.current
                  val vlcDir = APP.location/"vlc"
                  val vlcZip = vlcDir/"vlc.zip"
                  val vlcVersion = "3.0.18"
                  val vlcLink = when (os) {
                     Os.WINDOWS -> URI("http://download.videolan.org/pub/videolan/vlc/$vlcVersion/win64/vlc-$vlcVersion-win64.zip")
                     else -> fail { "Vlc auto-setup is not supported on $os" }
                  }

                  if (vlcDir.exists()) vlcDir.deleteRecursivelyOrThrow()
                  downloadFile(vlcLink, vlcZip, task)
                  vlcZip.unzip(vlcDir) { it.substringAfter("vlc-$vlcVersion/") }
                  vlcZip.del()
               }
            }
         }.onDone {
            it.toTry().ifErrorNotify {
               AppError("Failed to obtain Vlc", "Reason: ${it.stacktraceAsString}")
            }
         }
      }

      fun setup() = setup

   }

   data class AudioDevice(val name: String, val deviceId: String)

   companion object {
      private val logger = KotlinLogging.logger { }

      private fun discoverVlc(locations: List<File>) = NativeDiscovery(
         WindowsNativeDiscoveryStrategy().customize(locations),
         LinuxNativeDiscoveryStrategy().customize(locations),
         OsxNativeDiscoveryStrategy().customize(locations),
         WindowsNativeDiscoveryStrategy().customize(listOf(APP.location/"vlc")),
         LinuxNativeDiscoveryStrategy().customize(listOf(APP.location/"vlc")),
         OsxNativeDiscoveryStrategy().customize(listOf(APP.location/"vlc")),
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