package sp.it.util.ui

import javafx.scene.control.Slider

/** Range [Slider.min]..[Slider.max] */
val Slider.valueRange: ClosedFloatingPointRange<Double>
   get() = min..max

/** Absolute value, i.e., [Slider.value]. */
val Slider.valueAbs: Double
   get() = value

/** Relative value to [Slider.valueRange], value normalized to [0..1], i.e., (value-min)/(max-min). */
val Slider.valueRel: Double
   get() = (value-min)/(max-min)
