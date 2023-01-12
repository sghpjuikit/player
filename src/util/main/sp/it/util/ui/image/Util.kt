package sp.it.util.ui.image

import java.awt.image.BufferedImage as ImageBf
import javafx.scene.image.Image as ImageFx
import javafx.scene.image.WritableImage as ImageWr
import com.drew.imaging.gif.GifMetadataReader
import com.drew.metadata.gif.GifControlDirectory
import com.twelvemonkeys.image.ResampleOp
import java.awt.Dimension
import java.awt.Rectangle
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.IntBuffer
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.ZipFile
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Rectangle2D
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.event.IIOReadProgressListener
import javax.imageio.stream.ImageInputStream
import kotlin.concurrent.withLock
import mu.KotlinLogging
import sp.it.util.async.runFX
import org.jetbrains.annotations.Blocking
import sp.it.util.dev.fail
import sp.it.util.dev.failCase
import sp.it.util.file.toURLOrNull
import sp.it.util.file.type.MimeType
import sp.it.util.file.type.MimeType.Companion.`application∕x-krita`
import sp.it.util.file.type.MimeType.Companion.`image∕gif`
import sp.it.util.file.type.MimeType.Companion.`image∕webp`
import sp.it.util.file.type.mimeType
import sp.it.util.functional.Try
import sp.it.util.functional.getOr
import sp.it.util.functional.ifNull
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.math.max
import sp.it.util.type.Util.getFieldValue
import sp.it.util.type.Util.setField
import sp.it.util.ui.image.FitFrom.INSIDE
import sp.it.util.ui.image.FitFrom.OUTSIDE
import sp.it.util.ui.x
import sp.it.util.ui.x2

private val logger = KotlinLogging.logger {}

fun ImageWr.withUrl(url: String?): ImageWr = apply { setField(this, "url", url) }

fun ImageWr.withUrl(file: File?): ImageWr = apply { setField(this, "url", file?.toURLOrNull()?.toString()) }

@JvmOverloads
fun ImageBf.toFX(file: File? = null): ImageWr = SwingFXUtils.toFXImage(this, null).withUrl(file)

