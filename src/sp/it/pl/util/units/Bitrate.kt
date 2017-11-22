package sp.it.pl.util.units

import sp.it.pl.util.dev.Dependency

/** Media bit rate with kb/s unit. Supports variable and unknown value. */
data class Bitrate(/** Value in kb/s, -1 if unknown, -2 if variable. */ val value: Int): Comparable<Bitrate> {

    init {
        if (-2>value) throw IllegalArgumentException("Bitrate value=$value must be -2 or larger")
    }

    /** @return true iff the value is unknown */
    fun isUnknown() = value==-1

    /** @return true iff this represents variable bitrate */
    fun isVariable() = value==-2

    /** @return true iff this represents constant bitrate */
    fun isConstant() = !isVariable()

    override fun compareTo(other: Bitrate) = Integer.compare(value, other.value)

    /** For example: "320 kbps" or "N/A". */
    @Dependency("fromString")
    override fun toString() = when (value) {
        -1 -> VALUE_NA
        -2 -> VALUE_VARIABLE
        else -> "$value $UNIT"
    }

    companion object {
        private val UNIT = "kbps"
        private val VALUE_NA = "n/a"
        private val VALUE_VARIABLE = "~"

        @Dependency("toString")
        @Throws(IndexOutOfBoundsException::class, NumberFormatException::class, IllegalArgumentException::class)
        @JvmStatic
        fun fromString(s: String): Bitrate {
            if (VALUE_NA==s) return Bitrate(-1)
            if (VALUE_VARIABLE==s) return Bitrate(-2)
            var v = s
            if (v.endsWith(UNIT)) v = v.substring(0, v.length-UNIT.length)
            v = v.trim()
            return if (v.isEmpty()) Bitrate(-1) else Bitrate(v.toInt())
        }
    }

}