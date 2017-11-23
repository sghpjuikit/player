package sp.it.pl.util.units

import sp.it.pl.util.dev.Dependency
import java.io.File

/** File size. Supports values up to `2^63-1` bytes and unknown value. */
@Suppress("unused")
data class FileSize(private val v: Long): Comparable<FileSize> {

    constructor(file: File): this(file.sizeInB())

    init {
        if (v<-1) throw IllegalArgumentException("File size value= $v must be -1 or larger")
    }

    /** @return value in bytes or -1 if unknown */
    fun inBytes(): Long = v

    /** @return value in kB or -1 if unknown */
    fun inkBytes(): Long = if (v==-1L) -1 else v/Ki

    /** @return value in MB or -1 if unknown */
    fun inMBytes(): Long = if (v==-1L) -1 else v/Mi

    /** @return value in GB or -1 if unknown */
    fun inGBytes(): Long = if (v==-1L) -1 else v/Gi

    /** @return value in TB or -1 if unknown */
    fun inTBytes(): Long = if (v==-1L) -1 else v/Ti

    /** @return value in PB or -1 if unknown */
    fun inPBytes(): Long = if (v==-1L) -1 else v/Pi

    /** @return value in EB or -1 if unknown */
    fun inEBytes(): Long = if (v==-1L) -1 else v/Ei

    /** @return true iff the value is known/specific */
    fun isKnown() = v!=-1L

    /** @return true iff the value is not known/specific */
    fun isUnknown() = v==-1L

    @Dependency("fromString")
    override fun toString(): String {
        if (v==NA) return NAString
        val eb = v/Ei.toDouble()
        if (eb>=1) return String.format("%.2f EiB", eb)
        val pb = v/Pi.toDouble()
        if (pb>=1) return String.format("%.2f PiB", pb)
        val tb = v/Ti.toDouble()
        if (tb>=1) return String.format("%.2f TiB", tb)
        val gb = v/Gi.toDouble()
        if (gb>=1) return String.format("%.2f GiB", gb)
        val mb = v/Mi.toDouble()
        if (mb>=1) return String.format("%.2f MiB", mb)
        val kb = v/Ki.toDouble()
        if (kb>1) return String.format("%.2f kiB", kb)
        return String.format("%d B", v)
    }

    override fun compareTo(other: FileSize) = v.compareTo(other.v)

    companion object {

        /** `1024^1` */
        val Ki: Long = 1024
        /** `1024^2` */
        val Mi = Ki*Ki
        /** `1024^3` */
        val Gi = Ki*Mi
        /** `1024^4` */
        val Ti = Mi*Mi
        /** `1024^5` */
        val Pi = Ti*Ki
        /** `1024^6` */
        val Ei = Ti*Mi
        /** `0` */
        val MIN: Long = 0
        /** `2^63-1` */
        val MAX = java.lang.Long.MAX_VALUE
        /** Not available value. */
        val NA: Long = -1
        /** Not available string value. */
        val NAString = "Unknown"

        /** @return file size of this file in bytes */
        fun File.sizeInB(): Long {
            val l = length()
            return if (l==0L) NA else l
        }

        /** @return file size of this file */
        fun File.size() = FileSize(this)

        @Dependency("toString")
        @Throws(NumberFormatException::class)
        @JvmStatic
        fun fromString(s: String): FileSize {
            if (s==NAString) return FileSize(NA)

            var v = s
            var unit: Long = 1
            val b = maxOf(v.indexOf("B"), v.indexOf("b"))
            if (b>0) {
                val prefix = v.substring(b-1, b)
                var skip = 0
                when {
                    "k".equals(prefix, true) -> {
                        unit = Ki
                        skip++
                    }
                    "m".equals(prefix, true) -> {
                        unit = Mi
                        skip++
                    }
                    "g".equals(prefix, true) -> {
                        unit = Gi
                        skip++
                    }
                    "t".equals(prefix, true) -> {
                        unit = Ti
                        skip++
                    }
                    "p".equals(prefix, true) -> {
                        unit = Pi
                        skip++
                    }
                    "e".equals(prefix, true) -> {
                        unit = Ei
                        skip++
                    }
                }
                v = v.substring(0, b-skip).trim()
            }

            return FileSize((unit*v.toDouble()).toLong())
        }
    }
}