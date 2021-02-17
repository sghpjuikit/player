package sp.it.util.parsing

import sp.it.util.functional.Try

/** String-Object converter. */
interface ConverterString<T>: ConverterToString<T>, ConverterFromString<T>

/** Object to String converter. */
interface ConverterToString<in T> {

   /** @return text the object has been converted to */
   fun toS(o: T): String

}

/** String to Object converter. */
interface ConverterFromString<out T> {

   /** @return object converted from the text or error */
   fun ofS(s: String): Try<T, String>

   /** @return whether string can be parsed into the object of specified type successfully */
   fun isValid(s: String): Boolean = ofS(s).isOk

}