package util.graphics.image

import javafx.scene.image.Image
import util.dev.log
import util.file.mimetype.MimeType
import util.file.mimetype.mimeType
import util.graphics.IconExtractor
import util.graphics.image.ImageLoader.Params
import java.io.File


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
        log().debug("Loading img $p")

        return when (p.mime.name) {
            "image/vnd.adobe.photoshop" -> loadImagePsd(p.file, p.size.width, p.size.height, true)
            "application/x-msdownload",
            "application/x-ms-shortcut" -> IconExtractor.getFileIcon(p.file)
            else -> {
                val W = maxOf(0, p.size.width.toInt())
                val H = maxOf(0, p.size.height.toInt())
                val loadFullSize = W==0 && H==0
                imgImplLoadFX(p.file, W, H, loadFullSize)
            }
        }
    }
}

/** Pair of low quality/high quality image loaders, e.g.: to speed up thumbnail loading. */
object Image2PassLoader {
    val lq: ImageLoader = object: ImageLoader {
        override fun invoke(p: Params): Image? {
            log().debug("Loading LQ img $p")

            return when (p.mime.name) {
                "image/vnd.adobe.photoshop" -> loadImagePsd(p.file, p.size.width, p.size.height, false)
                else -> ImageStandardLoader(p)
            }
        }
    }
    val hq: ImageLoader = object: ImageLoader {
        override fun invoke(p: Params): Image? {
            log().debug("Loading HQ img $p")

            return when (p.mime.name) {
                "image/vnd.adobe.photoshop" -> ImageStandardLoader(p)
                else -> null
            }
        }
    }
}