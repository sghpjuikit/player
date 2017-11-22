package sp.it.pl.audio

import javafx.scene.media.MediaPlayer.Status
import sp.it.pl.audio.playback.PlaybackState
import sp.it.pl.audio.playlist.Playlist
import sp.it.pl.audio.playlist.PlaylistItem
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode
import sp.it.pl.util.math.millis
import java.net.URI
import java.util.*
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Id

@Entity(name = "PlayerStateDB")
class PlayerStateDB {
    @Id
    @JvmField var id = 0
    @ElementCollection @JvmField var playbacks: List<PlaybackStateDB>
    @ElementCollection @JvmField var playlists: List<PlaylistDB>
    @JvmField var playlistId: String? = null
    @JvmField var playbackId: String? = null

    constructor(s: PlayerState) {
        this.id = 0
        this.playbacks = s.playbacks.map { PlaybackStateDB(it) }
        this.playlists = s.playlists.map { PlaylistDB(it) }
        this.playbackId = s.playback.id?.toString()
        this.playlistId = PlaylistManager.active?.toString()
    }

}

@Entity(name = "PlaybackStateDB")
class PlaybackStateDB(s: PlaybackState) {
    @JvmField var id: String = s.id.toString()
    @JvmField var volume: Double = s.volume.value
    @JvmField var balance: Double = s.balance.value
    @JvmField var loopMode: String = s.loopMode.value.toString()
    @JvmField var status: String = s.status.value.toString()
    @JvmField var duration: Double = s.duration.value.toMillis()
    @JvmField var currentTime: Double = s.currentTime.value.toMillis()
    @JvmField var realTime: Double = s.realTime.value.toMillis()
    @JvmField var mute: Boolean = s.mute.value
    @JvmField var rate: Double = s.rate.value

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

@Entity(name = "PlaylistDB")
class PlaylistDB {
    @JvmField var id: String
    @JvmField var playing: Int
    @ElementCollection @JvmField var items: List<PlaylistItemDB>

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

@Entity(name = "PlaylistItemDB")
class PlaylistItemDB {
    @JvmField var artist: String
    @JvmField var title: String
    @JvmField var length: Double
    @JvmField var uri: String

    constructor(i: PlaylistItem) {
        this.artist = i.getArtist()
        this.title = i.getTitle()
        this.length = i.timeMs
        this.uri = i.uri.toString()
    }

    fun toPlaylistItem() = PlaylistItem(URI.create(uri), artist, title, length)

}