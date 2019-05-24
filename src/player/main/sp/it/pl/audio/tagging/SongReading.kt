package sp.it.pl.audio.tagging

import javafx.concurrent.Task
import javafx.scene.media.Media
import mu.KotlinLogging
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.main.APP
import sp.it.util.dev.Blocks
import sp.it.util.dev.failIfFxThread
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.toUnit
import java.util.ArrayList
import java.util.function.BiConsumer
import kotlin.streams.asSequence

private val logger = KotlinLogging.logger { }

/**
 * Reads metadata for this song by reading the underlying resource.
 *
 * @return metadata for specified song or [Metadata.EMPTY] if error occurs
 * @throws RuntimeException if called on fx thread
 */
@Blocks
fun Song.read(): Metadata {
    failIfFxThread()

    return when {
        isCorrupt() -> Metadata.EMPTY
        isFileBased() -> getFile()!!.readAudioFile().orNull()?.net { Metadata(it) } ?: Metadata.EMPTY
        else ->
            // TODO: implement properly & watch out for new Media() throwing Exception, see JavaFxPlayer.java
            try {
                val m = Media(uri.toString())
                PlaylistSong(uri, "", "", m.duration.toMillis()).toMeta()
            } catch (e: IllegalArgumentException) {
                logger.error(e) { "Error creating metadata for non file based song: $this" }
                toMeta()
            } catch (e: UnsupportedOperationException) {
                logger.error(e) { "Error creating metadata for non file based song: $this" }
                toMeta()
            }
    }
}

// TODO: handle error properly, return custom Result object
/**
 * Creates task that reads metadata for specified songs, returning all successfully read metadata.
 *
 * @param songs list of songs to read metadata for
 * @return the task reading metadata returning all successfully read metadata
 * @throws NullPointerException if any parameter null
 */
fun Song.Companion.readTask(songs: Collection<Song>) = object: Task<List<Metadata>>() {
    private val sb = StringBuilder(40)

    init {
        updateTitle("Reading metadata")
    }

    override fun call(): List<Metadata> {
        val all = songs.size
        var completed = 0
        var failed = 0
        val metadatas = ArrayList<Metadata>()

        for (song in songs) {
            if (isCancelled) return metadatas

            completed++

            val m = song.read()
            if (m.isEmpty()) failed++ // on fail
            else metadatas.add(m)    // on success

            updateMessage(all, completed, failed)
            updateProgress(completed.toLong(), all.toLong())
        }

        if (!isCancelled) {
            updateMessage(all, completed, failed)
            updateProgress(completed.toLong(), all.toLong())
        }

        return metadatas
    }

    private fun updateMessage(all: Int, done: Int, failed: Int) {
        sb.setLength(0)
        sb += "Read: "
        sb += done
        sb += "/"
        sb += all
        sb += " "
        sb += " Failed: "
        sb += failed
        updateMessage(sb.toString())
    }
}

// TODO: handle error properly, failed songs should still be added as Song.toMeta() instead of ignored
/**
 * Creates a task that:
 *  *  Reads metadata from files of the songs.
 *  *  Adds songs to library. If library already contains the song, it will not be added.
 *  *  Returns detailed information about the end result
 *
 * @return the task
 */
fun Song.Companion.addToLibTask(songs: Collection<Song>) = object: Task<AddSongsToLibResult>() {
    private val sb = StringBuilder(40)

    init {
        updateTitle("Adding songs to library")
        updateMessage("Progress: -")
        updateProgress(0, 1)
    }

    override fun call(): AddSongsToLibResult {
        val all = ArrayList(songs)
        val processed = ArrayList<Song>(all.size)
        val converted = ArrayList<Metadata>(all.size)
        val skipped = ArrayList<Song>(0)

        for (song in songs) {
            if (isCancelled) break

            var m = APP.db.getSong(song)
            if (m==null) {
                song.writeNoRefresh { it.setLibraryAddedNowIfEmpty() }
                m = song.read()

                if (m.isEmpty()) skipped += song
                else converted += m

            } else {
                skipped += song
            }

            processed += song

            updateMessage(all.size, processed.size, skipped.size)
            updateProgress(processed.size.toLong(), all.size.toLong())
        }

        if (!isCancelled) {
            APP.db.addSongs(converted)

            updateMessage(all.size, processed.size, skipped.size)
            updateProgress(processed.size.toLong(), all.size.toLong())
        }

        return AddSongsToLibResult(all, processed, converted, skipped)
    }


    private fun updateMessage(all: Int, done: Int, skipped: Int) {
        sb.setLength(0)
        sb += "Added: "
        sb += done
        sb += " / "
        sb += all
        sb += " Skipped: "
        sb += skipped
        updateMessage(sb.toString())
    }
}

class AddSongsToLibResult(
        val all: List<Song>,
        val processed: List<Song>,
        val converted: List<Metadata>,
        val skipped: List<Song>
)

// TODO: return proper Result object
/** @return a task that removes from library all songs, which refer to non-existent files */
fun Song.Companion.removeMissingFromLibTask() = object: Task<Unit>() {
    private val sb = StringBuilder(40)

    init {
        updateTitle("Removing missing songs from library")
    }

    override fun call() {
        var completed = 0
        val allItems = APP.db.songsById.streamV().asSequence().toList()
        val all = allItems.size
        val removedItems = ArrayList<Metadata>()

        for (m in allItems) {
            if (isCancelled) break

            completed++

            if (m.isFileBased() && !m.getFile()!!.exists()) {
                removedItems.add(m)
            }

            updateMessage(all, completed, 0)
            updateProgress(completed.toLong(), all.toLong())
        }

        if (!isCancelled) {
            APP.db.removeSongs(removedItems)

            updateMessage(all, completed, removedItems.size)
            updateProgress(completed.toLong(), all.toLong())
        }
    }

    private fun updateMessage(all: Int, done: Int, removed: Int) {
        sb.setLength(0)
        sb += "Checked: "
        sb += done
        sb += "/"
        sb += all
        sb += " "
        sb += " Removed: "
        sb += removed
        updateMessage(sb.toString())
    }
}

// TODO: remove
fun <T> Task<T>.setOnDone(onEnd: BiConsumer<Boolean, T>) {
    setOnSucceeded { onEnd.accept(true, value) }
    setOnFailed { onEnd.accept(false, value) }
    setOnCancelled { onEnd.accept(false, value) }
}

private operator fun StringBuilder.plusAssign(text: String) = append(text).toUnit()
private operator fun StringBuilder.plusAssign(number: Int) = append(number).toUnit()