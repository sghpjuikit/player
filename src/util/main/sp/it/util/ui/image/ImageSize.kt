package sp.it.util.ui.image

import java.awt.Dimension

data class ImageSize(@JvmField val width: Double, @JvmField val height: Double) {

   constructor(width: Int, height: Int): this(width.toDouble(), height.toDouble())

   constructor(size: Dimension): this(size.width, size.height)

   operator fun div(by: Double) = ImageSize(width/by, height/by)

}