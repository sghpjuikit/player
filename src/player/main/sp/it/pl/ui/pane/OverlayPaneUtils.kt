package sp.it.pl.ui.pane

import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import kotlin.math.max
import kotlin.math.min

/** @return expanded image by given offset with content set to pixel colors at image's edge to avoid blur artefacts */
fun Image.adjustForBlur(blur: Int): WritableImage {
   val originalImg = this
   val originalPixels = originalImg.pixelReader
   val originalWidth = originalImg.width.toInt()
   val originalHeight = originalImg.height.toInt()

   val newWidth = originalWidth + (2 * blur)
   val newHeight = originalHeight + (2 * blur)
   val newImg = WritableImage(newWidth, newHeight)
   val newPixels = newImg.pixelWriter

   // Copy the original image's pixels to the new image, but add the surrounding pixels
   for (x in 0 until newWidth) {
      for (y in 0 until newHeight) {
         val color = if (x < blur || x >= newWidth - blur ||
            y < blur || y >= newHeight - blur) {
            // If the pixel is outside the original image,
            // set it to the color at the edge of the original image
            val edgeX = min(max(x - blur, 0), originalWidth - 1)
            val edgeY = min(max(y - blur, 0), originalHeight - 1)
            originalPixels.getColor(edgeX, edgeY)
         } else {
            // Otherwise, set it to the original image's pixel value
            originalPixels.getColor(x - blur, y - blur)
         }
         newPixels.setColor(x, y, color)
      }
   }

   return newImg
}