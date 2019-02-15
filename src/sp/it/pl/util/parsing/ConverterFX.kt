package sp.it.pl.util.parsing

import mu.KLogging
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.functional.Try
import sp.it.pl.util.functional.Try.error
import sp.it.pl.util.functional.Try.ok
import sp.it.pl.util.functional.net

/** Converter for javaFX bean convention.  */
class ConverterFX: Converter() {

    private val delimiterVal = "-"
    private val delimiterName = ":"

    @Suppress("UNCHECKED_CAST")
    override fun <T> ofS(type: Class<T>, text: String): Try<T, String> {
        if (text==Parsers.DEFAULT.stringNull)
            return Try.ok(null)

        try {
            val values = text.split(delimiterVal).toTypedArray()
            val valueType = Class.forName(values[0])
            if (type.isAssignableFrom(valueType)) {
                val v = (valueType.getConstructor().newInstance() as T)!!
                val c = v.toConfigurable()
                values.asSequence()
                        .drop(1)
                        .forEach {
                            val nameValue = it.split(delimiterName).toTypedArray()
                            if (nameValue.size==2) {
                                val name = nameValue[0]
                                val value = nameValue[1]
                                c.setField(name, value)
                            } else {
                                // ignore instead of error
                            }
                        }
                return ok(v)
            } else {
                val message = "$valueType is not $type"
                logger.warn { message }
                return error(message)
            }

        } catch (e: Exception) {
            logger.warn(e) { "Parsing failed, class=$type text=$text" }
            return error(e.message)
        }
    }

    override fun <T: Any?> toS(o: T): String = o
            ?.net {
                val v = it as Any
                val values = v.toConfigurable().fields.joinToString(delimiterVal) { it.name+delimiterName+it.valueS }
                v::class.java.name+delimiterVal+values
            }
            ?: Parsers.DEFAULT.stringNull

    private fun Any.toConfigurable() = Configurable.configsFromFxPropertiesOf(this)

    companion object: KLogging()

}