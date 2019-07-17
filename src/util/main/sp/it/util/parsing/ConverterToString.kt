package sp.it.util.parsing

/** Object to String converter. */
interface ConverterToString<in T> {

   /** @return text the object has been converted to */
   fun toS(o: T?): String

}