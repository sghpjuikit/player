package sp.it.pl.audio.tagging

import sp.it.pl.audio.Player
import sp.it.pl.audio.PlayerConfiguration
import sp.it.pl.audio.Song
import sp.it.pl.main.APP
import sp.it.pl.service.notif.Notifier
import sp.it.util.async.runFX
import sp.it.util.dev.ThreadSafe
import sp.it.util.dev.failIfFxThread

// TODO: use Fut

@ThreadSafe
fun Song.write(setter: (MetadataWriter) -> Unit) = listOf(this).write(setter)

@ThreadSafe
@JvmOverloads
fun Collection<Song>.write(setter: (MetadataWriter) -> Unit, action: ((List<Metadata>) -> Unit)? = null) {
    if (PlayerConfiguration.readOnly) return

    Player.IO_THREAD.execute {
        writeNoRefresh(setter)
        val ms = asSequence().map { it.read() }.filter { !it.isEmpty() }.toList()
        Player.refreshSongsWith(ms)
        if (action!=null) runFX { action(ms) }
    }
}

fun Song.write(setter: (MetadataWriter) -> Unit, action: ((Boolean) -> Unit)?) {
    if (PlayerConfiguration.readOnly) return

    if (this.isFileBased()) {
        Player.IO_THREAD.execute {
            val w = MetadataWriter()
            w.reset(this)
            setter(w)
            val b = w.write()

            val m = this.read()
            if (!m.isEmpty()) Player.refreshItemWith(m)
            if (action!=null) runFX { action(b) }
        }
    }
}

fun Song.writeNoRefresh(setter: (MetadataWriter) -> Unit) = listOf(this).writeNoRefresh(setter)

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
fun Song.rate(rating: Double?) {
    if (PlayerConfiguration.readOnly) return

    write { it.setRatingPercent(rating ?: -1.0) }
    APP.services.use<Notifier> { it.showTextNotification("Song rating changed to: $rating", "Update") }
}