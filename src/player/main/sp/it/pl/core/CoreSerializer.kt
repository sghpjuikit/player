package sp.it.pl.core

import mu.KotlinLogging
import sp.it.pl.main.APP
import sp.it.util.async.threadFactory
import sp.it.util.dev.Blocks
import sp.it.util.dev.ThreadSafe
import sp.it.util.file.childOf
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.reflect.jvm.jvmName

val logger = KotlinLogging.logger { }

object CoreSerializer: Core {

    private lateinit var executor: ExecutorService

    override fun init() {
        if (!::executor.isInitialized)
            executor = Executors.newSingleThreadExecutor(threadFactory("Serialization", false))!!
    }

    override fun dispose() {
        if (::executor.isInitialized)
            executor.shutdown()
    }

    @ThreadSafe
    fun useAtomically(block: CoreSerializer.() -> Unit) {
        executor.execute { this.block() }
    }

    /**
     * Deserializes single instance of this type from file.
     *
     * @return deserialized object or null if none existed or error
     */
    @Blocks
    inline fun <reified T: Serializable> readSingleStorage(): T? {
        val f = APP.DIR_LIBRARY.childOf(T::class.simpleName ?: T::class.jvmName)

        if (!f.exists()) return null

        try {
            FileInputStream(f).use {
                ObjectInputStream(it).use {
                    return it.readObject() as T
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to deserialize file=$f to type=${T::class}" }
            return null
        }
    }

    /**
     * Serializes single instance of this type from file.
     * * there can only be one object of the erased type of this type (e.g. List<A> and List<B> produce same type)
     * * any previously stored object is overwritten
     *
     */
    @Blocks
    inline fun <reified T: Serializable> writeSingleStorage(o: T) {
        val f = APP.DIR_LIBRARY.childOf(T::class.simpleName ?: T::class.jvmName)
        try {
            FileOutputStream(f).use {
                ObjectOutputStream(it).use {
                    it.writeObject(o)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to serialize object=${o::class} to file=$f" }
            throw e
        }
    }

}