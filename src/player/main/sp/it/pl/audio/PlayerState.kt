package sp.it.pl.audio

import java.util.UUID
import org.jetbrains.annotations.Blocking
import sp.it.pl.audio.playback.PlaybackState
import sp.it.pl.audio.playlist.Playlist
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.core.CoreSerializer
import sp.it.pl.layout.WidgetSource.OPEN
import sp.it.pl.layout.feature.PlaylistFeature
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.ifErrorNotify
import sp.it.util.async.future.Fut
import sp.it.util.collections.setTo
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.dev.stacktraceAsString
import sp.it.util.functional.asIf
import sp.it.util.functional.orNull

/** State of player. */
class PlayerState {

   @JvmField val playback: PlaybackState
   @JvmField val playlists: MutableList<Playlist> = ArrayList()
   @JvmField var playlistId: UUID? = null

   constructor() {
      playback = PlaybackState.default()
   }

   constructor(s: PlayerStateDB) {
      playlists += s.playlists.map { it.toDomain() }
      playlistId = s.playlistId?.let { UUID.fromString(it) }
      playback = s.playback.toDomain()
   }

   fun serialize(): Fut<Unit> {
      failIfNotFxThread()

      val playlistsActive = APP.widgetManager.widgets.findAll(OPEN)
         .mapNotNull { it.controller.asIf<PlaylistFeature>()?.playlist?.id }
         .toSet()

      playlistId = PlaylistManager.active
      playlists setTo PlaylistManager.playlists.filter { it.id in playlistsActive }

      val db = PlayerStateDB(this@PlayerState)
      return CoreSerializer.useAtomically {
         writeSingleStorage(db)
      }
   }

   companion object {

      @Blocking
      fun deserialize(): PlayerState {
         failIfNotFxThread()

         val p = CoreSerializer.readSingleStorage<PlayerStateDB>()
            .ifErrorNotify {
               AppError(
                  "Failed to load song player state.",
                  "You may have to create a new playlist.\n\nExact problem:\n${it.stacktraceAsString}"
               )
            }
            .orNull()?.toDomain()
            ?: PlayerState()

         PlaylistManager.playlists += p.playlists
         PlaylistManager.active = p.playlistId

         return p
      }

   }

}