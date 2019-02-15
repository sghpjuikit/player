package sp.it.pl.util.math

infix fun Double.max(number: Double) = this.coerceAtLeast(number)
infix fun Double.min(number: Double) = this.coerceAtMost(number)