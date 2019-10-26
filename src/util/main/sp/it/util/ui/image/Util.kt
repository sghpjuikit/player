@file:Suppress("unused")

package sp.it.util.ui.image

import com.twelvemonkeys.image.ResampleOp
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import mu.KotlinLogging
import sp.it.util.dev.failIfFxThread
import sp.it.util.functional.Try
import sp.it.util.functional.getOr
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.ui.x
import sp.it.util.ui.x2
import java.awt.Dimension
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.ImageInputStream
import javafx.scene.image.Image as ImageFx
import javafx.scene.image.WritableImage as ImageWr
import java.awt.image.BufferedImage as ImageBf

private val logger = KotlinLogging.logger {}

@JvmOverloads
fun ImageBf.toFX(to: ImageWr? = null) = SwingFXUtils.toFXImage(this, to)!!

@JvmOverloads
fun ImageFx.toBuffered(to: ImageBf? = null): ImageBf? = SwingFXUtils.fromFXImage(this, to)

/** Scales image down to requested size. Size must not be 0. */
private fun ImageBf.toScaledDown(W: Int, H: Int): ImageBf {
   return if (width<=W || height<=H) {
      this
   } else {
      val iW = width
      val iH = height
      val iRatio = iW.toDouble()/iH
      val rRatio = W.toDouble()/H
      val rW = if (iRatio<rRatio) W else (H*iRatio).toInt()
      val rH = if (iRatio<rRatio) (W/iRatio).toInt() else H
      val imgScaled = ResampleOp(rW, rH).filter(this, null)
      flush()
      imgScaled
   }
}

/** Returns true if the image has at least 1 embedded thumbnail of any size.  */
private fun imgImplHasThumbnail(reader: ImageReader, index: Int, f: File): Boolean {
   return try {
      reader.readerSupportsThumbnails() && reader.hasThumbnails(index) // throws exception -> no thumb
   } catch (e: Exception) {
      // Catching IOException should be enough, but TwelveMonkeys library can screw up rarely
      logger.warn(e) { "Can't find image thumbnails $f" }
      false
   }
}

fun loadBufferedImage(file: File): Try<ImageBf, IOException> {
   return try {
      Try.ok(ImageIO.read(file))
   } catch (e: IOException) {
      logger.error(e) { "Can't read image $file for tray icon" }
      Try.error(e)
   }
}

fun loadImageThumb(file: File?, width: Double, height: Double): ImageFx? {
   if (file==null) return null

   // negative values have same effect as 0, 0 loads image at its size
   val w = maxOf(0, width.toInt())
   val h = maxOf(0, height.toInt())
   val loadFullSize = w==0 && h==0

   // psd special case
   return if (!file.path.endsWith(".psd", true)) {
      imgImplLoadFX(file, w, h, loadFullSize)
   } else {
      loadImagePsd(file, width, height, false)
   }
}

fun loadImagePsd(file: File, inS: InputStream, width: Double, height: Double, highQuality: Boolean) = loadImagePsd(file, ImageIO.createImageInputStream(inS), width, height, highQuality)

fun loadImagePsd(file: File, width: Double, height: Double, highQuality: Boolean) = loadImagePsd(file, ImageIO.createImageInputStream(file), width, height, highQuality)

