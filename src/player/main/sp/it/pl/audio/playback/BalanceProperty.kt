package sp.it.pl.audio.playback

import javafx.beans.property.SimpleDoubleProperty
import sp.it.pl.audio.playback.BalanceProperty.Companion.MAX
import sp.it.pl.audio.playback.BalanceProperty.Companion.MIN
import sp.it.util.math.clip

/** Double property for playback left-right volume balance. Value is always within valid range [MIN]-[MAX]. */
class BalanceProperty: SimpleDoubleProperty {

    /** Minimum volume value: [MIN] */
    val min get() = MIN
    /** Average volume value: [AVG] */
    val average get() = AVG
    /** Maximum volume value: [MAX] */
    val max get() = MAX

    constructor(v: Double): super(v.clip(MIN, MAX))

    /** Sets the value. Value outside of minimal-maximal value range will be clipped. */
    override fun set(v: Double) {
        super.set(v.clip(MIN, MAX))
    }

    /** Increment value by [STEP]. */
    fun leftByStep() {
        set(get()+STEP)
    }

    /** Decrement value by [STEP]. */
    fun rightByStep() {
        set(get()-STEP)
    }

    companion object {
        const val MIN = -1.0
        const val MAX = 1.0
        const val AVG = (MAX+MIN)/2
        const val STEP = 0.2
    }

}