@JvmOverloads
fun ImageBf.toFxAndFlush(file: File? = null): ImageWr = toFX(file).also { flush() }

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
private fun imgImplHasThumbnail(reader: ImageReader, index: Int, f: File?): Boolean {
   return try {
      reader.readerSupportsThumbnails() && reader.hasThumbnails(index) // throws exception -> no thumb
   } catch (e: Exception) {
      // Catching IOException should be enough, but TwelveMonkeys library can screw up rarely
      logger.warn(e) { "Can't find image thumbnails for file=$f" }
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

/** Thread interruption without [Thread.interrupt]. Avoids unpredictable exceptions and allows exception-free interrupting. */
object Interrupts {
   private val lock = ReentrantLock()
   @Volatile private var interrupts = arrayOf<Thread>()

   /** Whether current thread is interrupted */
   val isInterrupted: Boolean get() = Thread.currentThread() in interrupts

   fun dispose(t: Thread? = Thread.currentThread()) {
      if (t==null) return
      lock.withLock {
         if (t in interrupts) interrupts = interrupts.filter { it!==it }.toTypedArray()
      }
   }

   /** Interrupts the specified thread, by default current. */
   fun interrupt(t: Thread? = Thread.currentThread()) {
      if (t==null) return
      lock.withLock {
         if (t !in interrupts) interrupts += Thread.currentThread()
      }
   }
}


sealed interface ImageLoadParam { val file: File?; val size: ImageSize; val fit: FitFrom; val scaleExact: Boolean }
data class ImageLoadParamOfData(override val size: ImageSize, override val fit: FitFrom, override val scaleExact: Boolean = false): ImageLoadParam { override val file: Nothing? = null }
data class Params(override val file: File, override val size: ImageSize, override val fit: FitFrom, val mime: MimeType, override val scaleExact: Boolean = false): ImageLoadParam

private fun ImageInputStream.reader(): ImageReader? = ImageIO.getImageReaders(this).asSequence().firstOrNull()?.apply { input = this@reader; abortOnInterrupt() }

private fun <R> ImageReader.use(block: (ImageReader) -> R): R = AutoCloseable { dispose() }.use { block(this) }

private fun ImageReader.abortOnInterrupt() {
   addIIOReadProgressListener(
      object: IIOReadProgressListener {
         override fun sequenceStarted(r: ImageReader?, minIndex: Int) = if (Interrupts.isInterrupted) abort() else Unit
         override fun sequenceComplete(r: ImageReader?) = Unit
         override fun imageStarted(r: ImageReader?, imageIndex: Int) = if (Interrupts.isInterrupted) abort() else Unit
         override fun imageProgress(r: ImageReader?, percentageDone: Float) = if (Interrupts.isInterrupted) abort() else Unit
         override fun imageComplete(r: ImageReader?) = Unit
         override fun thumbnailStarted(r: ImageReader?, imageIndex: Int, thumbnailIndex: Int) = if (Interrupts.isInterrupted) abort() else Unit
         override fun thumbnailProgress(r: ImageReader?, percentageDone: Float) = if (Interrupts.isInterrupted) abort() else Unit
         override fun thumbnailComplete(r: ImageReader?) = Unit
         override fun readAborted(r: ImageReader?) = Unit
      }
   )
}

fun loadImagePsd(inS: InputStream, p: ImageLoadParam, highQuality: Boolean) = loadImagePsd(runTry { ImageIO.createImageInputStream(inS.buffered()) }.orNull(), p, highQuality)

fun loadImagePsd(p: Params, highQuality: Boolean) = loadImagePsd(runTry { ImageIO.createImageInputStream(FileInputStream(p.file).buffered()) }.orNull(), p, highQuality)

@Suppress("UNUSED_PARAMETER")
private fun loadImagePsd(imgStream: ImageInputStream?, p: ImageLoadParam, highQuality: Boolean): ImageFx? =
   imgStream?.use { stream ->
      stream.reader()?.use { reader ->
         var w = 0 max p.size.width.toInt()
         var h = 0 max p.size.height.toInt()
         val loadFullSize = w==0 && h==0
         val ii = reader.minIndex
         var i: ImageBf? = null
            ?: run {
               if (Interrupts.isInterrupted) null
               else if (!loadFullSize) {
                  runTry {
                     val tExists = imgImplHasThumbnail(reader, ii, p.file)
                     val tW = if (!tExists) 1 else reader.getThumbnailWidth(ii, 0)
                     val tH = if (!tExists) 1 else reader.getThumbnailHeight(ii, 0)
                     val tUse = tExists && w<=tW && h<=tH
                     val tRatio = tW.toDouble()/tH.toDouble()
                     val rRatio = w.toDouble()/h.toDouble()
                     val (sW, sH) = when {
                        p.fit==OUTSIDE && tRatio<rRatio -> tW to (tW/rRatio).toInt()
                        p.fit==OUTSIDE && tRatio>rRatio -> (tH*rRatio).toInt() to tH
                        else -> tW to tH
                     }
                     if (tUse) reader.readThumbnail(ii, 0).getSubimage((tW - sW)/2, (tH - sH)/2, sW, sH)
                     else null
                  } orNull {
                     logger.warn(it) { "Failed to read thumbnail for image=${p.file}" }
                  }
               } else
                  null
            }
            ?: runTry {
               val (iW, iH) = reader.getWidth(ii) to reader.getHeight(ii)
               if (w>iW || h>iH) { w = iW; h = iH }
               val iRatio = iW.toDouble()/iH.toDouble()
               val rRatio = w.toDouble()/h.toDouble()
               val rW = if (iRatio<rRatio) w else (h*iRatio).toInt()
               val rH = if (iRatio<rRatio) (w/iRatio).toInt() else h
               val irp = reader.defaultReadParam.apply {
                  val (sW, sH) = when {
                     p.fit==OUTSIDE && iRatio<rRatio -> iW to (iW/rRatio).toInt()
                     p.fit==OUTSIDE && iRatio>rRatio -> (iH*rRatio).toInt() to iH
                     else -> iW to iH
                  }
                  val ss = if (rW==0 || rH==0) 1 else when (p.fit) {
                     OUTSIDE -> maxOf(1, minOf(iW/rW, iH/rH)/2)
                     INSIDE -> maxOf(1, maxOf(iW/w, iH/h)/2)
                  }
                  setSourceSubsampling(ss, ss, 0, 0)
                  sourceRegion = Rectangle((iW - sW)/2, (iH - sH)/2, sW, sH)
               }

               if (Interrupts.isInterrupted) null
               else reader.read(ii, irp)
            } orNull {
               logger.warn(it) { "Failed to load image=${p.file}" }
            }

         if (!loadFullSize && p.scaleExact)
            i = i?.toScaledDown(w, h, down = p.scaleExact, up = p.scaleExact)

         i?.toFxAndFlush(p.file)
      }
   }

data class ImageFrame(val delayMs: Int, val durationMs: Int, val img: ImageFx)

fun loadImageFrames(inS: InputStream, p: Params): List<ImageFrame>? = loadImageFrames(runTry { ImageIO.createImageInputStream(inS.buffered()) }.orNull(), p)

fun loadImageFrames(p: Params): List<ImageFrame>? = loadImageFrames(runTry { ImageIO.createImageInputStream(FileInputStream(p.file).buffered()) }.orNull(), p)

fun loadImageFrames(imgStream: ImageInputStream?, p: Params): List<ImageFrame>? = when (p.mime) {
   `image∕gif`, `image∕webp` ->
      imgStream?.use { stream ->
         stream.reader()?.use { reader ->
            runTry {
               var w = 0 max p.size.width.toInt()
               var h = 0 max p.size.height.toInt()
               var delay = 0
               (reader.minIndex..Int.MAX_VALUE).asSequence()
                  .map {
                     runTry {
                        val (iW, iH) = reader.getWidth(it) to reader.getHeight(it)
                        val duration = reader.getDuration(it, p.mime)
                        delay += duration
                        if (w>iW || h>iH) { w = iW; h = iH }
                        val iRatio = iW.toDouble()/iH.toDouble()
                        val rRatio = w.toDouble()/h.toDouble()
                        val rW = if (iRatio<rRatio) w else (h*iRatio).toInt()
                        val rH = if (iRatio<rRatio) (w/iRatio).toInt() else h
                        val irp = reader.defaultReadParam.apply {
                           val (sW, sH) = when {
                              p.fit==OUTSIDE && iRatio<rRatio -> iW to (iW/rRatio).toInt()
                              p.fit==OUTSIDE && iRatio>rRatio -> (iH*rRatio).toInt() to iH
                              else -> iW to iH
                           }
                           val ss = if (rW==0 || rH==0) 1 else when (p.fit) {
                              OUTSIDE -> maxOf(1, minOf(iW/rW, iH/rH)/2)
                              INSIDE -> maxOf(1, maxOf(iW/w, iH/h)/2)
                           }
                           setSourceSubsampling(ss, ss, 0, 0)
                           sourceRegion = Rectangle((iW - sW)/2, (iH - sH)/2, sW, sH)
                        }

                        if (Interrupts.isInterrupted) null
                        else reader.read(it, irp)?.toFxAndFlush(p.file)?.net { ImageFrame(delay, duration, it) }
                     }
                  }
                  .takeWhile { !Interrupts.isInterrupted }
                  .takeWhile { it is Try.Ok && it.value!=null }
                  .takeWhile { !(it is Try.Error<*> && it.value is IndexOutOfBoundsException) }
                  .map { it.orThrow!! }
                  .toList()
            } orNull {
               logger.warn(it) { "Failed to load image=${p.file}" }
            }
         }
      }
   else -> null
}

/** @return true iff the image file is animated */
@Throws
private fun ImageReader.getDuration(ii: Int, fMime: MimeType): Int = when (fMime) {
   `image∕gif` -> getFieldValue(getFieldValue(this, "imageMetadata"), "delayTime")
   `image∕webp` -> getFieldValue(getFieldValue<List<Any>>(this, "frames")[ii], "duration")
   else -> 0
}

/** @return true iff the image file is animated */
@Throws
fun isImageAnimated(f: File, fMime: MimeType = f.mimeType()): Boolean = when (fMime) {
   `image∕gif`  -> GifMetadataReader.readMetadata(f).getDirectoriesOfType(GifControlDirectory::class.java).size>1
   `image∕webp` -> WebpUtils.isAnimated(f)
   else -> false
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
@Blocking
fun getImageDim(f: File): Try<Dimension, Throwable> =
   runTry {
      when (f.mimeType()) {
         `application∕x-krita` -> {
            ZipFile(f).use {
               val entry = it.getEntry("maindoc.xml") ?: fail { "No maindoc.xml found" }
               it.getInputStream(entry).bufferedReader().use {
                  it.useLines {
                     val l = it.find { it.trimStart().startsWith("<IMAGE") } ?: fail { "No <IMAGE> tag found" }
                     val width = l.substringAfter("width=\"").substringBefore("\"").toInt()
                     val height = l.substringAfter("height=\"").substringBefore("\"").toInt()
                     Dimension(width, height)
                  }
               }
            }
         }
         else ->
            ImageIO.createImageInputStream(FileInputStream(f).buffered()).ifNull { fail { "No stream" } }!!.use { stream ->
               stream.reader().ifNull { fail { "No reader" } }!!.use { reader ->
                  val ii = reader.minIndex
                  val width = reader.getWidth(ii)
                  val height = reader.getHeight(ii)
                  Dimension(width, height)
               }
            }
      }
   }.ifError {
      logger.warn(it) { "Problem finding out image size" }
   }

/** @return new black image of specified size */
fun createImageBlack(size: ImageSize) = ImageBf(size.width.toInt(), size.height.toInt(), ImageBf.TYPE_INT_RGB)

/** @return new transparent image of specified size */
fun createImageTransparent(size: ImageSize) = ImageBf(size.width.toInt(), size.height.toInt(), ImageBf.TYPE_INT_ARGB)