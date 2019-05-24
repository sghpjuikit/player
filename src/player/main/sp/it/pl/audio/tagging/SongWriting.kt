package sp.it.pl.audio.tagging

import sp.it.pl.audio.Player
import sp.it.pl.audio.PlayerConfiguration
import sp.it.pl.audio.Song
import sp.it.pl.gui.objects.rating.Rating
import sp.it.pl.main.APP
import sp.it.pl.service.notif.Notifier
import sp.it.util.async.runFX
import sp.it.util.dev.Blocks
import sp.it.util.dev.ThreadSafe
import sp.it.util.dev.failIfFxThread
import sp.it.util.functional.Try

// TODO: use Fut

/**
 * @param setter song tag editor
 */
@ThreadSafe
fun Song.write(setter: (MetadataWriter) -> Unit) = listOf(this).write(setter, {})

/**
 * @param setter song tag editor
 * @param action executes after writing with the result on fx thread
 */
@ThreadSafe
fun Collection<Song>.write(setter: (MetadataWriter) -> Unit, action: (List<Metadata>) -> Unit) {
    if (PlayerConfiguration.readOnly) return

    Player.IO_THREAD.execute {
        writeNoRefresh(setter)
        val ms = asSequence().map { it.read() }.filter { !it.isEmpty() }.toList()
        Player.refreshSongsWith(ms)
        runFX { action(ms) }
    }
}

/**
 * @param setter song tag editor
 * @param action executes after writing with the result on fx thread
 */
@ThreadSafe
fun Song.write(setter: (MetadataWriter) -> Unit, action: (Try<Boolean, Exception>) -> Unit) {
    if (PlayerConfiguration.readOnly) return

    if (isFileBased()) {
        Player.IO_THREAD.execute {
            val w = MetadataWriter()
            w.reset(this)
            setter(w)
            val b = w.write()

            val m = read()
            if (!m.isEmpty()) Player.refreshItemWith(m)
            runFX { action(b) }
        }
    } else {
        runFX { action(Try.error(Exception("Song is not a file: $uri"))) }
    }
}

/**
 * @param setter song tag editor
 */
@Blocks
fun Song.writeNoRefresh(setter: (MetadataWriter) -> Unit) = listOf(this).writeNoRefresh(setter)

/**
 * @param setter song tag editor
 */
@Blocks
fun Collection<Song>.writeNoRefresh(setter: (MetadataWriter) -> Unit) {
    failIfFxThread()
    if (PlayerConfiguration.readOnly) return

    val w = MetadataWriter()
    forEach {
        if (it.isFileBased()) {
            w.reset(it)
            setter(w)
            w.write()
        }
    }
}

/** Rates the song with <0-1> value representing percentage of the rating or null to remove the value altogether */
@ThreadSafe
fun Song.writeRating(rating: Double?) {
    if (PlayerConfiguration.readOnly) return

    write({ it.setRatingPercent(rating ?: -1.0) }) {
        if (it.isOk)
            APP.services.use<Notifier> { it.showNotification(Rating(initialRating = rating), "Song rating changed ") }
    }
}