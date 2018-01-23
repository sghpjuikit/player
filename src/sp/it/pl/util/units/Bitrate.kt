package sp.it.pl.util.units

import sp.it.pl.util.dev.Dependency
import sp.it.pl.util.functional.Try

/** Media bit rate with kb/s unit. Supports variable and unknown value. */
data class Bitrate(/** Value in kb/s, 0 if unknown, and negative if variable. */ val value: Int): Comparable<Bitrate> {

    /** @return true iff the value is unknown */
    fun isUnknown() = value==VALUE_NA

    /** @return true iff this represents variable bitrate */
    fun isVariable() = value<0

    /** @return true iff this represents constant bitrate */
    fun isConstant() = !isVariable()

    override fun compareTo(other: Bitrate) = Integer.compare(value, other.value)

    /** For example: "~320 kbps" or "N/A". */
    @Dependency("fromString")
    override fun toString() = when {
        value==VALUE_NA -> VALUE_S_NA
        value<0 -> "$VALUE_S_VARIABLE${-value} $UNIT"
        else -> "$value $UNIT"
    }

    companion object {
        private val VALUE_NA = 0
        private val VALUE_S_NA = "n/a"
        private val VALUE_S_VARIABLE = "~"
        private val UNIT = "kbps"

        @Dependency("toString")
        @JvmStatic
        fun fromString(s: String): Try<Bitrate> {
            if (VALUE_S_NA==s) return Try.ok(Bitrate(VALUE_NA))

            return try {
                var v = s
                v = v.trim()
                if (v.startsWith(VALUE_S_VARIABLE)) v = v.substringAfter(VALUE_S_VARIABLE)
                if (v.endsWith(UNIT)) v = v.substring(0, v.length-UNIT.length)
                v = v.trim()

                Try.ok(if (v.isEmpty()) Bitrate(VALUE_NA) else Bitrate(v.toInt()))
            } catch (e: IndexOutOfBoundsException) {
                Try.error(e)
            } catch (e: NumberFormatException) {
                Try.error(e)
            } catch (e: IllegalArgumentException) {
                Try.error(e)
            }
        }
    }

}