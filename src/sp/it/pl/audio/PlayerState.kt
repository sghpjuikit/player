package sp.it.pl.audio

import sp.it.pl.audio.playback.PlaybackState
import sp.it.pl.audio.playlist.Playlist
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.core.CoreSerializer
import sp.it.pl.layout.widget.WidgetSource
import sp.it.pl.layout.widget.feature.PlaylistFeature
import sp.it.pl.main.APP
import java.util.ArrayList
import java.util.UUID
import kotlin.streams.asSequence

/** State of player. */
class PlayerState {

    @JvmField var playback: PlaybackState = PlaybackState.default()
    @JvmField val playbacks: MutableList<PlaybackState> = ArrayList()
    @JvmField val playlists: MutableList<Playlist> = ArrayList()
    @JvmField var playlistId: UUID? = null
    @JvmField var playbackId: UUID? = null

    constructor() {
        playbacks += playback
    }

    private constructor(s: PlayerStateDB) {
        playbacks += s.playbacks.map { it.toDomain() }
        playlists += s.playlists.map { it.toDomain() }
        playbackId = s.playbackId?.let { UUID.fromString(it) }
        playlistId = s.playlistId?.let { UUID.fromString(it) }
        playback = playbacks.find { it.id==playbackId } ?: PlaybackState.default()
    }

    fun serialize() {
        playback.realTime.set(Player.player.realTime.get()) // TODO: remove
        suspendPlayback()

        playbackId = playback.id
        playlistId = PlaylistManager.active

        val activePlaylists = APP.widgetManager.widgets.findAll(WidgetSource.OPEN).asSequence()
                .filter { it.info.hasFeature(PlaylistFeature::class.java) }
                .mapNotNull { (it.controller as PlaylistFeature?)?.playlist?.id }
                .toSet()
        playlists.clear()
        playlists += PlaylistManager.playlists
        playlists.removeIf { it.id !in activePlaylists }

        CoreSerializer.writeSingleStorage(PlayerStateDB(this))
    }

    private fun suspendPlayback() {
        val p = playbacks.find { it.id==playbackId }
        if (p!=null)
            p.change(playback)
        else {
            val s = PlaybackState.default()
            s.change(playback)
            playbacks += s
        }
    }

    companion object {

        @JvmStatic
        fun deserialize() = CoreSerializer.readSingleStorage<PlayerStateDB>()
                .let { if (it==null) PlayerState() else PlayerState(it) }
                .also {
                    PlaylistManager.playlists += it.playlists
                    PlaylistManager.active = it.playlistId
                }

    }

}