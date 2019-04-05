package sp.it.pl.util.ui.image

import java.awt.Dimension

data class ImageSize(@JvmField val width: Double, @JvmField val height: Double) {

    constructor(width: Int, height: Int): this(width.toDouble(), height.toDouble())

    constructor(size: Dimension): this(size.width, size.height)

}