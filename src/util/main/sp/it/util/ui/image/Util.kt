package sp.it.util.ui.image

import java.awt.image.BufferedImage as ImageBf
import javafx.scene.image.Image as ImageFx
import javafx.scene.image.WritableImage as ImageWr
import com.twelvemonkeys.image.ResampleOp
import java.awt.Dimension
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.IntBuffer
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Rectangle2D
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.ImageInputStream
import mu.KotlinLogging
import sp.it.util.async.runFX
import sp.it.util.dev.Blocks
import sp.it.util.dev.failCase
import sp.it.util.dev.failIf
import sp.it.util.dev.failIfFxThread
import sp.it.util.file.toURLOrNull
import sp.it.util.functional.Try
import sp.it.util.functional.getOr
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.math.max
import sp.it.util.type.Util.setField
import sp.it.util.ui.x
import sp.it.util.ui.x2

private val logger = KotlinLogging.logger {}

fun ImageWr.withUrl(url: String?): ImageWr = apply { setField(this, "url", url) }

fun ImageWr.withUrl(file: File?): ImageWr = apply { setField(this, "url", file?.toURLOrNull()?.toString()) }

@JvmOverloads
fun ImageBf.toFX(file: File? = null): ImageWr = SwingFXUtils.toFXImage(this, null).withUrl(file)

// https://github.com/javafxports/openjdk-jfx/pull/472#issuecomment-500547180
@JvmOverloads
fun ImageBf.toFXCustom(file: File? = null): ImageWr {
   val bb = IntBuffer.allocate(width*height)
   val pb = PixelBuffer(width, height, bb, PixelFormat.getIntArgbPreInstance())
   getRGB(0, 0, width, height, bb.array(), 0, width)
   runFX {
      pb.updateBuffer { Rectangle2D(0.0, 0.0, width.toDouble(), height.toDouble()) }
   }
   return ImageWr(pb).withUrl(file)
}

@JvmOverloads
fun ImageFx.toBuffered(to: ImageBf? = null): ImageBf? = SwingFXUtils.fromFXImage(this, to)

/** Scales image to requested size. Size must not be 0. */
@Suppress("KotlinConstantConditions")
private fun ImageBf.toScaledDown(W: Int, H: Int, down: Boolean, up: Boolean): ImageBf = when {
   !down && !up -> this
   down && up -> {
      val isNecessary = width!=W || height!=H
      if (isNecessary) {
         val imgScaled = ResampleOp(W, H).filter(this, null)
         flush()
         imgScaled
      } else {
         this
      }
   }
   up || down -> {
      val isNecessary = (up && (width<W || height<H)) || (down && (width>W || height>H))
      if (isNecessary) {
         val iRatio = width.toDouble()/height
         val rRatio = W.toDouble()/H
         val rW = if (iRatio<rRatio) W else (H*iRatio).toInt()
         val rH = if (iRatio<rRatio) (W/iRatio).toInt() else H
         val imgScaled = ResampleOp(rW, rH).filter(this, null)
         flush()
         imgScaled
      } else {
         this
      }
   }
   else -> failCase(up to down)
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

object Interrupts {

   val isInterrupted: Boolean
      get() = Thread.currentThread().isInterrupted

   fun interrupt(t: Thread) {
      failIf(!t.isVirtual) { "Can not interrupt non-virtual thread" }
      t.interrupt()
   }

}

fun loadImagePsd(file: File, inS: InputStream, width: Double, height: Double, highQuality: Boolean, scaleExact: Boolean) = loadImagePsd(file, runTry { ImageIO.createImageInputStream(inS.buffered()) }.orNull(), width, height, highQuality, scaleExact)

fun loadImagePsd(file: File, width: Double, height: Double, highQuality: Boolean, scaleExact: Boolean) = loadImagePsd(file, runTry { ImageIO.createImageInputStream(FileInputStream(file).buffered()) }.orNull(), width, height, highQuality, scaleExact)

private fun loadImagePsd(file: File, imgStream: ImageInputStream?, width: Double, height: Double, highQuality: Boolean, scaleExact: Boolean): ImageFx? {
   failIfFxThread()

   return imgStream?.use { stream ->

      if (Interrupts.isInterrupted) return@use null
      val reader = ImageIO.getImageReaders(stream).asSequence().firstOrNull() ?: return null
      reader.input = stream
      AutoCloseable { reader.dispose() }.use { _ ->
         var w = 0 max width.toInt()
         var h = 0 max height.toInt()
         val loadFullSize = w==0 && h==0
         val ii = reader.minIndex
         var i: ImageBf? = null
            ?: run {
               if (Interrupts.isInterrupted) null
               else if (!loadFullSize) {
                  runTry {
                     val thumbHas = imgImplHasThumbnail(reader, ii, file)
                     val thumbW = if (!thumbHas) 0 else reader.getThumbnailWidth(ii, 0)
                     val thumbH = if (!thumbHas) 0 else reader.getThumbnailHeight(ii, 0)
                     val thumbUse = thumbHas && w<=thumbW && h<=thumbH
                     if (thumbUse) reader.readThumbnail(ii, 0) else null
                  } orNull {
                     if (!Interrupts.isInterrupted) logger.warn(it) { "Failed to read thumbnail for image=$file" }
                  }
               } else null
            }
            ?: runTry {
               if (Interrupts.isInterrupted)
                  return@runTry null

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
                  val px = when {
                     !highQuality && rW!=0 && rH!=0 -> {
                        val sw = iW/rW
                        val sh = iH/rH
                        maxOf(1, minOf(sw, sh)/2) // quality == 2/3 == ok, great performance
                     }
                     else -> 1 // 1 == max quality
                  }

                  setSourceSubsampling(px, px, 0, 0)
               }

               if (Interrupts.isInterrupted) null
               else reader.read(ii, irp)
            } orNull {
               if (!Interrupts.isInterrupted) logger.warn(it) { "Failed to load image=$file" }
            }

         if (!loadFullSize)
            i = i?.toScaledDown(w, h, down = scaleExact, up = scaleExact)

         i?.toFX(file)
      }
   }

}

