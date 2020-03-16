package sp.it.pl.audio.playback

import javafx.scene.media.Media
import javafx.scene.media.MediaException
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaPlayer.Status.PAUSED
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.scene.media.MediaPlayer.Status.STOPPED
import javafx.util.Duration
import javafx.util.Duration.ZERO
import mu.KLogging
import sp.it.pl.audio.Song
import sp.it.pl.audio.playback.VolumeProperty.Companion.linToLog
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode.*
import sp.it.pl.main.APP
import sp.it.util.async.runIO
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.attach1If
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncTo

class JavaFxPlayer: GeneralPlayer.Play {

   private var player: MediaPlayer? = null
   private val onDispose = Disposer()

   override val canDoSongRepeat = false

   override fun play() = player?.play() ?: Unit

   override fun pause() = player?.pause() ?: Unit

   override fun resume() = player?.play() ?: Unit

   override fun seek(duration: Duration) = player?.seek(duration) ?: Unit

   override fun stop() = player?.stop() ?: Unit

   override fun createPlayback(song: Song, state: PlaybackState, onOK: Function0<Unit>, onFail: Function1<Boolean, Unit>) {
      runIO {
         try {
            // TODO: Media creation throws MediaException (FileNotFoundException) for files containing some special chars (unicode?)
            //       I think it is the same problem as the ne with vlcPlayer, needs file:///
            //       https://stackoverflow.com/questions/10062270/how-to-target-a-file-a-path-to-it-in-java-javafx
            Media(song.uri.toString()) // can block for a long time
         } catch (e: MediaException) {
            logger.error { "Media creation error for ${song.uri}" }
            null
         }
      } ui { media ->
         if (media==null) {
            onFail(false)
         } else {
            val p = MediaPlayer(media)
            player = p

            p.startTime = ZERO

            state.volume sync { v -> p.volume = linToLog(v.toDouble()) } on onDispose
            state.mute syncTo p.muteProperty() on onDispose
            state.rate syncTo p.rateProperty() on onDispose
            p.onEndOfMedia = Runnable {
               APP.audio.onPlaybackEnd()
               if (state.loopMode.value==SONG) seek(ZERO)
               when (state.loopMode.value) {
                  OFF -> stop()
                  PLAYLIST -> PlaylistManager.playNextItem()
                  SONG -> seek(ZERO)
                  else -> Unit
               }
            }

            p.statusProperty().attach1If({ it==PLAYING || it==PAUSED || it==STOPPED }) {
               p.currentTimeProperty() syncTo state.currentTime on onDispose
               p.statusProperty().sync { if (!APP.audio.isSuspended) state.status.value = it } on onDispose
            }
            p.statusProperty() attach {
               if (it==PLAYING || it==PAUSED) {
                  if (APP.audio.startTime!=null) {
                     APP.audio.startTime?.let { seek(it) }
                     APP.audio.startTime = null
                  }
               }
            }

            val s = state.status.value
            if (APP.audio.startTime!=null) {
               if (s==PLAYING) play()
               else if (s==PAUSED) pause()
            }

            onOK()
         }
      }
   }

   override fun disposePlayback() {
      if (player==null) return

      onDispose()
      player?.audioSpectrumListener = null
      player?.onEndOfMedia = null
      player?.dispose()   //  calls stops() and frees resources
      player = null
   }

   override fun dispose() = disposePlayback()

   companion object: KLogging()
}