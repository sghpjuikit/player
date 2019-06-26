package sp.it.pl.core

import mu.KotlinLogging
import sp.it.pl.main.APP
import sp.it.pl.main.App.Rank.SLAVE
import sp.it.util.async.threadFactory
import sp.it.util.dev.Blocks
import sp.it.util.dev.ThreadSafe
import sp.it.util.file.div
import sp.it.util.file.writeSafely
import sp.it.util.functional.Try
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.runTry
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
         executor = Executors.newSingleThreadExecutor(threadFactory("Serialization", false))
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
      val f = APP.DIR_LIBRARY/(T::class.simpleName ?: T::class.jvmName)

      if (!f.exists()) return null

      return runTry {
         ObjectInputStream(f.inputStream()).use {
            it.readObject() as T
         }
      }.getOrSupply {
         logger.error(it) { "Failed to deserialize file=$f to type=${T::class}" }
         null
      }
   }

   /**
    * Serializes single instance of this type from file.
    * * there can only be one object of the erased type of this type (e.g. List<A> and List<B> produce same type)
    * * any previously stored object is overwritten
    */
   @Blocks
   inline fun <reified T: Serializable> writeSingleStorage(o: T): Try<Nothing?, Throwable> {
      if (APP.rank==SLAVE) return Try.ok()

      val f = APP.DIR_LIBRARY/(T::class.simpleName ?: T::class.jvmName)
      return f.writeSafely {
         runTry {
            ObjectOutputStream(it.outputStream()).use {
               it.writeObject(o)
            }
         }
      }.ifError {
         logger.error(it) { "Failed to serialize object=${o::class} to file=$f" }
      }
   }

}