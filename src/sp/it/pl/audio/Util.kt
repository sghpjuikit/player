package sp.it.pl.audio

import javafx.scene.media.MediaPlayer.Status
import sp.it.pl.audio.playback.PlaybackState
import sp.it.pl.audio.playlist.Playlist
import sp.it.pl.audio.playlist.PlaylistItem
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.util.math.millis
import java.io.Serializable
import java.net.URI
import java.util.HashMap
import java.util.UUID

class MetadatasDB: HashMap<String, Metadata>, Serializable {
    constructor(): super()
    constructor(items: Map<String, Metadata>): super(items)
}

class PlayerStateDB: Serializable {
    var playbacks: List<PlaybackStateDB>
    var playlists: List<PlaylistDB>
    var playlistId: String? = null
    var playbackId: String? = null

    constructor(s: PlayerState) {
        this.playbacks = s.playbacks.map { PlaybackStateDB(it) }
        this.playlists = s.playlists.map { PlaylistDB(it) }
        this.playbackId = s.playback.id.toString()
        this.playlistId = PlaylistManager.active?.toString()
    }

}

class PlaybackStateDB(s: PlaybackState): Serializable {
    var id: String = s.id.toString()
    var volume: Double = s.volume.value
    var balance: Double = s.balance.value
    var loopMode: String = s.loopMode.value.toString()
    var status: String = s.status.value.toString()
    var duration: Double = s.duration.value.toMillis()
    var currentTime: Double = s.currentTime.value.toMillis()
    var realTime: Double = s.realTime.value.toMillis()
    var mute: Boolean = s.mute.value
    var rate: Double = s.rate.value

    fun toPlaybackState() = PlaybackState(UUID.fromString(id)).also {
        it.volume.value = volume
        it.balance.value = balance
        it.loopMode.value = LoopMode.valueOf(loopMode)
        it.status.value = Status.valueOf(status)
        it.duration.value = millis(duration)
        it.currentTime.value = millis(currentTime)
        it.realTime.value = millis(realTime)
        it.mute.value = mute
        it.rate.value = rate
    }

}

class PlaylistDB: Serializable {
    var id: String
    var playing: Int
    var items: List<PlaylistItemDB>

    constructor(p: Playlist) {
        this.id = p.id.toString()
        this.playing = p.playingI.get()
        this.items = p.map { PlaylistItemDB(it) }
    }

    fun toPlaylist() = Playlist(UUID.fromString(id)).also {
        it += items.map { it.toPlaylistItem() }
        it.updatePlayingItem(playing)
    }

}

class PlaylistItemDB: Serializable {
    var artist: String
    var title: String
    var length: Double
    var uri: String

    constructor(i: PlaylistItem) {
        this.artist = i.getArtist()
        this.title = i.getTitle()
        this.length = i.timeMs
        this.uri = i.uri.toString()
    }

    fun toPlaylistItem() = PlaylistItem(URI.create(uri), artist, title, length)

}