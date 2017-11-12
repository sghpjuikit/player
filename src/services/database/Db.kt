package services.database

import audio.Item
import audio.tagging.Metadata
import audio.tagging.MetadataWriter
import layout.widget.controller.io.InOutput
import main.App.APP
import util.access.v
import util.async.FX
import util.async.future.Fut
import util.async.runFX
import util.collections.mapset.MapSet
import util.dev.ThreadSafe
import util.file.Util.readFileLines
import util.functional.Functors.Ƒ2
import java.io.File
import java.net.URI
import java.util.*
import java.util.UUID.fromString
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.Persistence

// TODO: implement Service
// TODO: make thread-safe
@Suppress("unused")
class Db {

    private val FILE_DB_HOME by lazy { APP.DIR_LIBRARY }
    private val FILE_DB by lazy { File(APP.DIR_LIBRARY, "library_database.odb") }
    private val FILE_MOODS by lazy { File(APP.DIR_RESOURCES, "moods.cfg") }
    lateinit var emf: EntityManagerFactory
    lateinit var em: EntityManager

    /**
     * In memory item database. Use for library. Thread-safe.
     * Items are hashed by [Item.getId].
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

    private fun configure() = System.setProperty("objectdb.home", FILE_DB_HOME.path) // new $objectdb

    fun start() {
        configure()

        emf = Persistence.createEntityManagerFactory(FILE_DB.path)
        em = emf.createEntityManager()


        Fut<Any>()
                // load database
                .supply { getAllItems() }
                .use(FX, Consumer(this::setInMemoryDB))
                // load string store
                .then({
                    try {
                        val sss = em.createQuery("SELECT p FROM StringStore p", StringStore::class.java).resultList
                        stringPool = if (sss.isEmpty()) StringStore() else sss[0]

                        // populate metadata fields strings if empty
                        if (stringPool.getStrings("album").isEmpty() && !itemsById.isEmpty()) {
                            Metadata.Field.FIELDS.asSequence()
                                    .filter { it.isAutoCompletable() }
                                    .forEach { f ->
                                        val pool = stringPool.getStrings(f.name())
                                        itemsById.asSequence()
                                                .map { it.getFieldS(f, "") }
                                                .filter { !it.isEmpty() }
                                                .distinct()
                                                .forEach { pool.add(it) }
                                    }

                            // add default moods (stored in file)
                            val pool = stringPool.getStrings(Metadata.Field.MOOD.name())
                            readFileLines(FILE_MOODS).forEach { pool.add(it) }

                            // persist
                            em.transaction.begin()
                            em.merge(stringPool)
                            em.transaction.commit()
                        }

                        // add default moods (stored in file)
                        val pool = stringPool.getStrings(Metadata.Field.MOOD.name())
                        readFileLines(FILE_MOODS).forEach { pool.add(it) }

                        // persist
                        em.transaction.begin()
                        em.merge(stringPool)
                        em.transaction.commit()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                })
                .showProgressOnActiveWindow()
    }

    fun stop() {
        if (em.isOpen) em.close()
        emf.close()
    }

    fun exists(item: Item) = exists(item.uri)

    fun exists(uri: URI) = em.find(Metadata::class.java, uri.toString()) != null

    /** @return item from library with the URI of the specified item or null if not found */
    fun getItem(item: Item) = getItem(item.uri)

    /** @return item from library with the specified URI or null if not found */
    fun getItem(uri: URI): Metadata? = em.find(Metadata::class.java, uri.toString())

    fun getAllItems(): List<Metadata> = em.createQuery("SELECT p FROM MetadataItem p", Metadata::class.java).resultList

    fun addItems(items: Collection<Metadata>) {
        if (items.isEmpty()) return

        em.transaction.begin()
        val added = ArrayList<Metadata>()
        items.forEach { m ->
            if (em.find(Metadata::class.java, m.id) == null) {
                em.persist(m)
                added.add(m)
            }
        }
        em.transaction.commit()

        MetadataWriter.use(added, { it.setLibraryAddedNowIfEmpty() })

        updateInMemoryDbFromPersisted()
    }

    fun removeItems(items: Collection<Item>) {
        em.transaction.begin()
        items.forEach { item ->
            val inDb = em.find(Metadata::class.java, item.id)
            if (inDb != null) em.remove(inDb)
        }
        em.transaction.commit()

        updateInMemoryDbFromPersisted()
    }

    fun removeAllItems() = removeItems(items.i.value)

    fun updatePer(items: Collection<Metadata>) {
        em.transaction.begin()
        items.forEach { em.merge(it) }
        em.transaction.commit()
    }

    @ThreadSafe
    private fun setInMemoryDB(l: List<Metadata>) {
        itemsById.clear()
        itemsById.addAll(l)
        runFX { items.i.setValue(l) }
    }

    @ThreadSafe
    fun updateInMemoryDbFromPersisted() = setInMemoryDB(getAllItems())

}