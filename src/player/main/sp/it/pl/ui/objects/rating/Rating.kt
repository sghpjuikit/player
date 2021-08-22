package sp.it.pl.ui.objects.rating

import javafx.css.StyleConverter.getBooleanConverter
import javafx.css.StyleConverter.getSizeConverter
import javafx.geometry.Pos
import javafx.scene.control.Control
import sp.it.util.access.StyleableCompanion
import sp.it.util.access.V
import sp.it.util.access.sv
import sp.it.util.access.svMetaData
import sp.it.util.access.v
import sp.it.util.functional.asIs
import sp.it.util.math.clip

/** A control for allowing users to provide a rating as 0-1 double. */
class Rating @JvmOverloads constructor(initialRating: Double? = null): Control() {

   /** Whether the value can be changed by user. */
   val editable = v(true)
   /** Rating value in 0-1. Value will clipped to range. Default 0. */
   val rating = object: V<Double?>(initialRating) {
      override fun set(nv: Double?) {
         super.set(nv?.clip(0.0, 1.0))
      }
   }
   /** The alignment of the graphics. Default [Pos.CENTER]. */
   val alignment = v(Pos.CENTER)
   /** The maximum-allowed rating value. Default 5. */
   val icons by sv(ICON_COUNT)
   /** If true this allows for users to set a rating as a floating point value in number of icons. */
   val partialRating by sv(PARTIAL)
   /** Rating value handler called when user changes the value */
   var onRatingEdited: (Double?) -> Unit = {}

   init {
      styleClass += "rating"
   }

   override fun getControlCssMetaData() = classCssMetaData

   override fun createDefaultSkin() = RatingSkinStar(this)

   companion object: StyleableCompanion() {
      val ICON_COUNT by svMetaData<Rating, Int>("-fx-icon-count", getSizeConverter().asIs(), 5, Rating::icons)
      val PARTIAL by svMetaData<Rating, Boolean>("-fx-partial", getBooleanConverter(), false, Rating::partialRating)
   }
}