private fun loadImagePsd(file: File, imageInputStream: ImageInputStream?, width: Double, height: Double, highQuality: Boolean): ImageFx? {
   failIfFxThread()

   val stream = imageInputStream ?: return null
   val reader = ImageIO.getImageReaders(stream).asSequence().firstOrNull() ?: return null
   reader.input = stream

   // negative values have same effect as 0, 0 loads image at its size
   var w = maxOf(0, width.toInt())
   var h = maxOf(0, height.toInt())
   val loadFullSize = w==0 && h==0
   val scale = !loadFullSize
   val ii = reader.minIndex
   var i: ImageBf? = null
      ?: run {
         if (!loadFullSize) {
            runTry {
               val thumbHas = imgImplHasThumbnail(reader, ii, file)
               val thumbW = if (!thumbHas) 0 else reader.getThumbnailWidth(ii, 0)
               val thumbH = if (!thumbHas) 0 else reader.getThumbnailHeight(ii, 0)
               val thumbUse = thumbHas && w<=thumbW && h<=thumbH
               if (thumbUse) {
                  reader.readThumbnail(ii, 0)
               }
            } orNull {
               logger.warn(it) { "Failed to read thumbnail for image=$file" }
            }
         }
         null
      }
      ?: run {
         val iW = reader.getWidth(ii)
         val iH = reader.getHeight(ii)
         if (w>iW || h>iH) {
            w = iW
            h = iH
         }
         val iRatio = iW.toDouble()/iH.toDouble()
         val rRatio = w.toDouble()/h.toDouble()
         val rW = if (iRatio<rRatio) w else (h*iRatio).toInt()
         val rH = if (iRatio<rRatio) (w/iRatio).toInt() else h

         val irp = reader.defaultReadParam.apply {
            var px = 1
            if (!highQuality && rW!=0 && rH!=0) {
               val sw = reader.getWidth(ii)/rW
               val sh = reader.getHeight(ii)/rH
               px = maxOf(1, maxOf(sw, sh)/3) // quality == 2/3 == ok, great performance
            }
            // max quality is px==1, but quality/performance ratio would suck
            setSourceSubsampling(px, px, 0, 0)
         }

         runTry {
            reader.read(ii, irp)
         } orNull {
            logger.warn(it) { "Failed to load image=$file" }
         }
      }
   reader.dispose()

   if (scale)
      i = i?.toScaledDown(w, h)

   return i?.toFX()
}

/**
 * Loads image file into a javaFx's [Image].
 *
 * The image loading executes on background thread if called on
 * [javafx.application.Platform.isFxApplicationThread] or current thread otherwise, so to not block or
 * overwhelm the fx ui thread.
 */
fun imgImplLoadFX(file: File, W: Int, H: Int, loadFullSize: Boolean): ImageFx {
   val isFxThread = Platform.isFxApplicationThread()
   return if (loadFullSize) {
      ImageFx(file.toURI().toString(), isFxThread)
   } else {
      var requestedSize = (W x H) max (0 x 0)
      val imageSize = getImageDim(file).map { it.width x it.height }.getOr(Integer.MAX_VALUE.x2)
      if (requestedSize.x>imageSize.x || requestedSize.y>imageSize.y) {
         requestedSize = imageSize
      }
      val iRatio = imageSize.x/imageSize.y
      val rRatio = requestedSize.x/requestedSize.y
      val neededSize = if (iRatio<rRatio) requestedSize.x.x2/(1 x iRatio) else requestedSize.y.x2*(iRatio x 1)
      val sharpenSize = neededSize*1.5
      val finalSize = sharpenSize min imageSize // should not surpass real size (javafx.scene.Image would)
      ImageFx(file.toURI().toString(), finalSize.x, finalSize.y, true, true, isFxThread)
   }
}

/**
 * Returns image size in pixels or error if unable to find out.
 * Does i/o, but does not read whole image into memory.
 */
fun getImageDim(f: File): Try<Dimension, Throwable> {
   val stream = ImageIO.createImageInputStream(f) ?: return run {
      logger.warn { "Problem finding out image size for $f, could not create image input stream" }
      Try.error(RuntimeException("No reader found for $f"))
   }
   val reader = ImageIO.getImageReaders(stream).asSequence().firstOrNull() ?: return run {
      logger.warn { "Problem finding out image size for $f, no image reader found" }
      runTry { stream.close() }
      Try.error(RuntimeException("No image reader found for $f"))
   }
   return runTry {
      reader.input = stream
      val ii = reader.minIndex // 1st image index
      val width = reader.getWidth(ii)
      val height = reader.getHeight(ii)
      Dimension(width, height)
   }.ifAny {
      reader.dispose()
   }.ifError {
      logger.warn(it) { "Problem finding out image size for $f" }
   }
}

/** @return new black image of specified size */
fun createImageBlack(size: ImageSize) = ImageBf(size.width.toInt(), size.height.toInt(), ImageBf.TYPE_INT_RGB)

/** @return new transparent image of specified size */
fun createImageTransparent(size: ImageSize) = ImageBf(size.width.toInt(), size.height.toInt(), ImageBf.TYPE_INT_ARGB)