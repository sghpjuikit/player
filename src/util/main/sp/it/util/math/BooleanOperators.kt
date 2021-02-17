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