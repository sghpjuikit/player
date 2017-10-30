package gui.objects.rating

import javafx.scene.control.Control
import util.Util.clip
import util.access.V
import util.access.v
import java.util.function.Consumer

/** A control for allowing users to provide a rating as 0-1 double. */
class Rating @JvmOverloads constructor(iconCount: Int = 5, initialRating: Double = 0.0): Control() {

    /** Rating value in 0-1. Value will clipped to range. Default 0. */
    @JvmField val rating = object: V<Double>(initialRating) {
        override fun set(nv: Double) {
            super.set(clip(0.0, nv, 1.0))
        }
    }

    /** The maximum-allowed rating value. Default 5. */
    @JvmField val icons = v(iconCount)

    /** If true this allows for users to set a rating as a floating point value in number of icons. */
    @JvmField val partialRating = v(false)

    /** Whether the value can be changed by user. */
    @JvmField val editable = v(true)

    /** Rating value handler called when user changes the value */
    @JvmField var onRatingEdited = Consumer<Double> {}

    init {
        styleClass.setAll("rating")
    }

    override fun createDefaultSkin() = RatingSkin(this)

}