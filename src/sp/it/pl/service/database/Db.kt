package sp.it.pl.service.database

import mu.KLogging
import sp.it.pl.audio.Item
import sp.it.pl.audio.MetadatasDB
import sp.it.pl.audio.Player
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataReader
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
import java.net.URI
import java.util.Comparator
import java.util.UUID.fromString
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
class Db {

    private var running = false
    private lateinit var moods: Set<String>

    /** In memory item database. Use for library. Items are hashed by [Item.id]. */
    @ThreadSafe val itemsById = MapSet(ConcurrentHashMap<String, Metadata>(2000, 1f, 3), { it.id })
    /** Map of unique values per field gathered from [itemsById] */
    @ThreadSafe val itemUniqueValuesByField = ConcurrentHashMap<Metadata.Field<*>, Set<String>>()

    val items = InOutput<List<Metadata>>(fromString("396d2407-7040-401e-8f85-56bc71288818"), "All library songs", List::class.java)

    /**
     * Comparator defining the sorting for items in operations that wish to
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

    fun exists(item: Item) = exists(item.uri)

    fun exists(uri: URI) = itemsById.containsKey(uri.toString())

    /** @return item from library with the URI of the specified item or null if not found */
    fun getItem(item: Item) = getItem(item.uri)

    /** @return item from library with the specified URI or null if not found */
    fun getItem(uri: URI): Metadata? = itemsById[uri.toString()]

    fun getAllItems(): MetadatasDB = CoreSerializer.readSingleStorage() ?: MetadatasDB()

    fun addItems(items: Collection<Metadata>) {
        if (items.isEmpty()) return

        CoreSerializer.useAtomically {
            val ms = MetadatasDB(itemsById.backingMap())
            items.forEach { ms[it.id] = it }
            writeSingleStorage(ms)

            updateInMemoryDbFromPersisted()
        }
    }

    fun removeItems(items: Collection<Item>) {
        if (items.isEmpty()) return

        CoreSerializer.useAtomically {
            val ms = MetadatasDB(itemsById.backingMap())
            items.forEach { ms.remove(it.id) }
            writeSingleStorage(ms)
            updateInMemoryDbFromPersisted()
        }

    }

    fun removeAllItems() {
        CoreSerializer.useAtomically {
            writeSingleStorage(MetadatasDB())
            updateInMemoryDbFromPersisted()
        }
    }

    private fun setInMemoryDB(l: List<Metadata>) {
        itemsById.clear()
        itemsById += l
        updateItemValues()
        runFX { items.i.value = l }
    }

    private fun updateItemValues() {
        itemUniqueValuesByField.clear()
        Metadata.Field.FIELDS.asSequence()
                .filter { it.isAutoCompletable() }
                .forEach { f ->
                    itemUniqueValuesByField[f] = itemsById.asSequence()
                            .map { it.getFieldS(f, "") }
                            .filter { it.isNotBlank() }
                            .toSet()
                }
        itemUniqueValuesByField[Metadata.Field.MOOD] = moods
    }

    @ThreadSafe
    fun updateInMemoryDbFromPersisted() = setInMemoryDB(getAllItems().values.toList())

    @ThreadSafe
    fun refreshItemsFromFile(items: List<Item>) {
        runNew {
            val metadatas = items.asSequence().map { MetadataReader.readMetadata(it) }.filter { !it.isEmpty() }.toList()
            Player.refreshItemsWith(metadatas)
        }.showAppProgress("Refreshing library from disk")
    }

    @ThreadSafe
    fun itemToMeta(item: Item, action: (Metadata) -> Unit) {
        if (item.same(Player.playingItem.get())) {
            action(Player.playingItem.get())
            return
        }

        APP.db.itemsById[item.id]
                .ifNotNull { action(it) }
                .ifNull {
                    runOn(Player.IO_THREAD) {
                        MetadataReader.readMetadata(item)
                    } ui {
                        action(it)
                    }
                }
    }

    @ThreadSafe
    fun removeInvalidItems(): Fut<Unit> {
        return runNew {
            MetadataReader.buildRemoveMissingFromLibTask().run()
        }
    }

    companion object: KLogging()
}