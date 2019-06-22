package sp.it.util.math

/** Golden ratio - `1.6180339887`. */
const val GOLDEN_RATIO = 1.6180339887

/** Equivalent to [Boolean.or] */
infix operator fun Boolean.plus(that: Boolean) = this || that

/** Equivalent to [Boolean.and] */
infix operator fun Boolean.times(that: Boolean) = this && that

/** Equivalent to [coerceAtLeast] */
infix fun <T : Comparable<T>> T.max(that: T) = this.coerceAtLeast(that)

/** Equivalent to [coerceAtMost] */
infix fun <T : Comparable<T>> T.min(that: T) = this.coerceAtMost(that)