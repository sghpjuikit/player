package sp.it.pl.audio

import sp.it.pl.audio.playback.PlaybackState
import sp.it.pl.audio.playlist.Playlist
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.core.CoreSerializer
import sp.it.pl.layout.widget.WidgetSource.OPEN
import sp.it.pl.layout.widget.feature.PlaylistFeature
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.ifErrorNotify
import sp.it.util.collections.setTo
import sp.it.util.dev.Blocks
import sp.it.util.dev.stacktraceAsString
import sp.it.util.functional.getOrSupply
import java.util.ArrayList
import java.util.UUID

/** State of player. */
class PlayerState {

   @JvmField var playback: PlaybackState = PlaybackState.default()
   @JvmField val playlists: MutableList<Playlist> = ArrayList()
   @JvmField var playlistId: UUID? = null

   constructor()

   constructor(s: PlayerStateDB) {
      playlists += s.playlists.map { it.toDomain() }
      playlistId = s.playlistId?.let { UUID.fromString(it) }
      playback = s.playback.toDomain()
   }

   @Blocks
   fun serialize() {
      playlistId = PlaylistManager.active

      val activePlaylists = APP.widgetManager.widgets.findAll(OPEN)
         .mapNotNull { (it.controller as? PlaylistFeature)?.playlist?.id }
         .toSet()
      playlists setTo PlaylistManager.playlists
      playlists.removeIf { it.id !in activePlaylists }

      CoreSerializer.useAtomically {
         writeSingleStorage(PlayerStateDB(this@PlayerState))
      }
   }

   companion object {

      @Blocks
      @JvmStatic
      fun deserialize() = CoreSerializer.readSingleStorage<PlayerStateDB>()
         .ifErrorNotify {
            AppError(
               "Failed to load song player state.",
               "You may have to create a new playlist.\n\nExact problem:\n${it.stacktraceAsString}"
            )
         }
         .map { it.toDomain() }
         .getOrSupply { PlayerState() }
         .also {
            PlaylistManager.playlists += it.playlists
            PlaylistManager.active = it.playlistId
         }

   }

}