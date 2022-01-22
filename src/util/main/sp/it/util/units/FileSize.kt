package sp.it.util.units

import sp.it.util.functional.Try
import java.io.File
import java.util.Locale
import sp.it.util.parsing.ConverterString
import sp.it.util.parsing.ConverterToUiString

/** File size. Supports up to `2^63-1` bytes and unknown value [UNKNOWN]. */
@Suppress("unused")
data class FileSize(private val v: Long): Comparable<FileSize> {

   constructor(file: File): this(file.sizeInBytes())

   init {
      if (v<-1) throw IllegalArgumentException("File size value= $v must be -1 or larger")
   }

   /** @return value in bytes or [VALUE_NA] if unknown */
   fun inBytes(): Long = v

   /** @return value in kB or [VALUE_NA] if unknown */
   fun inkBytes(): Long = if (v==VALUE_NA) VALUE_NA else v/Ki

   /** @return value in MB or [VALUE_NA] if unknown */
   fun inMBytes(): Long = if (v==VALUE_NA) VALUE_NA else v/Mi

   /** @return value in GB or [VALUE_NA] if unknown */
   fun inGBytes(): Long = if (v==VALUE_NA) VALUE_NA else v/Gi

   /** @return value in TB or [VALUE_NA] if unknown */
   fun inTBytes(): Long = if (v==VALUE_NA) VALUE_NA else v/Ti

   /** @return value in PB or [VALUE_NA] if unknown */
   fun inPBytes(): Long = if (v==VALUE_NA) VALUE_NA else v/Pi

   /** @return value in EB or [VALUE_NA] if unknown */
   fun inEBytes(): Long = if (v==VALUE_NA) VALUE_NA else v/Ei

   /** @return true iff the value is known/specific */
   fun isKnown() = v!=VALUE_NA

   /** @return true iff the value is not known/specific */
   fun isUnknown() = v==VALUE_NA

   override fun toString() = toS(this)

   override fun compareTo(other: FileSize) = v.compareTo(other.v)

   companion object: ConverterString<FileSize>, ConverterToUiString<FileSize> {

      /** `1024^1` */
      const val Ki: Long = 1024
      /** `1024^2` */
      const val Mi = Ki*Ki
      /** `1024^3` */
      const val Gi = Mi*Ki
      /** `1024^4` */
      const val Ti = Gi*Ki
      /** `1024^5` */
      const val Pi = Ti*Ki
      /** `1024^6` */
      const val Ei = Pi*Ki
      /** `0` */
      const val VALUE_MIN: Long = 0
      /** `2^63-1` */
      const val VALUE_MAX = java.lang.Long.MAX_VALUE
      /** Not available value. */
      const val VALUE_NA: Long = -1
      /** Not available string value. */
      const val VALUE_NA_S = "Unknown"

      /** FileSize of [VALUE_NA] */
      val UNKNOWN = FileSize(VALUE_NA)

      /** @return file size of this file in bytes */
      fun File.sizeInBytes(): Long {
         val l = length()
         return if (l==0L) VALUE_NA else l
      }

      /** @return file size of this file */
      fun File.size() = FileSize(this)

      override fun toUiS(o: FileSize, locale: Locale): String {
         if (o.v==VALUE_NA) return VALUE_NA_S
         val eb = o.v/Ei.toDouble()
         if (eb>=1) return "%.2f EiB".format(locale, eb)
         val pb = o.v/Pi.toDouble()
         if (pb>=1) return "%.2f PiB".format(locale, pb)
         val tb = o.v/Ti.toDouble()
         if (tb>=1) return "%.2f TiB".format(locale, tb)
         val gb = o.v/Gi.toDouble()
         if (gb>=1) return "%.2f GiB".format(locale, gb)
         val mb = o.v/Mi.toDouble()
         if (mb>=1) return "%.2f MiB".format(locale, mb)
         val kb = o.v/Ki.toDouble()
         if (kb>1) return "%.2f kiB".format(locale, kb)
         return "%d B".format(locale, o.v)
      }

      override fun toS(o: FileSize): String = toUiS(o, Locale.ROOT)

      override fun ofS(s: String): Try<FileSize, String> {
         if (s==VALUE_NA_S) return Try.ok(UNKNOWN)

         var v = s
         var unit: Long = 1
         val b = maxOf(v.indexOf("B"), v.indexOf("b"))
         if (b>0) {
            val prefix = v.substring(b - 1, b)
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
            v = v.substring(0, b - skip).trim()
         }
         return try {
            Try.ok(FileSize((unit*v.toDouble()).toLong()))
         } catch (e: NumberFormatException) {
            Try.error(e.message ?: "Unknown error")
         }
      }

      /** @return file size (unlike constructor optimized using [UNKNOWN]]) */
      fun ofBytes(value: Long): FileSize = if (value==VALUE_NA) UNKNOWN else FileSize(value)

   }
}