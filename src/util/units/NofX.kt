package util.units

import util.dev.Dependency
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
        @Throws(PatternSyntaxException::class, NumberFormatException::class, IndexOutOfBoundsException::class)
        @JvmStatic
        fun fromString(s: String): NofX {
            val a = s.split("/")
            if (a.size!=2) throw IndexOutOfBoundsException("'Text=$s' is not in an 'x/y' format")
            return NofX(a[0].toInt(), a[1].toInt())
        }
    }

}