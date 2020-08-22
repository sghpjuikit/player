package sp.it.util.parsing

import sp.it.util.functional.Try

/** String to Object converter. */
interface ConverterFromString<T> {

   /** @return object converted from the text or error */
   fun ofS(s: String): Try<T?, String>

   /** @return whether string can be parsed into the object of specified type successfully */
   fun isValid(s: String): Boolean = ofS(s).isOk

}