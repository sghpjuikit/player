package sp.it.util.ui.image

import java.awt.Dimension
import sp.it.util.functional.Try
import sp.it.util.parsing.ConverterString

data class ImageSize(@JvmField val width: Double, @JvmField val height: Double) {

   constructor(width: Int, height: Int): this(width.toDouble(), height.toDouble())

   constructor(size: Dimension): this(size.width, size.height)

   operator fun div(by: Double) = ImageSize(width/by, height/by)

   override fun toString() = toS(this)

   companion object: ConverterString<ImageSize> {

      override fun toS(o: ImageSize) = "${o.width} x ${o.height}"

      override fun ofS(s: String): Try<ImageSize, String> {
         val a = s.split("x")
         return when {
            a.size!=2 -> Try.error("'Text=$s' is not in an 'width x height' format")
            else -> try {
               Try.ok(ImageSize(a[0].trim().toDouble(), a[1].trim().toDouble()))
            } catch (e: Throwable) {
               Try.error(e.message ?: "Unknown error")
            }
         }
      }
   }
}