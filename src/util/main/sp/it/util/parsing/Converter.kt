package sp.it.util.parsing

import sp.it.util.functional.Try

/** Multi-type bidirectional Object-String converter.  */
abstract class Converter {

   /** @return object converted from the text or error */
   abstract fun <T> ofS(type: Class<T>, text: String): Try<T, String>

   /** @return object converted from the text or error */
   inline fun <reified T> ofS(text: String) = ofS(T::class.java, text)

   /** @return text the object has been converted to */
   abstract fun <T> toS(o: T): String

   /** @return whether string can be parsed into the object of specified type successfully */
   fun <T> isValid(type: Class<T>, text: String): Boolean = ofS(type, text).isOk

   /** @return whether string can be parsed into the object of specified type successfully */
   inline fun <reified T> isValid(text: String): Boolean = ofS(T::class.java, text).isOk

   /** @return converter for specified type utilizing this converter */
   fun <T> toConverterOf(type: Class<T>): ConverterString<T> {
      return object: ConverterString<T> {
         override fun toS(o: T?) = this@Converter.toS(o)
         override fun ofS(s: String) = this@Converter.ofS(type, s)
      }
   }

}