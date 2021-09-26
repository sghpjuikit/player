package sp.it.util.ui

import javafx.scene.paint.Color

/** @return color with same r,g,b values but specified opacity */
fun Color.alpha(opacity: Double): Color {
   return Color(red, green, blue, opacity)
}