package sp.it.pl.ui.objects.rating

import javafx.geometry.Pos
import javafx.scene.control.Control
import sp.it.util.access.V
import sp.it.util.access.v
import sp.it.util.math.clip

/** A control for allowing users to provide a rating as 0-1 double. */
class Rating @JvmOverloads constructor(iconCount: Int = 5, initialRating: Double? = null): Control() {

   /** Rating value in 0-1. Value will clipped to range. Default 0. */
   @JvmField val rating = object: V<Double?>(initialRating) {
      override fun set(nv: Double?) {
         super.set(nv?.let { it.clip(0.0, 1.0) })
      }
   }

   /** The alignment of the graphics. Default [Pos.CENTER]. */
   @JvmField val alignment = v(Pos.CENTER)

   /** The maximum-allowed rating value. Default 5. */
   @JvmField val icons = v(iconCount)

   /** If true this allows for users to set a rating as a floating point value in number of icons. */
   @JvmField val partialRating = v(false)

   /** Whether the value can be changed by user. */
   @JvmField val editable = v(true)

   /** Rating value handler called when user changes the value */
   @JvmField var onRatingEdited: (Double?) -> Unit = {}

   init {
      styleClass += "rating"
   }

   override fun createDefaultSkin() = RatingSkinStar(this)

}