package sp.it.util.units

import sp.it.util.functional.Try
import sp.it.util.parsing.ConverterString

/** Media bit rate with kb/s unit. Supports variable and unknown value. */
data class Bitrate(
   /** Value in kb/s, 0 if unknown, and negative if variable. */
   val value: Int
): Comparable<Bitrate> {

   /** @return true iff the value is unknown */
   fun isUnknown() = value==VALUE_NA

   /** @return true iff this represents variable bitrate */
   fun isVariable() = value<0

   /** @return true iff this represents constant bitrate */
   fun isConstant() = !isVariable()

   override fun compareTo(other: Bitrate) = value compareTo other.value

   override fun toString() = toS(this)

   companion object: ConverterString<Bitrate> {
      private const val VALUE_NA = 0
      private const val VALUE_S_NA = "n/a"
      private const val VALUE_S_VARIABLE = "~"
      private const val UNIT = "kbps"

      /** Bitrate of [VALUE_NA] */
      val UNKNOWN = Bitrate(VALUE_NA)

      /** @return file size (unlike constructor optimized using [UNKNOWN]]) */
      fun ofInt(value: Int): Bitrate = if (value==VALUE_NA) UNKNOWN else Bitrate(value)

      /** For example: "~320 kbps" or "N/A". */
      override fun toS(o: Bitrate) = when {
         o.value==VALUE_NA -> VALUE_S_NA
         o.value<0 -> "$VALUE_S_VARIABLE${-o.value} $UNIT"
         else -> "${o.value} $UNIT"
      }

      override fun ofS(s: String): Try<Bitrate, String> {
         if (VALUE_S_NA==s) return Try.ok(UNKNOWN)

         return try {
            var v = s
            v = v.trim()
            if (v.startsWith(VALUE_S_VARIABLE)) v = v.substringAfter(VALUE_S_VARIABLE)
            if (v.endsWith(UNIT)) v = v.substring(0, v.length - UNIT.length)
            v = v.trim()

            Try.ok(if (v.isEmpty()) UNKNOWN else Bitrate(v.toInt()))
         } catch (e: IndexOutOfBoundsException) {
            Try.error(e.message ?: "Unknown error")
         } catch (e: NumberFormatException) {
            Try.error(e.message ?: "Unknown error")
         } catch (e: IllegalArgumentException) {
            Try.error(e.message ?: "Unknown error")
         }
      }
   }

}