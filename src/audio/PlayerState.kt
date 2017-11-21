package audio

import audio.playback.PLAYBACK
import audio.playback.PlaybackState
import audio.playlist.Playlist
import audio.playlist.PlaylistManager
import main.App
import java.util.*

/** State of player. */
class PlayerState {

    @JvmField var playback: PlaybackState = PlaybackState.getDefault()
    @JvmField val playbacks: MutableList<PlaybackState> = ArrayList()
    @JvmField val playlists: MutableList<Playlist> = ArrayList()
    @JvmField var playlistId: UUID? = null
    @JvmField var playbackId: UUID? = null

    constructor() {
        playbacks += playback
    }

    private constructor(s: PlayerStateDB) {
        playbacks += s.playbacks.map { it.toPlaybackState() }
        playlists += s.playlists.map { it.toPlaylist() }
        playbackId = s.playbackId?.let { UUID.fromString(it) }
        playlistId = s.playlistId?.let { UUID.fromString(it) }
        playback = playbacks.find { it.id==playbackId } ?: PlaybackState.getDefault()
    }

    fun serialize() {
        playback.realTime.set(PLAYBACK.getRealTime())
        suspendPlayback()

        playbackId = playback.id
        playlistId = PlaylistManager.active

        playlists.clear()
        playlists += PlaylistManager.playlists

        App.APP.db.savePlayerState(this)
    }

    private fun suspendPlayback() {
        val p = playbacks.find { it.id==playbackId }
        if (p!=null)
            p.change(playback)
        else {
            val s = PlaybackState.getDefault()
            s.change(playback)
            playbacks += s
        }
    }

    override fun toString() = "PlayerState{playback=$playback, playbacks=$playbacks, playlists=$playlists, playlistId=$playlistId, playbackId=$playbackId}"

    companion object {

        @JvmStatic
        fun deserialize() = App.APP.db.loadPlayerState()
                .let { if (it==null) PlayerState() else PlayerState(it) }
                .also {
                    PlaylistManager.playlists += it.playlists
                    PlaylistManager.active = it.playlistId
                }

    }

}