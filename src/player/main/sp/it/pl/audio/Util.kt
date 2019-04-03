package sp.it.pl.audio

import javafx.scene.media.MediaPlayer.Status
import sp.it.pl.audio.playback.PlaybackState
import sp.it.pl.audio.playlist.Playlist
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.util.units.millis
import java.io.Serializable
import java.net.URI
import java.util.HashMap
import java.util.UUID

class MetadatasDB: HashMap<String, Metadata>, Serializable {
    constructor(): super()
    constructor(songs: Map<String, Metadata>): super(songs)
}

class PlayerStateDB: Serializable {
    var playback: PlaybackStateDB
    var playlists: List<PlaylistDB>
    var playlistId: String? = null

    constructor(s: PlayerState) {
        this.playback = PlaybackStateDB(s.playback)
        this.playlists = s.playlists.map { PlaylistDB(it) }
        this.playlistId = PlaylistManager.active?.toString()
    }

    fun toDomain() = PlayerState(this)
}

class PlaybackStateDB(s: PlaybackState): Serializable {
    var volume: Double = s.volume.value
    var loopMode: String = s.loopMode.value.toString()
    var status: String = s.status.value.toString()
    var duration: Double = s.duration.value.toMillis()
    var currentTime: Double = s.currentTime.value.toMillis()
    var realTime: Double = s.realTime.value.toMillis()
    var mute: Boolean = s.mute.value
    var rate: Double = s.rate.value

    fun toDomain() = PlaybackState().also {
        it.volume.value = volume
        it.loopMode.value = LoopMode.valueOf(loopMode)
        it.status.value = Status.valueOf(status)
        it.duration.value = duration.millis
        it.currentTime.value = currentTime.millis
        it.realTime.value = realTime.millis
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

    fun toDomain() = Playlist(UUID.fromString(id)).also {
        it += items.map { it.toDomain() }
        it.updatePlayingItem(playing)
    }
}

class PlaylistItemDB: Serializable {
    var artist: String
    var title: String
    var length: Double
    var uri: String

    constructor(i: PlaylistSong) {
        this.artist = i.getArtist()
        this.title = i.getTitle()
        this.length = i.timeMs
        this.uri = i.uri.toString()
    }

    fun toDomain() = PlaylistSong(URI.create(uri), artist, title, length)
}