package sp.it.pl.util.parsing

import sp.it.pl.util.functional.Try

/** Multi-type bidirectional Object-String converter.  */
abstract class Converter {

    /** @return object converted from the text or error */
    abstract fun <T> ofS(c: Class<T>, s: String): Try<T>

    /** @return object converted from the text or error */
    inline fun <reified T> ofS(s: String) = ofS(T::class.java, s)

    /** @return text the object has been converted to */
    abstract fun <T> toS(o: T?): String

    /** @return whether string can be parsed into the object of specified type successfully */
    fun <T> isValid(c: Class<T>, s: String): Boolean = ofS(c, s).isOk

    /** @return whether string can be parsed into the object of specified type successfully */
    inline fun <reified T> isValid(s: String): Boolean = ofS(T::class.java, s).isOk

    /** @return converter for specified type utilizing this converter */
    fun <T> toConverterOf(c: Class<T>): ConverterString<T> {
        return object: ConverterString<T> {
            override fun toS(o: T?) = this@Converter.toS(o)
            override fun ofS(s: String) = this@Converter.ofS(c, s)
        }
    }

}