/**
 * Loads image file into as [ImageFx].
 *
 * The image loading executes on background thread if called on [javafx.application.Platform.isFxApplicationThread] or
 * current thread otherwise.
 */
fun imgImplLoadFX(file: File, size: ImageSize, scaleExact: Boolean = false): ImageFx {
   val rW = 0.0 max size.width
   val rH = 0.0 max size.height
   val loadFullSize = rW==0.0 && rH==0.0
   val isFxThread = Platform.isFxApplicationThread()
   return when {
      loadFullSize -> ImageFx(file.toURI().toString(), isFxThread)
      scaleExact -> {
         val imageSize = getImageDim(file).map { it.width x it.height }.getOr(Integer.MAX_VALUE.x2)
         val requestedSize = rW x rH
         val iRatio = imageSize.x/imageSize.y
         val rRatio = requestedSize.x/requestedSize.y
         val finalSize = if (iRatio<rRatio) requestedSize.x.x2/(1 x iRatio) else requestedSize.y.x2*(iRatio x 1)
         ImageFx(file.toURI().toString(), finalSize.x, finalSize.y, true, true, isFxThread)
      }
      else -> {
         val imageSize = getImageDim(file).map { it.width x it.height }.getOr(Integer.MAX_VALUE.x2)
         val requestedSize = if (rW>imageSize.x || rH>imageSize.y) imageSize else rW x rH
         val iRatio = imageSize.x/imageSize.y
         val rRatio = requestedSize.x/requestedSize.y
         val neededSize = if (iRatio<rRatio) requestedSize.x.x2/(1 x iRatio) else requestedSize.y.x2*(iRatio x 1)
         val sharpenSize = neededSize*1.0 // 1.5
         val finalSize = sharpenSize min imageSize // should not surpass real size (javafx.scene.Image would)
         ImageFx(file.toURI().toString(), finalSize.x, finalSize.y, true, true, isFxThread)
      }
   }
}

/**
 * Returns image size in pixels or error if unable to find out.
 * Involves an i/o operation, but does not read whole image into memory.
 */
@Blocks
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