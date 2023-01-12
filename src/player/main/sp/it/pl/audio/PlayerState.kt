package sp.it.pl.audio

import java.util.UUID
import sp.it.pl.audio.playback.PlaybackState
import sp.it.pl.audio.playlist.Playlist
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.core.CoreSerializer
import sp.it.pl.layout.WidgetSource.OPEN
import sp.it.pl.layout.feature.PlaylistFeature
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.ifErrorNotify
import sp.it.util.collections.setTo
import org.jetbrains.annotations.Blocking
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

   @Blocking
   fun serialize() {
      playlistId = PlaylistManager.active

      val activePlaylists = APP.widgetManager.widgets.findAll(OPEN)
         .mapNotNull { it.controller.asIf<PlaylistFeature>()?.playlist?.id }
         .toSet()
      playlists setTo PlaylistManager.playlists
      playlists.removeIf { it.id !in activePlaylists }

      CoreSerializer.useAtomically {
         writeSingleStorage(PlayerStateDB(this@PlayerState))
      }
   }

   companion object {

      @Blocking
      fun deserialize() = run {
         CoreSerializer.readSingleStorage<PlayerStateDB>()
            .ifErrorNotify {
               AppError(
                  "Failed to load song player state.",
                  "You may have to create a new playlist.\n\nExact problem:\n${it.stacktraceAsString}"
               )
            }
            .orNull()?.toDomain()
            ?: PlayerState()
      }.also {
         PlaylistManager.playlists += it.playlists // TODO: not thread safe
         PlaylistManager.active = it.playlistId
      }

   }

}