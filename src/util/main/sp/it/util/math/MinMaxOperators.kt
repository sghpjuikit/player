package sp.it.util.math

/**
 * Equivalent to [coerceAtLeast]
 *
 * Infix operator precedence is lower than arithmetic operators:
 *
 * '`a * b - c max d * e`' is equivalent to '`(a * b - c) max (d * e)`'
 *
 * '`1.0 max 3.0 * 0.1 min 0.5 + 10.0`' is equivalent to '`1.0 max (3.0 * 0.1) min (0.5 + 10.0)`'
 *
 * '`1.0 max 3.0 min 0.5`' is equivalent to '`(1.0 max 3.0) min 0.5`'
 */
infix fun <T: Comparable<T>> T.max(that: T) = coerceAtLeast(that)

/**
 * Equivalent to [coerceAtMost]
 *
 * Infix operator precedence is lower than arithmetic operators:
 *
 * '`a * b - c min d * e`' is equivalent to '`(a * b - c) min (d * e)`'
 *
 * '`1.0 max 3.0 * 0.1 min 0.5 + 10.0`' is equivalent to '`1.0 max (3.0 * 0.1) min (0.5 + 10.0)`'
 *
 * '`1.0 max 3.0 min 0.5`' is equivalent to '`(1.0 max 3.0) min 0.5`'
 */
infix fun <T: Comparable<T>> T.min(that: T) = coerceAtMost(that)

/** Equivalent to [coerceIn] or `minimum max this min maximum` */
fun <T: Comparable<T>> T.clip(minimum: T, maximum: T) = coerceIn(minimum, maximum)

/** @return this number in modular math (e.g. 3 in mod 2 is 1) */
infix fun Int.toMod(mod: Int): Int {
   var number = this
   while (number < 0) number += mod
   return this % mod
}

/** @return this number in modular math (e.g. 3 in mod 2 is 1) */
infix fun Long.toMod(mod: Long): Long {
   var number = this
   while (number < 0) number += mod
   return this % mod
}