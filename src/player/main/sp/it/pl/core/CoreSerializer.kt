package sp.it.pl.core

import io.fury.Fury
import io.fury.config.CompatibleMode.SCHEMA_CONSISTENT
import io.fury.config.Language.JAVA
import java.io.File
import java.io.FileNotFoundException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.jvm.jvmName
import mu.KotlinLogging
import org.jetbrains.annotations.Blocking
import sp.it.pl.audio.MetadatasDB
import sp.it.pl.audio.PlaybackStateDB
import sp.it.pl.audio.PlayerStateDB
import sp.it.pl.audio.PlaylistDB
import sp.it.pl.audio.PlaylistItemDB
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.main.APP
import sp.it.pl.main.App.Rank.SLAVE
import sp.it.util.async.actor.ActorSe
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
   private val serializerActor = ActorSe<CoreSerializer.() -> Unit>("Serializator") { this.it() }

   private val fury by lazy {
      Fury.builder()
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
         .apply {
            register(PlayerStateDB::class.java)
            register(PlaybackStateDB::class.java)
            register(PlaylistDB::class.java)
            register(PlaylistItemDB::class.java)
            register(MetadatasDB::class.java)
            register(Metadata::class.java)
         }
   }

   class SerializerFury {
      fun read(f: File): Any? = f.inputStream().use { fury.deserialize(it) }
      fun write(f: File, o: Any?): Unit = f.outputStream().use { fury.serialize(it, o) }
   }

   class SerializerJava {
      fun read(f: File): Any? = ObjectInputStream(f.inputStream().buffered()).use { it.readObject() }
      fun write(f: File, o: Any?) = ObjectOutputStream(f.outputStream().buffered()).use { it.writeObject(o) }
   }

   override fun init() = Unit

   override fun dispose() =
      serializerActor.close()

   @ThreadSafe
   fun useAtomically(block: CoreSerializer.() -> Unit): Unit =
      serializerActor(block)

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