package sp.it.util.math

/** Golden ratio - `1.6180339887`. */
const val GOLDEN_RATIO = 1.6180339887

/** Equivalent to [Boolean.or] */
infix operator fun Boolean.plus(that: Boolean) = this || that

/** Equivalent to [Boolean.and] */
infix operator fun Boolean.times(that: Boolean) = this && that

/** Equivalent to [Double.coerceAtLeast] */
infix fun Double.max(number: Double) = this.coerceAtLeast(number)

/** Equivalent to [Double.coerceAtMost] */
infix fun Double.min(number: Double) = this.coerceAtMost(number)