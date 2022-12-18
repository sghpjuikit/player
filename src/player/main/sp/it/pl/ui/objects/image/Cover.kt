package sp.it.pl.ui.objects.image

import java.awt.image.BufferedImage
import java.io.File
import java.util.Objects
import javafx.scene.image.Image
import org.jaudiotagger.tag.images.Artwork
import sp.it.pl.audio.tagging.imageOrNull
import sp.it.pl.audio.tagging.info
import sp.it.pl.image.ImageStandardLoader
import sp.it.util.dev.Blocks
import sp.it.util.functional.orNull
import sp.it.util.ui.image.FitFrom
import sp.it.util.ui.image.FitFrom.OUTSIDE
import sp.it.util.ui.image.ImageLoadParamOfData
import sp.it.util.ui.image.ImageSize
import sp.it.util.ui.image.toFX

sealed interface Cover {

   /** @return if [getImage] is guaranteed to return null */
   fun isEmpty(): Boolean

   /** @return the cover image in the request size (not guaranteed) or null if none */
   @Blocks
   fun getImage(size: ImageSize, fit: FitFrom): Image?

   /** @return the cover image in original resolution or null if none */
   @Blocks
   fun getImageFullSize(): Image? = getImage(ImageSize(0, 0), OUTSIDE)

   /** @return file denoting the image or null if none */
   fun getFile(): File?

   enum class CoverSource {
      /** Use no cover source */
      NONE,
      /** Use tag as cover source */
      TAG,
      /** Use parent directory image as source */
      DIRECTORY,
      /** Use all sources in their respective order and return first find */
      ANY
   }

}

/** Empty cover. */
object EmptyCover: Cover {
   override fun getFile() = null
   override fun isEmpty(): Boolean = true
   override fun getImage(size: ImageSize, fit: FitFrom) = null
}

/** Cover represented by a [java.io.File]. */
data class FileCover(private val file: File?, private val description: String? = null): Cover {
   override fun getImage(size: ImageSize, fit: FitFrom) = ImageStandardLoader(file, size, fit)
   override fun getFile() = file
   override fun isEmpty() = file==null
}

/** Cover represented by a [javafx.scene.image.Image] or [java.awt.image.BufferedImage]. */
class ImageCover: Cover {
   private val imageI: Image?
   private val imageB: BufferedImage?
   /** Human-readable information about the cover or null if none. No guarantees about the format. */
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

   override fun getImage(size: ImageSize, fit: FitFrom): Image? = if (imageB==null) imageI else imageB.toFX(null)

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

class ArtworkCover: Cover {
   private val artwork: Artwork?
   /** Human-readable information about the cover or null if none. No guarantees about the format. */
   val description: String?

   constructor(image: Artwork?, description: String?) {
      this.artwork = image
      this.description = description
   }

   override fun getImage(size: ImageSize, fit: FitFrom): Image? = artwork?.imageOrNull(ImageLoadParamOfData(size, fit))?.orNull()

   override fun getFile(): Nothing? = null

   override fun isEmpty() = artwork==null

   override fun equals(other: Any?): Boolean {
      if (other===this) return true
      if (other !is ArtworkCover) return false
      if (other.artwork===artwork) return true
      return artwork?.width==other.artwork?.width && artwork?.height==other.artwork?.height && artwork?.info==other.artwork?.info && artwork?.binaryData.contentEquals(other.artwork?.binaryData)
   }

   override fun hashCode(): Int {
      var hash = 3
      hash = 37*hash + Objects.hashCode(this.artwork)
      return hash
   }

}