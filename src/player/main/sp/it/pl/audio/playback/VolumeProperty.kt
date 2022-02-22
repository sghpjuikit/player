package sp.it.pl.audio.playback

import javafx.beans.property.SimpleDoubleProperty
import sp.it.pl.audio.playback.VolumeProperty.Companion.MAX
import sp.it.pl.audio.playback.VolumeProperty.Companion.MIN
import sp.it.pl.main.Double01
import sp.it.util.math.clip

/** Double property for playback volume. Value is always within valid range [MIN]-[MAX]. */
class VolumeProperty: SimpleDoubleProperty {

   /** Minimum volume value: [MIN] */
   val min: Double01 get() = MIN
   /** Average volume value: [AVG] */
   val average: Double01 get() = AVG
   /** Maximum volume value: [MAX] */
   val max: Double01 get() = MAX

   constructor(v: Double01): super(v.clip(MIN, MAX))

   /** Sets the value. Value outside of minimal-maximal value range will be clipped. */
   override fun set(v: Double01) {
      super.set(v.clip(MIN, MAX))
   }

   /** Increment value by [STEP]. */
   fun incByStep() = set(get() + STEP)

   /** Decrement value by [STEP]. */
   fun decByStep() = set(get() - STEP)

   companion object {
      const val MIN = 0.0
      const val MAX = 1.0
      const val AVG = 0.5
      const val STEP = 0.1

      /** @return logarithmic 0-1 value from linear 0-1 value */
      @JvmStatic fun linToLog(v: Double01): Double = v*v

   }

}