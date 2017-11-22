package sp.it.pl.util.math

import javafx.util.Duration

class Portion(v: Double = 0.0) {
    private val value = v.coerceIn(RANGE)

    operator fun times(number: Double): Double = number*value

    operator fun times(dur: Duration): Duration = dur.multiply(value)

    companion object {
        private val RANGE = 0.0.rangeTo(1.0)
    }
}