package sp.it.util.parsing

import sp.it.util.functional.Try
import sp.it.util.functional.and
import sp.it.util.functional.asIs
import sp.it.util.type.VType
import sp.it.util.type.jvmErasure
import sp.it.util.type.type
import kotlin.reflect.KClass

/** Multi-type bidirectional Object-String converter.  */
abstract class Converter {

   /** @return object converted from the text or error */
   abstract fun <T: Any> ofS(type: KClass<T>, text: String): Try<T?, String>

   /** @return object converted from the text or error */
   fun <T> ofS(type: VType<T>, text: String): Try<T, String> = ofS(type.jvmErasure, text).run {
      and {
         if (it!=null || type.isNullable) Try.ok()
         else Try.error("Null is not $type")
      }
   }.asIs()

   /** @return object converted from the text or error */
   inline fun <reified T> ofS(text: String) = ofS(type<T>(), text)

   /** @return text the object has been converted to */
   abstract fun <T> toS(o: T): String

   /** @return whether string can be parsed into the object of specified type successfully */
   fun <T: Any> isValid(type: KClass<T>, text: String): Boolean = ofS(type, text).isOk

   /** @return whether string can be parsed into the object of specified type successfully */
   fun <T> isValid(type: VType<T>, text: String): Boolean = ofS(type, text).isOk

   /** @return whether string can be parsed into the object of specified type successfully */
   inline fun <reified T> isValid(text: String): Boolean = ofS<T>(text).isOk

   /** @return converter for specified type utilizing this converter */
   fun <T: Any> toConverterOf(type: KClass<T>): ConverterString<T> {
      return object: ConverterString<T> {
         override fun toS(o: T?) = this@Converter.toS(o)
         override fun ofS(s: String) = this@Converter.ofS(type, s)
      }
   }

}