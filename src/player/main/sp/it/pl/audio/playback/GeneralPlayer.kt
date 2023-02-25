package sp.it.pl.audio.playback

import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.scene.media.MediaPlayer.Status.STOPPED
import javafx.util.Duration
import javafx.util.Duration.ZERO
import kotlin.math.pow
import mu.KLogging
import sp.it.pl.audio.PlayerState
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.main.APP
import sp.it.pl.main.AppTexts.textNoVal
import sp.it.util.access.readOnly
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.runFX
import sp.it.util.async.runVT
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.reactive.attach1IfNonNull
import sp.it.util.units.millis

/** Audio player which abstracts away from the implementation. */
class GeneralPlayer(state: PlayerState) {

   private val state = state
   private var p: Play? = null
   private val _pInfo = ReadOnlyObjectWrapper("<none>")
   val pInfo = _pInfo.readOnlyProperty.readOnly()
   private var i: Song? = null
   private var seekVolumeAnim = anim(150.millis) { state.playback.volumeFadeMultiplier.value = it.pow(2) }

   fun play(song: PlaylistSong) {
      APP.audio.isSuspendedBecauseStartedPaused = false

      // Do not recreate player if same song plays again
      // 1) improves performance
      // 2) avoids firing some playback events
      if (p!=null && song.same(i)) {
         seek(ZERO)
         return
      }

      i = song
      p?.disposePlayback()
      val player = p ?: computePlayer().apply { p = this }
      val onUnableToPlay = { _: PlaylistSong -> runFX { PlaylistManager.use { it.playNextItem() } } }

      _pInfo.value = when (p) {
         null -> textNoVal
         is VlcPlayer -> "VlcPlayer"
         is JavaFxPlayer -> "JavaFX"
      }

      if (player==null) {
         logger.info { "Player=null can not play song=$song{}" }
         onUnableToPlay(song)
      } else {
         runVT {
            if (song.isCorrupt()) {
               onUnableToPlay(song)
            } else {
               runFX {
                  player.createPlayback(
                     song,
                     state.playback,
                     onOK = {
                        player.play()
                        state.playback.realTimeImpl.syncRealTimeOnPlay()
                        // throw song change event
                        APP.audio.playingSong.songChanged(song)
                        APP.audio.isSuspended = false
                        // fire other events (may rely on the above)
                        APP.audio.onPlaybackStart()
                        if (APP.audio.postActivating1st || !APP.audio.postActivating)
                        song.time?.takeIf { it.toMillis()>0.0 }.ifNotNull { time -> APP.audio.onPlaybackAt.forEach { it.restart(time) } } // TODO: this should work for corrupted/ not updated or 00:00 songs
                        APP.audio.postActivating = false
                        APP.audio.postActivating1st = false
                     },
                     onFail = { unableToPlayAny ->
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

   @Suppress("RedundantNullableReturnType")
   private fun computePlayer(): Play? = VlcPlayer()

   fun resume() {
      p.ifNotNull {
         it.resume()
         APP.audio.onPlaybackAt.forEach { it.unpause() }
      }.ifNull {
         if (APP.audio.isSuspendedBecauseStartedPaused)
            PlaylistManager.use {
               it.playing.ifNotNull { play(it) }
            }
      }
   }

   fun pause() {
      p.ifNotNull {
         it.pause()
         APP.audio.onPlaybackAt.forEach { it.pause() }
      }
   }

   fun pauseResume() {
      if (state.playback.status.value==PLAYING) pause()
      else resume()
   }

   fun stop() {
      p.ifNotNull {
         it.stop()
         runFX {
            APP.audio.playingSong.songChanged(Metadata.EMPTY)
            APP.audio.onPlaybackAt.forEach { it.stop() }
            PlaylistManager.playlists.forEach { it.updatePlayingItem(-1) }
            PlaylistManager.active = null
         }
      }
   }

   fun seek(to: Duration) {
      p.ifNotNull {
         if (state.playback.status.value==STOPPED) pause()

         seekVolumeAnim.stop()
         state.playback.volumeFadeMultiplier.value = 0.0
         state.playback.currentTime.value = to // allow next doSeek() target correct value even if this has not finished
         state.playback.currentTime.attach1IfNonNull { APP.audio.onSeekDone() }
         p?.seek(to)
         seekVolumeAnim.jumpTo(ZERO)
         seekVolumeAnim.playFromDir(true)
      }.ifNull {
         if (APP.audio.isSuspendedBecauseStartedPaused)
            PlaylistManager.use {
               it.playing.ifNotNull { play(it) }
            }
      }
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

   sealed interface Play {

      val canDoSongRepeat: Boolean

      fun play()

      fun pause()

      fun resume()

      fun seek(duration: Duration)

      fun stop()

      fun createPlayback(song: Song, state: PlaybackState, onOK: () -> Unit, onFail: (Boolean) -> Unit)

      /** Stops playback if any and disposes of the player resources. */
      fun disposePlayback()   // TODO: JavaFXPLayer has async implementation, but lcPlayer has blocking implementation, fix and document

      fun dispose()
   }

}