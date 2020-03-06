package sp.it.util.parsing

import mu.KLogging
import sp.it.util.conf.toConfigurableFx
import sp.it.util.file.properties.PropVal.PropValN
import sp.it.util.functional.Try
import sp.it.util.type.isSuperclassOf
import java.util.Base64
import kotlin.reflect.full.createInstance
import kotlin.text.Charsets.UTF_8

/** Converter for javaFX bean convention.  */
class ConverterFX: Converter() {

   @Suppress("UNCHECKED_CAST")
   override fun <T> ofS(type: Class<T>, text: String): Try<T?, String> {
      if (text==Parsers.DEFAULT.stringNull)
         return Try.ok(null)

      return try {
         val (valueType64, values64) = text.split2(delimiter1[0])
         val valueType = Class.forName(valueType64.fromBase64())
         when {
            type.isSuperclassOf(valueType) -> {
               val v = (valueType.kotlin.createInstance() as T)!!
               val c = v.toConfigurableFx()
               values64.split(delimiter1).forEach { value64 ->
                  if (value64.count { it==delimiter2 }==1) {
                     val (cName64, cValues64) = value64.split2(delimiter2)
                     val field = c.getConfig(cName64.fromBase64())
                     field?.valueAsProperty = PropValN(cValues64.split(delimiter3).map { it.fromBase64() })
                  } else {
                     Try.error("$delimiter2 must appear exactly once per value, but value=$value64")
                  }
               }
               Try.ok<T>(v)
            }
            else -> {
               val message = "$valueType is not $type"
               logger.warn { message }
               Try.error(message)
            }
         }
      } catch (e: Exception) {
         logger.warn(e) { "Parsing failed, class=$type text=$text" }
         Try.error(e.message ?: "Unknown error")
      }
   }

   override fun <T: Any?> toS(o: T): String = when (o) {
      null -> Parsers.DEFAULT.stringNull
      else -> {
         val v = o as Any
         val values = v.toConfigurableFx().getConfigs().joinToString(delimiter1) {
            it.name.toBase64() + delimiter2 + it.valueAsProperty.valN.joinToString(delimiter3) { it.toBase64() }
         }
         v::class.java.name.toBase64() + delimiter1 + values
      }
   }


   companion object: KLogging() {
      private const val delimiter1 = "-"
      private const val delimiter2 = ':'
      private const val delimiter3 = "|"
      private fun String.toBase64() = Base64.getEncoder().encodeToString(toByteArray(UTF_8))
      private fun String.fromBase64() = String(Base64.getDecoder().decode(this), UTF_8)
      private fun String.split2(delimiter: Char) = substringBefore(delimiter) to substringAfter(delimiter)
   }

}