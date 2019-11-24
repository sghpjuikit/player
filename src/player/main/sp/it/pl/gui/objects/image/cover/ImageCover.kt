package sp.it.pl.gui.objects.image.cover

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import java.awt.image.BufferedImage
import java.util.Objects

/**
 * Denotes Cover represented by a [javafx.scene.image.Image] or [java.awt.image.BufferedImage].
 */
class ImageCover: Cover {
   private val imageI: Image?
   private val imageB: BufferedImage?
   private val info: String

   constructor(image: Image?, description: String) {
      this.imageI = image
      this.imageB = null
      this.info = description
   }

   constructor(image: BufferedImage?, description: String) {
      this.imageB = image
      this.imageI = null
      this.info = description
   }

   override fun getImage(): Image? = if (imageB==null) imageI else SwingFXUtils.toFXImage(imageB, null)

   override fun getImage(width: Double, height: Double): Image? = image

   override fun getFile(): Nothing? = null

   override fun isEmpty() = imageB==null && imageI==null

   override fun getDescription() = info

   override fun equals(other: Any?): Boolean {
      if (other===this) return true
      return other is ImageCover && info==other.info && imageI==other.imageI && bufferedImagesEqual(imageB, other.imageB)
   }

   override fun hashCode(): Int {
      var hash = 3
      hash = 37*hash + Objects.hashCode(this.imageI)
      hash = 37*hash + Objects.hashCode(this.imageB)
      return hash
   }

   companion object {
      fun bufferedImagesEqual(img1: BufferedImage?, img2: BufferedImage?): Boolean = when {
            img1===img2 -> true
            img1==img2 -> true
            img1==null || img2==null -> false
            img1.width!=img2.width || img1.height!=img2.height -> false
            else -> {
               fun e(): Boolean {
                  for (x in 0 until img1.width) {
                     for (y in 0 until img1.height) {
                        if (img1.getRGB(x, y)!=img2.getRGB(x, y))
                           return false
                     }
                  }
                  return true
               }
               e()
            }
         }
   }
}