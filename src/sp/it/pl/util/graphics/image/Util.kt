@file:Suppress("unused")

package sp.it.pl.util.graphics.image

import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import mu.KotlinLogging
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.resizers.configurations.Rendering
import net.coobird.thumbnailator.resizers.configurations.ScalingMode
import sp.it.pl.util.file.Util.getSuffix
import sp.it.pl.util.functional.Try
import sp.it.pl.util.functional.supplyFirst
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.imageio.ImageReadParam
import javax.imageio.ImageReader

private typealias ImageFx = Image
private typealias ImageBf = BufferedImage
private typealias ImageWr = WritableImage

private val logger = KotlinLogging.logger {}

fun ImageBf.toFX(wImg: ImageWr? = null) = SwingFXUtils.toFXImage(this, wImg)!!

/** Scales image to requested size, returning new image instance and flushing the old. Size must not be 0.  */
private fun ImageBf.toScaled(W: Int, H: Int, highQuality: Boolean): ImageBf {
    return if (width==W || height==H) {
        this
    } else {
        try {
            Thumbnails.of(this)
                    .scalingMode(if (highQuality) ScalingMode.PROGRESSIVE_BILINEAR else ScalingMode.BILINEAR)
                    .size(W, H).keepAspectRatio(true)
                    .rendering(if (highQuality) Rendering.QUALITY else Rendering.SPEED)
                    .asBufferedImage()
        } catch (e: IOException) {
            logger.warn(e) { "Can't find image thumbnails $this" }
            this
        } finally {
            flush()
        }
    }
}

/** Returns true if the image has at least 1 embedded thumbnail of any size.  */
private fun imgImplHasThumbnail(reader: ImageReader, index: Int, f: File): Boolean {
    return try {
        reader.readerSupportsThumbnails() && reader.hasThumbnails(index) // throws exception -> no thumb
        true
    } catch (e: IOException) {
        logger.warn(e) { "Can't find image thumbnails $f" }
        false
    } catch (e: Exception) {
        // The twelvemonkeys library seems to have a few bugs, throwing all kinds of exceptions,
        // including NullPointerException and ConcurrentModificationError
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
    val W = maxOf(0, width.toInt())
    val H = maxOf(0, height.toInt())
    val loadFullSize = W==0 && H==0

    // psd special case
    return if (!file.path.endsWith("psd")) {
        imgImplLoadFX(file, W, H, loadFullSize)
    } else {
        loadImagePsd(file, width, height, false)
    }
}

fun loadImagePsd(file: File, width: Double, height: Double, highQuality: Boolean): ImageFx? {
    if (Platform.isFxApplicationThread())
        logger.warn(Throwable()) { "Loading image on FX thread!" }

    // negative values have same effect as 0, 0 loads image at its size
    val W = maxOf(0, width.toInt())
    val H = maxOf(0, height.toInt())
    val loadFullSize = W==0 && H==0

    ImageIO.createImageInputStream(file).use { input ->
        val readers = ImageIO.getImageReaders(input)
        if (!readers.hasNext()) return null

        val reader = readers.next()!!
        reader.input = input
        val ii = reader.minIndex // 1st image index
        var i: ImageBf = supplyFirst(
                {
                    if (!loadFullSize) {
                        val thumbHas = false // imgImplHasThumbnail(reader, ii, file) // TODO work around TwelveMonkeys PsdReader thumbnail bugs
                        val thumbW = if (!thumbHas) 0 else reader.getThumbnailWidth(ii, 0)
                        val thumbH = if (!thumbHas) 0 else reader.getThumbnailHeight(ii, 0)
                        val thumbUse = thumbHas && width<=thumbW && height<=thumbH
                        if (thumbUse) {
                            reader.readThumbnail(ii, 0)
                        }
                    }
                    null
                },
                {
                    var px = 1
                    if (!highQuality) {
                        val sw = reader.getWidth(ii)/W
                        val sh = reader.getHeight(ii)/H
                        px = maxOf(1, maxOf(sw, sh)*2/3) // quality == 2/3 == ok, great performance
                    }
                    // max quality is px==1, but quality/performance ratio would suck
                    // the only quality issue is with halftone patterns (e.g. manga),
                    // they really ask for max quality
                    val irp = ImageReadParam()
                    irp.setSourceSubsampling(px, px, 0, 0)
                    reader.read(ii, irp)
                }
        )!!
        reader.dispose()

        // scale, also improves quality, fairly quick
        if (!loadFullSize)
            i = i.toScaled(W, H, highQuality)

        return i.toFX()
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
        Image(file.toURI().toString(), isFxThread)
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
fun getImageDim(f: File): Try<Dimension, Void> {
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
            // The twelvemonkeys library seems to have a bug
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
fun createImageBlack(size: ImageSize) = BufferedImage(size.width.toInt(), size.height.toInt(), BufferedImage.TYPE_INT_RGB)

/** @return new transparent image of specified size */
fun createImageTransparent(size: ImageSize) = BufferedImage(size.width.toInt(), size.height.toInt(), BufferedImage.TYPE_INT_ARGB)
