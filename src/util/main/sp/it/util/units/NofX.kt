package sp.it.util.units

import java.util.regex.PatternSyntaxException
import sp.it.util.functional.Try
import sp.it.util.parsing.ConverterString

/** Amount within an amount. For example 15/20.  */
data class NofX(val n: Int, val of: Int): Comparable<NofX> {

   override fun compareTo(other: NofX): Int {
      val i = of.compareTo(other.of)
      return if (i!=0) i else n.compareTo(other.n)
   }

   fun toString(separator: String) = (if (n==-1) "?" else "$n") + separator + (if (of==-1) "?" else "$of")

   override fun toString() = toS(this)

   companion object: ConverterString<NofX> {

      override fun toS(o: NofX) = o.toString("/")

      override fun ofS(s: String): Try<NofX, String> {
         val a = s.split("/")
         return when {
            a.size!=2 -> Try.error("'Text=$s' is not in an 'x/y' format")
            else -> try {
               Try.ok(NofX(a[0].toInt(), a[1].toInt()))
            } catch (e: PatternSyntaxException) {
               Try.error(e.message ?: "Unknown error")
            } catch (e: NumberFormatException) {
               Try.error(e.message ?: "Unknown error")
            }
         }
      }
   }

}