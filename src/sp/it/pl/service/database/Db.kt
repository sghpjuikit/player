package sp.it.pl.service.database

import sp.it.pl.audio.Item
import sp.it.pl.audio.MetadatasDB
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.core.CoreSerializer
import sp.it.pl.layout.widget.controller.io.InOutput
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.access.v
import sp.it.pl.util.async.future.Fut
import sp.it.pl.util.async.runFX
import sp.it.pl.util.collections.mapset.MapSet
import sp.it.pl.util.dev.ThreadSafe
import java.io.File
import java.net.URI
import java.util.*
import java.util.UUID.fromString
import java.util.concurrent.ConcurrentHashMap

// TODO: implement proper API & subclass Service
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
        moods = File(APP.DIR_RESOURCES, "moods.cfg").useLines { it.toSet() }

        Fut<Any>()
                .then { updateInMemoryDbFromPersisted() }
                .showProgressOnActiveWindow()
    }

    fun stop() {
        running = true
    }

    fun exists(item: Item) = exists(item.uri)

    fun exists(uri: URI) = itemsById.containsKey(uri.toString())

    /** @return item from library with the URI of the specified item or null if not found */
    fun getItem(item: Item) = getItem(item.uri)

    /** @return item from library with the specified URI or null if not found */
    fun getItem(uri: URI): Metadata? = itemsById.get(uri.toString())

    fun getAllItems(): MetadatasDB = CoreSerializer.readSingleStorage() ?: MetadatasDB()

    @Suppress("DEPRECATION")
    fun addItems(items: Collection<Metadata>) {
        if (items.isEmpty()) return

        CoreSerializer.useAtomically {
            val ms = MetadatasDB(itemsById.backingMap())
            items.forEach { ms.put(it.id, it) }
            writeSingleStorage(ms)

            updateInMemoryDbFromPersisted()
        }
    }

    @Suppress("DEPRECATION")
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
        runFX { items.i.setValue(l) }
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

}