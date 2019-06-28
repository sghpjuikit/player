package sp.it.util.parsing

import mu.KLogging
import sp.it.util.conf.Configurable
import sp.it.util.functional.Try
import java.util.Base64
import kotlin.text.Charsets.UTF_8

/** Converter for javaFX bean convention.  */
class ConverterFX: Converter() {

   @Suppress("UNCHECKED_CAST")
   override fun <T> ofS(type: Class<T>, text: String): Try<T?, String> {
      if (text==Parsers.DEFAULT.stringNull)
         return Try.ok(null)

      return try {
         val values = text.split(delimiter1)
         val valueType = Class.forName(values[0].fromBase64())
         if (type.isAssignableFrom(valueType)) {
            val v = (valueType.getConstructor().newInstance() as T)!!
            val c = v.toConfigurable()
            values.asSequence().drop(1).forEach {
                  val nameValue = it.split(delimiter2)
                  if (nameValue.size==2) {
                     val name = nameValue[0].fromBase64()
                     val value = nameValue[1].fromBase64()
                     c.getField(name)?.valueS = value
                  } else {
                     // ignore instead of error
                  }
               }
            Try.ok(v)
         } else {
            val message = "$valueType is not $type"
            logger.warn { message }
            Try.error(message)
         }
      } catch (e: Exception) {
         logger.warn(e) { "Parsing failed, class=$type text=$text" }
         Try.error(e.message ?: "")
      }
   }

   override fun <T: Any?> toS(o: T): String = when(o) {
      null -> Parsers.DEFAULT.stringNull
      else -> {
         val v = o as Any
         val values = v.toConfigurable().fields.joinToString(delimiter1) { it.name.toBase64() + delimiter2 + it.valueS.toBase64() }
         v::class.java.name.toBase64() + delimiter1 + values
      }
   }


   companion object: KLogging() {
      private const val delimiter1 = "-"
      private const val delimiter2 = ":"
      private fun Any.toConfigurable() = Configurable.configsFromFxPropertiesOf(this)
      private fun String.toBase64() = Base64.getEncoder().encodeToString(toByteArray(UTF_8))
      private fun String.fromBase64() = String(Base64.getDecoder().decode(this), UTF_8)
   }

}