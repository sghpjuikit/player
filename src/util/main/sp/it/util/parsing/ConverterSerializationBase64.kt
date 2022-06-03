package sp.it.util.parsing

import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import sp.it.util.functional.Try
import sp.it.util.functional.runTry
import sp.it.util.text.decodeBase64ToBytes
import sp.it.util.text.encodeBase64

/** Converter that converts objects to/from [String] using Java serialization mechanism and Base64 representation. */
object ConverterSerializationBase64 {

   @Suppress("UNCHECKED_CAST")
   fun <T: Serializable> ofS(text: String): Try<T, Throwable> = runTry {
      ObjectInputStream(text.decodeBase64ToBytes().orThrow.inputStream()).use { it.readObject() as T }
   }

   fun <T: Serializable> toS(o: T): Try<String, Throwable> = runTry {
      ByteArrayOutputStream().use { os ->
         ObjectOutputStream(os).use {
            it.writeObject(o)
            os.toByteArray().encodeBase64()
         }
      }
   }

}