package sp.it.util.units

import java.time.Year
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.parsing.ConverterToString

/**
 * Range of year units.
 * Stores minimum value, maximum value and specificness - whether any value the range was created for was unspecified.
 * Has proper text representation.
 *
 * Mutable. Accumulating year values expands this range.
 */
class RangeYear {
   private var min = Integer.MAX_VALUE
   private var max = Integer.MIN_VALUE
   private var hasUnspecified = false

   /** Accumulate specific year value or unspecific value (null) into this range */
   fun accumulate(year: Year?) = accumulate(year?.value)

   /** Accumulate specific year value as int or unspecific value (null) into this range */
   fun accumulate(year: Int?) {
      if (year==null) {
         hasUnspecified = true
      } else {
         min = min min year
         max = max max year
      }
   }

   operator fun plusAssign(year: Int?) = accumulate(year)

   operator fun plusAssign(year: Year?) = accumulate(year)

   /** @return true iff no value has been accumulated, so both [hasSpecific] and [hasUnspecified] return false */
   fun isEmpty(): Boolean = !hasSpecific() && !hasUnSpecific()

   /** @return true iff all specific years in this range are lesser than the specified year */
   fun isAfter(y: Year) = hasSpecific() && min>y.value

   /** @return true iff all specific years in this range are greater than the specified year */
   fun isBefore(y: Year) = hasSpecific() && max<y.value

   /** @return true iff this range has some specific year >= and some specific year <= than the specified year */
   operator fun contains(y: Year) = hasSpecific() && min<=y.value && max>=y.value

   /** @return true iff any specific year has been accumulated to this range */
   fun hasSpecific() = max!=Integer.MIN_VALUE

   /** @return true iff any unspecific year has been accumulated to this range */
   fun hasUnSpecific() = hasUnspecified

   override fun toString() = toS(this)

   companion object: ConverterToString<RangeYear> {

      override fun toS(o: RangeYear) = when {
         o.isEmpty() -> "<none>"
         o.hasSpecific() -> {
            when (o.min==o.max) {
               true -> (if (o.hasUnspecified) "? " else "") + o.max
               false -> "" + o.min + (if (o.hasUnspecified) " ? " else " - ") + o.max
            }
         }
         else -> ""
      }

   }
}