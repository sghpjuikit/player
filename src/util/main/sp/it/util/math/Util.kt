package sp.it.util.math

/** Golden ratio - `1.6180339887`. */
const val GOLDEN_RATIO = 1.6180339887

/**
 * Equivalent to [Boolean.or]
 *
 * Infix operator precedence is the same as boolean operators:
 *
 * '`false times false || true`' is equivalent to '`(false times false) || true`'
 *
 * '`true || false times false`' is equivalent to '`true || (false times false)`'
 *
 * '`false times false plus true`' is equivalent to '`(false times false) plus true`'
 */
infix operator fun Boolean.plus(that: Boolean) = this || that

/**
 * Equivalent to [Boolean.and]
 *
 * Infix operator precedence is the same as boolean operators:
 *
 * '`false times false || true`' is equivalent to '`(false times false) || true`'
 *
 * '`true || false times false`' is equivalent to '`true || (false times false)`'
 *
 * '`false times false plus true`' is equivalent to '`(false times false) plus true`'
 */
infix operator fun Boolean.times(that: Boolean) = this && that

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

/**
 * Equivalent to [coerceIn] or `minimum max this min maximum`
 */
fun <T: Comparable<T>> T.clip(minimum: T, maximum: T) = coerceIn(minimum, maximum)