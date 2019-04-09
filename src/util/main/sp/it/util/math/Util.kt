package sp.it.util.math

/** Golden ratio - `1.6180339887`. */
const val GOLDEN_RATIO = 1.6180339887

infix fun Double.max(number: Double) = this.coerceAtLeast(number)
infix fun Double.min(number: Double) = this.coerceAtMost(number)