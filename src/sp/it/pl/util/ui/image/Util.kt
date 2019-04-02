@file:Suppress("unused")

package sp.it.pl.util.ui.image

import com.twelvemonkeys.image.ResampleOp
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import mu.KotlinLogging
import sp.it.pl.util.dev.failIfFxThread
import sp.it.pl.util.file.Util.getSuffix
import sp.it.pl.util.functional.Try
import sp.it.pl.util.functional.orNull
import sp.it.pl.util.functional.runTry
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

fun ImageBf.toFX(wImg: ImageWr? = null) = SwingFXUtils.toFXImage(this, wImg)!!

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
    } catch (e: IOException) {
        logger.warn(e) { "Can't find image thumbnails $f" }
        false
    } catch (e: Exception) {
        // TODO: remove, should not longer happen
        // The TwelveMonkeys library seems to have a few bugs, throwing all kinds of exceptions,
        // including NullPointerException
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

private fun loadImagePsd(file: File, imageInputStream: ImageInputStream, width: Double, height: Double, highQuality: Boolean): ImageFx? {
    failIfFxThread()

    imageInputStream.use { input ->
        val readers = ImageIO.getImageReaders(input)
        if (!readers.hasNext()) return null

        // negative values have same effect as 0, 0 loads image at its size
        var w = maxOf(0, width.toInt())
        var h = maxOf(0, height.toInt())
        val loadFullSize = w==0 && h==0
        val scale = !loadFullSize
        val reader = readers.next()!!
        reader.input = input
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
                if (w>iW || h > iH) {
                    w = iW
                    h = iH
                }
                val iRatio = iW.toDouble()/iH
                val rRatio = w.toDouble()/h
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
        // find out real image file resolution
        val dt = getImageDim(file)
        val w = dt.map { d -> d.width }.getOr(Integer.MAX_VALUE)
        val h = dt.map { d -> d.height }.getOr(Integer.MAX_VALUE)

        // lets not surpass real size (javafx.scene.Image does that if we do not stop it)
        val widthFin = minOf(W, w)
        val heightFin = minOf(H, h)
        ImageFx(file.toURI().toString(), widthFin.toDouble(), heightFin.toDouble(), true, true, isFxThread)
    }
}

/**
 * Returns image size in pixels or error if unable to find out.
 * Does i/o, but does not read whole image into memory.
 */
fun getImageDim(f: File): Try<Dimension, Nothing?> {
    // see more at:
    // http://stackoverflow.com/questions/672916/how-to-get-image-height-and-width-using-java
    val suffix = getSuffix(f.toURI())
    val readers = ImageIO.getImageReadersBySuffix(suffix)
    if (readers.hasNext()) {
        val reader = readers.next()
        try {
            ImageIO.createImageInputStream(f).use { stream ->
                reader.input = stream
                val ii = reader.minIndex // 1st image index
                val width = reader.getWidth(ii)
                val height = reader.getHeight(ii)
                return Try.ok(Dimension(width, height))
            }
        } catch (e: IOException) {
            logger.warn(e) {"Problem finding out image size $f" }
            return Try.error()
        } catch (e: NullPointerException) {
            // The TwelveMonkeys library seems to have a bug
            logger.warn(e) { "Problem finding out image size $f" }
            return Try.error()
        } finally {
            reader.dispose()
        }
    } else {
        logger.warn { "No reader found for given file: $f" }
        return Try.error()
    }

}

/** @return new black image of specified size */
fun createImageBlack(size: ImageSize) = java.awt.image.BufferedImage(size.width.toInt(), size.height.toInt(), java.awt.image.BufferedImage.TYPE_INT_RGB)

/** @return new transparent image of specified size */
fun createImageTransparent(size: ImageSize) = java.awt.image.BufferedImage(size.width.toInt(), size.height.toInt(), java.awt.image.BufferedImage.TYPE_INT_ARGB)