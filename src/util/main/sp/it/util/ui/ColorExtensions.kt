package sp.it.util.ui

import javafx.scene.paint.Color
import kotlin.math.roundToInt

/** @return color with same r,g,b values but specified opacity */
fun Color.alpha(opacity: Double): Color {
   return Color(red, green, blue, opacity)
}

/** @return hex value of this color, parsable by [Color.web] */
fun Color.toHex(): String {
   val r = (red*255.0).roundToInt()
   val g = (green*255.0).roundToInt()
   val b = (blue*255.0).roundToInt()
   val a = (opacity*255.0).roundToInt()
   return if (a==255) {
      String.format("#%02x%02x%02x", r, g, b)
   } else {
      String.format("#%02x%02x%02x%02x", r, g, b, a)
   }
}