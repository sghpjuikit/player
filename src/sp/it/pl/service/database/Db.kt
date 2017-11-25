package sp.it.pl.service.database

import sp.it.pl.audio.Item
import sp.it.pl.audio.MetadatasDB
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.layout.widget.controller.io.InOutput
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.core.CoreSerializer
import sp.it.pl.util.access.v
import sp.it.pl.util.async.FX
import sp.it.pl.util.async.future.Fut
import sp.it.pl.util.async.runFX
import sp.it.pl.util.collections.mapset.MapSet
import sp.it.pl.util.dev.ThreadSafe
import sp.it.pl.util.functional.Functors.Ƒ2
import java.io.File
import java.net.URI
import java.util.*
import java.util.UUID.fromString
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

// TODO: implement proper API & subclass Service
@Suppress("unused")
class Db {

    private val FILE_MOODS by lazy { File(APP.DIR_RESOURCES, "moods.cfg") }
    private var running = false

    /**
     * In memory item database. Use for library. Thread-safe.
     * Items are hashed by [Item.id].
     */
    @ThreadSafe
    val itemsById = MapSet(ConcurrentHashMap<String, Metadata>(2000, 1f, 3), { it.id })
    val items = InOutput<List<Metadata>>(fromString("396d2407-7040-401e-8f85-56bc71288818"), "All library songs", List::class.java)

    /**
     * Comparator defining the sorting for items in operations that wish to
     * provide consistent sorting across the application.
     *
     * The comparator should reflect library table sort order.
     */
    var libraryComparator = v<Comparator<in Metadata>>(Comparator { a, b -> a.compareTo(b) })

    /**
     * In memory storage for strings that persists in database.
     * Map that maps sets of strings to string keys. The keys are case-insensitive.
     *
     * The store is loaded when DB starts. Changes persist immediately.
     */
    lateinit var stringPool: StringStore

    var autocompletionContains = true
    val autocompletionFilter = Ƒ2<String, String, Boolean> { text, phrase ->
        if (autocompletionContains) text.contains(phrase) else text.startsWith(phrase)
    }

    fun init() {
        if (running) return
        running = true

        Fut<Any>()
                .supply { getAllItems().values.toList() }
                .use(FX, Consumer(this::setInMemoryDB))
                .then {
                    stringPool = CoreSerializer.readSingleStorage() ?: StringStore().also { pools ->
                        pools.modify {
                            Metadata.Field.FIELDS.asSequence()
                                    .filter { it.isAutoCompletable() }
                                    .forEach { f ->
                                        pools.getStrings(f.name()) += itemsById.asSequence()
                                                .map { it.getFieldS(f, "") }
                                                .filter { !it.isEmpty() }
                                                .distinct()
                                    }

                            FILE_MOODS.useLines { pools.getStrings(Metadata.Field.MOOD.name()) += it }
                        }
                    }
                }
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

    @ThreadSafe
    private fun setInMemoryDB(l: List<Metadata>) {
        itemsById.clear()
        itemsById.addAll(l)
        runFX { items.i.setValue(l) }
    }

    @ThreadSafe
    fun updateInMemoryDbFromPersisted() = setInMemoryDB(getAllItems().values.toList())

}