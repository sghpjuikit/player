package sp.it.util.units

import sp.it.util.dev.Dependency
import sp.it.util.functional.Try
import java.util.regex.PatternSyntaxException

/** Amount within an amount. For example 15/20.  */
data class NofX(val n: Int, val of: Int): Comparable<NofX> {

    override fun compareTo(other: NofX): Int {
        val i = of.compareTo(other.of)
        return if (i!=0) i else n.compareTo(other.n)
    }

    @Dependency("fromString")
    override fun toString() = toString("/")

    fun toString(separator: String) = (if (n==-1) "?" else "$n")+separator+(if (of==-1) "?" else "$of")

    companion object {
        @Dependency("toString")
        @JvmStatic
        fun fromString(s: String): Try<NofX, Throwable> {
            val a = s.split("/")
            return when {
                a.size!=2 -> Try.error(IndexOutOfBoundsException("'Text=$s' is not in an 'x/y' format"))
                else -> try {
                    Try.ok(NofX(a[0].toInt(), a[1].toInt()))
                } catch (e: PatternSyntaxException) {
                    Try.error(e)
                } catch (e: NumberFormatException) {
                    Try.error(e)
                }
            }
        }
    }

}