package sp.it.util.ui.image

import javafx.scene.image.Image
import mu.KotlinLogging
import sp.it.util.async.IO
import sp.it.util.file.div
import sp.it.util.file.type.MimeGroup.Companion.video
import sp.it.util.file.type.MimeType
import sp.it.util.file.type.mimeType
import sp.it.util.functional.orNull
import sp.it.util.system.runAsProgram
import sp.it.util.ui.IconExtractor
import sp.it.util.ui.image.ImageLoader.Params
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
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

      return if (p.mime.group==video) {
         val tmpDir: File = File(System.getProperty("user.home")).absoluteFile/"video-covers"
         val tmpFile = tmpDir/"video-covers"/"${p.file.nameWithoutExtension}.png"
         if (tmpFile.exists()) {
            ImageStandardLoader(p.copy(file = tmpFile, mime = tmpFile.mimeType()))
         } else {
            tmpDir.mkdirs()
            getThumb(p.file.absolutePath, tmpFile.absolutePath, 0, 0, 1f).map {
               ImageStandardLoader(p.copy(file = tmpFile, mime = tmpFile.mimeType()))
            }.orNull()
         }
      } else when (p.mime.name) {
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

// TODO handle videos shorter than specified time
// TODO: move out + handle errors
fun getThumb(videoFilename: String, thumbFilename: String, hour: Int, min: Int, sec: Float) = run {
   val ffmpeg = File("").absoluteFile/"ffmpeg"/"bin"/"ffmpeg.exe"
   val args = arrayOf("-y", "-i", "\"$videoFilename\"", "-vframes", "1", "-ss", "$hour:$min:$sec", "-f", "mjpeg", "-an", "\"$thumbFilename\"")
   ffmpeg.runAsProgram(*args).then(IO) { it.map { it.waitFor(5, TimeUnit.SECONDS) } }.getDoneOrNull() ?: error("fff")
}