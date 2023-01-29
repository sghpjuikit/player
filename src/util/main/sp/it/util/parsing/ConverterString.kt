package sp.it.util.parsing

import java.util.Locale
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Error
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.type.VType

/** String-Object converter. */
interface ConverterString<T>: ConverterToString<T>, ConverterFromString<T> {

   /** @return this converter, supporting null by representing it with the specified text */
   fun nullable(nullText: String): ConverterString<T?> = object: ConverterString<T?> {
      override fun toS(o: T?) = if (o==null) nullText else this@ConverterString.toS(o)
      override fun ofS(s: String) = if (s==nullText) Ok(null) else this@ConverterString.ofS(s)
   }

   fun nonNull(): ConverterString<T & Any> = object: ConverterString<T & Any> {
      override fun toS(o: T & Any) = this@ConverterString.toS(o)
      override fun ofS(s: String): Try<T & Any, String> = this@ConverterString.ofS(s).net { if ( it is Ok && it.value==null) Error("Must not be null") else it.asIs() }
   }

}

fun <T> ConverterString<T?>.nullOf(type: VType<T>): ConverterString<T> = if (type.isNotNull) nonNull().asIs() else this.asIs()

/** Object to String converter. */
fun interface ConverterToUiString<in T> {

   /** @return human-readable text the object has been converted to */
   fun toUiS(o: T, locale: Locale): String

}

/** Object to String converter. */
fun interface ConverterToString<in T> {

   /** @return text the object has been converted to */
   fun toS(o: T): String

}

/** String to Object converter. */
fun interface ConverterFromString<out T> {

   /** @return object converted from the text or error */
   fun ofS(s: String): Try<T, String>

   /** @return whether string can be parsed into the object of specified type successfully */
   fun isValid(s: String): Boolean = ofS(s).isOk

}