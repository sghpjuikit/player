package sp.it.pl.core

import io.fury.Fury
import io.fury.config.CompatibleMode.SCHEMA_CONSISTENT
import io.fury.config.Language.JAVA
import java.io.File
import java.io.FileNotFoundException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.jvm.jvmName
import mu.KotlinLogging
import org.jetbrains.annotations.Blocking
import sp.it.pl.main.APP
import sp.it.pl.main.App.Rank.SLAVE
import sp.it.util.async.threadFactory
import sp.it.util.dev.ThreadSafe
import sp.it.util.file.div
import sp.it.util.file.writeSafely
import sp.it.util.functional.Try
import sp.it.util.functional.net
import sp.it.util.functional.orAlso
import sp.it.util.functional.runTry

val logger = KotlinLogging.logger { }

object CoreSerializer: Core {

   private val serializer = SerializerFury()
   private lateinit var executor: ExecutorService

   class SerializerJava {
      fun read(f: File): Any? =
         ObjectInputStream(f.inputStream().buffered()).use {
            it.readObject()
         }

      fun write(f: File, o: Any?) =
         ObjectOutputStream(f.outputStream().buffered()).use {
            it.writeObject(o)
         }
   }

   private val fury = Fury.builder()
      // set java only
      // gives performance for no loss
      .withLanguage(JAVA)
      // disable codegen
      // codegen causes large (6ms -> 200ms) startup cost for little effect
      .withCodegen(false)
      // allow unknown types
      // but may be insecure if the classes contains malicious code
      .requireClassRegistration(false)
      // disable reference tracking for shared/circular reference
      // gives performance for no loss
      .withRefTracking(false)
      // enable strict schema validation
      // gives better reasoning about state
      .withCompatibleMode(SCHEMA_CONSISTENT)
      .build()!!

   class SerializerFury {

      fun read(f: File): Any? {
       return  f.inputStream().use {
            fury.deserialize(it)
         }
      }

      fun write(f: File, o: Any?) {
         f.outputStream().use {
            fury.serialize(it, o)
         }
      }
   }

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
   @Blocking
   inline fun <reified T: Serializable> readSingleStorage(): Try<T?, Throwable> = readSingleStorage(T::class)

   /** [readSingleStorage] */
   @Blocking
   fun <T: Serializable> readSingleStorage(c: KClass<T>): Try<T?, Throwable> {
      val f = APP.location.user.library/(c.simpleName ?: c.jvmName)
      return runTry {
         serializer.read(f)?.net(c::cast)
      }.orAlso {
         if (it is FileNotFoundException) Try.ok(null)
         else Try.error(it)
      }
   }

   /**
    * Serializes single instance of this type from file.
    * * there can only be one object of the erased type of this type (e.g. List<A> and List<B> produce same type)
    * * any previously stored object is overwritten
    */
   @Blocking
   inline fun <reified T: Serializable> writeSingleStorage(o: T): Try<Nothing?, Throwable> = writeSingleStorage(o, T::class)

   /** [writeSingleStorage] */
   @Blocking
   fun <T: Serializable> writeSingleStorage(o: T, c: KClass<T>): Try<Nothing?, Throwable> {
      if (APP.rank==SLAVE) return Try.ok()

      val f = APP.location.user.library/(c.simpleName ?: c.jvmName)

      return f.writeSafely {
         runTry {
            serializer.write(it, o)
         }
      }.ifError {
         logger.error(it) { "Failed to serialize object=${o::class} to file=$f" }
      }
   }

}