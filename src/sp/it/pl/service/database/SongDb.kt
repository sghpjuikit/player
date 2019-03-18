package sp.it.pl.service.database

import mu.KLogging
import sp.it.pl.audio.MetadatasDB
import sp.it.pl.audio.Player
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.readMetadata
import sp.it.pl.audio.tagging.removeMissingSongsFromLibTask
import sp.it.pl.core.CoreSerializer
import sp.it.pl.layout.widget.controller.io.InOutput
import sp.it.pl.main.APP
import sp.it.pl.main.showAppProgress
import sp.it.pl.util.access.v
import sp.it.pl.util.async.future.Fut
import sp.it.pl.util.async.runFX
import sp.it.pl.util.async.runNew
import sp.it.pl.util.async.runOn
import sp.it.pl.util.collections.mapset.MapSet
import sp.it.pl.util.dev.ThreadSafe
import sp.it.pl.util.file.div
import sp.it.pl.util.functional.ifNotNull
import sp.it.pl.util.functional.ifNull
import sp.it.pl.util.functional.orNull
import sp.it.pl.util.functional.runTry
import sp.it.pl.util.units.uuid
import java.net.URI
import java.util.Comparator
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
class SongDb {

    private var running = false
    private lateinit var moods: Set<String>

    /** All library songs. Use output for reading/observing. Using input does not change db and has little use. */
    val songs = InOutput<List<Metadata>>(uuid("396d2407-7040-401e-8f85-56bc71288818"), "All library songs")
    /** All library songs by [Song.id]. This is in memory db and should be used as read-only. */
    @ThreadSafe val songsById = MapSet(ConcurrentHashMap<String, Metadata>(2000, 1f, 3), { it.id })
    /** Map of unique values per field gathered from [songsById] */
    @ThreadSafe val itemUniqueValuesByField = ConcurrentHashMap<Metadata.Field<*>, Set<String>>()

    /**
     * Comparator defining the sorting for songs in operations that wish to
     * provide consistent sorting across the application.
     *
     * The comparator should reflect library table sort order.
     */
    var libraryComparator = v<Comparator<in Metadata>>(Comparator { a, b -> a.compareTo(b) })

    fun init() {
        if (running) return
        running = true
        moods = runTry { (APP.DIR_RESOURCES/"moods.txt").useLines { it.toSet() } }
                .ifError { logger.error(it) { "Unable to read moods from file" } }
                .orNull() ?: setOf()

        runNew { updateInMemoryDbFromPersisted() }.showAppProgress("Loading song database")
    }

    fun stop() {
        running = true
    }

    fun exists(song: Song) = exists(song.uri)

    fun exists(uri: URI) = songsById.containsKey(uri.toString())

    /** @return song from library with the URI of the specified song or null if not found */
    fun getSong(song: Song) = getSong(song.uri)

    /** @return item from library with the specified URI or null if not found */
    fun getSong(uri: URI): Metadata? = songsById[uri.toString()]

    fun getAllSongs(): MetadatasDB = CoreSerializer.readSingleStorage() ?: MetadatasDB()

    fun addSongs(items: Collection<Metadata>) {
        if (items.isEmpty()) return

        CoreSerializer.useAtomically {
            val ms = MetadatasDB(songsById.backingMap())
            items.forEach { ms[it.id] = it }
            writeSingleStorage(ms)

            updateInMemoryDbFromPersisted()
        }
    }

    fun removeSongs(songs: Collection<Song>) {
        if (songs.isEmpty()) return

        CoreSerializer.useAtomically {
            val ms = MetadatasDB(songsById.backingMap())
            songs.forEach { ms.remove(it.id) }
            writeSingleStorage(ms)
            updateInMemoryDbFromPersisted()
        }

    }

    fun removeAllSongs() {
        CoreSerializer.useAtomically {
            writeSingleStorage(MetadatasDB())
            updateInMemoryDbFromPersisted()
        }
    }

    private fun setInMemoryDB(l: List<Metadata>) {
        songsById.clear()
        songsById += l
        updateSongValues()
        runFX { songs.i.value = l }
    }

    private fun updateSongValues() {
        itemUniqueValuesByField.clear()
        Metadata.Field.FIELDS.asSequence()
                .filter { it.isAutoCompletable() }
                .forEach { f ->
                    itemUniqueValuesByField[f] = songsById.asSequence()
                            .map { it.getFieldS(f, "") }
                            .filter { it.isNotBlank() }
                            .toSet()
                }
        itemUniqueValuesByField[Metadata.Field.MOOD] = moods
    }

    @ThreadSafe
    fun updateInMemoryDbFromPersisted() = setInMemoryDB(getAllSongs().values.toList())

    @ThreadSafe
    fun refreshSongsFromFile(songs: List<Song>) {
        runNew {
            val metadatas = songs.asSequence().map { it.readMetadata() }.filter { !it.isEmpty() }.toList()
            Player.refreshSongsWith(metadatas)
        }.showAppProgress("Refreshing library from disk")
    }

    @ThreadSafe
    fun songToMeta(song: Song, action: (Metadata) -> Unit) {
        if (song.same(Player.playingSong.get())) {
            action(Player.playingSong.get())
            return
        }

        APP.db.songsById[song.id]
                .ifNotNull { action(it) }
                .ifNull {
                    runOn(Player.IO_THREAD) {
                        song.readMetadata()
                    } ui {
                        action(it)
                    }
                }
    }

    @ThreadSafe
    fun removeInvalidSongs(): Fut<Unit> {
        return runNew {
            removeMissingSongsFromLibTask().run()
        }
    }

    companion object: KLogging()
}