package sp.it.util.ui

import javafx.geometry.Rectangle2D
import sp.it.util.math.P

/** Left top corner of the bounds represented as point */
val Rectangle2D.min get() = P(minX, minY)

/** Bottom right corner of the bounds represented as point */
val Rectangle2D.max get() = P(maxX, maxY)

/** PositionLeft top corner of the bounds represented as point */
val Rectangle2D.xy get() = min

/** Size of the bounds represented as point */
val Rectangle2D.size get() = P(width, height)

/** Centre of this rectangle */
val Rectangle2D.centre get() = P(centreX, centreY)

/** X centre of this rectangle */
val Rectangle2D.centreX get() = minX + width/2

/** Y centre of this rectangle */
val Rectangle2D.centreY get() = minY + height/2

/** Range [Rectangle2D.minX]-[Rectangle2D.maxX] */
val Rectangle2D.x: ClosedFloatingPointRange<Double> get() = minX..maxX

/** Range [Rectangle2D.minY]-[Rectangle2D.maxY] */
val Rectangle2D.y: ClosedFloatingPointRange<Double> get() = minY..maxY