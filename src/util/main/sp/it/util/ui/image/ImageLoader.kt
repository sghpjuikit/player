package sp.it.util.ui.image

import javafx.scene.image.Image
import mu.KotlinLogging
import sp.it.util.file.type.MimeType
import sp.it.util.file.type.mimeType
import sp.it.util.ui.IconExtractor
import sp.it.util.ui.image.ImageLoader.Params
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

private val logger = KotlinLogging.logger {}

interface ImageLoader {

    /**
     * Loads image file with requested size (aspect ratio remains unaffected).
     *
     * @param file file to load.
     * @param size requested size. Size of <=0 requests original image size. Size > original will be clipped.
     * @return loaded image or null if file null or not a valid image source.
     * @throws IllegalArgumentException when on fx thread
     */
    operator fun invoke(file: File?, size: ImageSize) = if (file==null) null else invoke(Params(file, size, file.mimeType()))

    operator fun invoke(file: File?) = invoke(file, ImageSize(0.0, 0.0))

    operator fun invoke(p: Params): Image?

    data class Params(val file: File, val size: ImageSize, val mime: MimeType)

}

/** Standard image loader attempting the best possible quality and broad file type support. */
object ImageStandardLoader: ImageLoader {

    override fun invoke(p: Params): Image? {
        logger.debug { "Loading img $p" }

        return when (p.mime.name) {
            "image/vnd.adobe.photoshop" -> loadImagePsd(p.file, p.size.width, p.size.height, true)
            "application/x-msdownload",
            "application/x-ms-shortcut" -> IconExtractor.getFileIcon(p.file)
            "application/x-kra" -> {
                try {
                    ZipFile(p.file)
                        .let { it.getInputStream(it.getEntry("mergedimage.png")) }
                        ?.let { loadImagePsd(p.file, it, p.size.width, p.size.height, false) }
                } catch (e: IOException) {
                    logger.error(e) { "Unable to load image from ${p.file}" }
                    null
                }
            }
            "image/gif" -> {
                val W = maxOf(0, p.size.width.toInt())
                val H = maxOf(0, p.size.height.toInt())
                val loadFullSize = W==0 && H==0
                imgImplLoadFX(p.file, W, H, loadFullSize)
            }
            else -> loadImagePsd(p.file, p.size.width, p.size.height, false)
        }
    }
}

/** Pair of low quality/high quality image loaders, e.g.: to speed up thumbnail loading. */
object Image2PassLoader {
    val lq: ImageLoader = object: ImageLoader {
        override fun invoke(p: Params): Image? {
            logger.debug { "Loading LQ img $p" }

            return when (p.mime.name) {
                "image/vnd.adobe.photoshop" -> loadImagePsd(p.file, p.size.width, p.size.height, false)
                else -> ImageStandardLoader(p)
            }
        }
    }
    val hq: ImageLoader = object: ImageLoader {
        override fun invoke(p: Params): Image? {
            logger.debug { "Loading HQ img $p" }

            return when (p.mime.name) {
                "image/vnd.adobe.photoshop" -> ImageStandardLoader(p)
                else -> null
            }
        }
    }

}