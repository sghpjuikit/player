package sp.it.pl.image

import javafx.scene.image.Image
import mu.KotlinLogging
import sp.it.pl.image.ImageLoader.Params
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.ifErrorNotify
import sp.it.pl.main.onErrorNotify
import sp.it.pl.main.withAppProgress
import sp.it.util.async.FX
import sp.it.util.async.IO
import sp.it.util.async.runIO
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.Util.saveFileAs
import sp.it.util.file.div
import sp.it.util.file.type.MimeGroup.Companion.video
import sp.it.util.file.type.MimeType
import sp.it.util.file.type.mimeType
import sp.it.util.file.unzip
import sp.it.util.functional.orNull
import sp.it.util.system.Os
import sp.it.util.system.runAsProgram
import sp.it.util.ui.IconExtractor
import sp.it.util.ui.image.ImageSize
import sp.it.util.ui.image.imgImplLoadFX
import sp.it.util.ui.image.loadImagePsd
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

private val logger = KotlinLogging.logger {}

interface ImageLoader {

   operator fun invoke(file: File?, size: ImageSize) = invoke(file, size, false)

   /**
    * Loads image file with requested size (aspect ratio remains unaffected).
    *
    * @param file file to load.
    * @param size requested size. Size of <=0 requests original image size. Size > original will be scaled down.
    * @param scaleExact if true, size < original will be scaled up
    * @throws IllegalArgumentException when on fx thread
    */
   operator fun invoke(file: File?, size: ImageSize, scaleExact: Boolean) = if (file==null) null else invoke(Params(file, size, file.mimeType(), scaleExact))

   operator fun invoke(file: File?) = invoke(file, ImageSize(0.0, 0.0))

   operator fun invoke(p: Params): Image?

   data class Params(val file: File, val size: ImageSize, val mime: MimeType, val scaleExact: Boolean = false)

}

/** Standard image loader attempting the best possible quality and broad file type support. */
object ImageStandardLoader: ImageLoader {

   override fun invoke(p: Params): Image? {
      logger.debug { "Loading img $p" }

      return when (p.mime.group) {
         video -> {
            val tmpDir: File = File(System.getProperty("user.home")).absoluteFile/"video-covers"
            val tmpFile = tmpDir/"${p.file.nameWithoutExtension}.jpg"
            if (tmpFile.exists()) {
               this(p.copy(file = tmpFile, mime = tmpFile.mimeType()))
            } else {
               tmpDir.mkdirs()
               getThumb(p.file.absolutePath, tmpFile.absolutePath, 0, 0, 1f)
               if (tmpFile.exists()) this(p.copy(file = tmpFile, mime = tmpFile.mimeType()))
               else null
            }
         }
         else -> when (p.mime.name) {
            "image/vnd.adobe.photoshop" -> loadImagePsd(p.file, p.size.width, p.size.height, highQuality = true, scaleExact = p.scaleExact)
            "application/x-msdownload",
            "application/x-ms-shortcut" -> IconExtractor.getFileIcon(p.file)
            "application/x-kra" -> {
               try {
                  ZipFile(p.file)
                     .let { it.getInputStream(it.getEntry("mergedimage.png")) }
                     ?.let { loadImagePsd(p.file, it, p.size.width, p.size.height, highQuality = false, scaleExact = p.scaleExact) }
               } catch (e: IOException) {
                  logger.error(e) { "Unable to load image from ${p.file}" }
                  null
               }
            }
            "image/jpeg", "image/png", "image/gif" -> imgImplLoadFX(p.file, p.size, p.scaleExact)
            else -> loadImagePsd(p.file, p.size.width, p.size.height, highQuality = false, scaleExact = p.scaleExact)
         }
      }
   }
}

/** Pair of low quality/high quality image loaders, e.g.: to speed up thumbnail loading. */
object Image2PassLoader {
   val lq: ImageLoader = object: ImageLoader {
      override fun invoke(p: Params): Image? {
         logger.debug { "Loading LQ img $p" }

         return when (p.mime.name) {
            "image/vnd.adobe.photoshop" -> loadImagePsd(p.file, p.size.width, p.size.height, highQuality = false, scaleExact = p.scaleExact)
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
private fun getThumb(videoFilename: String, thumbFilename: String, hour: Int, min: Int, sec: Float) = run {
   val ffmpeg = ffmpeg.getDone().toTry().orNull() ?: fail { "ffmpeg not available" }
   val args = arrayOf("-y", "-i", "\"$videoFilename\"", "-vframes", "1", "-ss", "$hour:$min:$sec", "-f", "mjpeg", "-an", "\"$thumbFilename\"")
   ffmpeg.runAsProgram(*args).then(IO) { it.waitFor(5, TimeUnit.SECONDS) }
      .onErrorNotify { AppError("Failed to extract video cover of $videoFilename", "Reason: ${it.stacktraceAsString}") }
      .getDone()
}

private val ffmpeg by lazy {
   val os = Os.current
   val ffmpegDir = APP.location/"ffmpeg"
   val ffmpegZip = ffmpegDir/"ffmpeg.zip"
   val ffmpegBinary = when (os) {
      Os.WINDOWS -> ffmpegDir/"bin"/"ffmpeg.exe"
      Os.OSX -> ffmpegDir/"bin"/"ffmpeg"
      else -> fail { "Video cover extraction using ffmpeg is not supported on $os" }
   }
   val ffmpegVersion = "ffmpeg-20190826-0821bc4-win64-static"
   val ffmpegLink = URI(
      when (os) {
         Os.WINDOWS -> "https://ffmpeg.zeranoe.com/builds/win64/static/$ffmpegVersion.zip"
         Os.OSX -> "https://ffmpeg.zeranoe.com/builds/macos64/static/$ffmpegVersion.zip"
         else -> fail { "Video cover extraction using ffmpeg is not supported on $os" }
      }
   )
   runIO {
      fun Boolean.orFailIO(message: () -> String) = also { if (!this) throw IOException(message()) }

      if (!ffmpegBinary.exists()) {
         if (ffmpegDir.exists()) ffmpegDir.deleteRecursively().orFailIO { "Failed to remove ffmpeg in=$ffmpegDir" }
         saveFileAs(ffmpegLink.toString(), ffmpegZip)
         ffmpegZip.unzip(ffmpegDir) { it.substringAfter("$ffmpegVersion/") }
         ffmpegBinary.setExecutable(true).orFailIO { "Failed to make file=$ffmpegBinary executable" }
         ffmpegZip.delete().orFailIO { "Failed to clean up downloaded file=$ffmpegZip" }
      }

      failIf(!ffmpegBinary.exists()) { "Ffmpeg executable=$ffmpegBinary does not exist" }
      failIf(!ffmpegBinary.canExecute()) { "Ffmpeg executable=$ffmpegBinary must be executable" }
      ffmpegBinary
   }.withAppProgress("Obtaining ffmpeg").onDone(FX) {
      it.toTry().ifErrorNotify {
         AppError(
            "Failed to obtain ffmpeg",
            """
               |ffmpeg version: $ffmpegVersion
               |ffmpeg link: $ffmpegLink
               |
               | ${it.stacktraceAsString}
            """.trimMargin()
         )
      }
   }
}