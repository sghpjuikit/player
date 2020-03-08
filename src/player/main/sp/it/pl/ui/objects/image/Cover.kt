package sp.it.pl.ui.objects.image

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import sp.it.pl.image.ImageStandardLoader
import sp.it.util.dev.Blocks
import sp.it.util.ui.image.ImageSize
import java.awt.image.BufferedImage
import java.io.File
import java.util.Objects

interface Cover {

   /** @return if [getImage] is guaranteed to return null */
   fun isEmpty(): Boolean

   /** @return the cover image in the request size (not guaranteed) or null if none */
   @Blocks
   fun getImage(width: Double, height: Double): Image?

   /** @return the cover image in the request size (not guaranteed) or null if none */
   @Blocks
   fun getImage(size: ImageSize): Image? = getImage(size.width, size.height)

   /** @return the cover image or null if none */
   @Blocks
   fun getImage(): Image?

   /** @return file denoting the image or null if none */
   fun getFile(): File?

   enum class CoverSource {
      /** Use tag as cover source */
      TAG,
      /** Use parent directory image as source */
      DIRECTORY,
      /** Use all of the sources in their respective order and return first find */
      ANY
   }

}

/** Empty cover. */
object EmptyCover: Cover {
   override fun getImage() = null
   override fun getFile() = null
   override fun isEmpty(): Boolean = true
   override fun getImage(width: Double, height: Double) = null
   override fun getImage(size: ImageSize) = null
}

/** Cover represented by a [java.io.File]. */
data class FileCover(private val file: File?, private val description: String? = null): Cover {
   override fun getImage() = ImageStandardLoader(file)
   override fun getImage(width: Double, height: Double) = if (file==null) null else ImageStandardLoader(file, ImageSize(width, height))
   override fun getFile() = file
   override fun isEmpty() = file==null
}


/** Cover represented by a [javafx.scene.image.Image] or [java.awt.image.BufferedImage]. */
class ImageCover: Cover {
   private val imageI: Image?
   private val imageB: BufferedImage?
   /** Human readable information about the cover or null if none. No guarantees about the format. */
   val description: String?

   constructor(image: Image?, description: String?) {
      this.imageI = image
      this.imageB = null
      this.description = description
   }

   constructor(image: BufferedImage?, description: String?) {
      this.imageB = image
      this.imageI = null
      this.description = description
   }

   override fun getImage(): Image? = if (imageB==null) imageI else SwingFXUtils.toFXImage(imageB, null)

   override fun getImage(width: Double, height: Double): Image? = getImage()

   override fun getFile(): Nothing? = null

   override fun isEmpty() = imageB==null && imageI==null

   override fun equals(other: Any?): Boolean {
      if (other===this) return true
      return other is ImageCover && description==other.description && imageI==other.imageI && bufferedImagesEqual(imageB, other.imageB)
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