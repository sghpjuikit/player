package sp.it.pl.image

import java.io.File
import java.io.IOException
import java.net.URI
import java.util.zip.ZipFile
import javafx.scene.image.Image
import javafx.util.Duration
import mu.KLogging
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import sp.it.pl.audio.SimpleSong
import sp.it.pl.audio.tagging.read
import sp.it.pl.image.ImageLoader.Params
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.ifErrorNotify
import sp.it.pl.main.isAudio
import sp.it.pl.main.runAsAppProgram
import sp.it.pl.main.withAppProgress
import sp.it.pl.ui.objects.image.Cover.CoverSource
import sp.it.util.Util.filenamizeString
import sp.it.util.async.FX
import sp.it.util.async.runIO
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
import sp.it.util.dev.failIfFxThread
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.Util.saveFileAs
import sp.it.util.file.div
import sp.it.util.file.nameOrRoot
import sp.it.util.file.parentDirOrRoot
import sp.it.util.file.type.MimeGroup.Companion.audio
import sp.it.util.file.type.MimeGroup.Companion.video
import sp.it.util.file.type.MimeType
import sp.it.util.file.type.MimeType.Companion.`image∕vnd·adobe·photoshop`
import sp.it.util.file.type.mimeType
import sp.it.util.file.unzip
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.functional.traverse
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.system.Os
import sp.it.util.ui.IconExtractor
import sp.it.util.ui.image.ImageSize
import sp.it.util.ui.image.imgImplLoadFX
import sp.it.util.ui.image.loadImagePsd
import sp.it.util.ui.image.toFX
import sp.it.util.units.millis
import sp.it.util.units.seconds

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
object ImageStandardLoader: KLogging(), ImageLoader {

   override fun invoke(p: Params): Image? {
      logger.debug { "Loading img $p" }
      failIfFxThread()

      return when (p.mime.group) {
         audio -> {
            if (p.file.isAudio()) SimpleSong(p.file).read().getCover(CoverSource.ANY).getImage()
            else null
         }
         video -> {
            val imgDir: File = File(System.getProperty("user.home")).absoluteFile/"video-covers"
            val imgName = buildString {
               // To enable multiple size versions, we requested image size
               // To prevent invalid data, we embed file size
               // To prevent conflict we embed file name, file path length and file path fragments
               // There is a risk to run into file path limit on some platforms, so we shorten path fragments
               append(p.file.length()).append("-")
               append(p.size.width.toInt() max 0).append("x").append(p.size.height.toInt() max 0).append("-")
               append(p.file.path.length).append("-")
               append(p.file.traverse { it.parentFile }.drop(1).toList().reversed().joinToString("") {
                  "${it.nameOrRoot.firstOrNull()?.toString()}${it.nameOrRoot.lastOrNull()}-" }
               )
               append(p.file.name)
            }
            val imgFile = imgDir/"${filenamizeString(imgName)}.jpg"
            if (imgFile.exists()) {
               this(p.copy(file = imgFile, mime = imgFile.mimeType()))
            } else {
               imgDir.mkdirs()
               extractThumb(p.file.absolutePath, imgFile.absolutePath, 1.seconds)
               if (imgFile.exists()) this(p.copy(file = imgFile, mime = imgFile.mimeType()))
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
            "application/pdf" -> {
               runTry {
                  PDFRenderer(PDDocument.load(p.file)).renderImageWithDPI(0, p.size.height.toFloat()/8.27f).toFX()
               } orNull {
                  logger.error(it) { "Unable to load pdf image preview for=${p.file}" }
               }
            }
            else -> loadImagePsd(p.file, p.size.width, p.size.height, highQuality = false, scaleExact = p.scaleExact)
         }
      }
   }
}

/** Pair of low quality/high quality image loaders, e.g.: to speed up thumbnail loading. */
object Image2PassLoader: KLogging() {
   val lq: ImageLoader = object: ImageLoader {
      override fun invoke(p: Params): Image? {
         logger.debug { "Loading LQ img $p" }

         return when (p.mime) {
            `image∕vnd·adobe·photoshop` -> loadImagePsd(p.file, p.size.width, p.size.height, highQuality = false, scaleExact = p.scaleExact)
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

private fun extractThumb(videoFilename: String, thumbFilename: String, at: Duration) = run {
   val ffmpeg = ffmpeg.getDone().toTry().orNull() ?: fail { "ffmpeg not available" }
   val ffprobe = ffprobe.getDone().toTry().orNull() ?: fail { "ffprobe not available" }

   val durArgs = arrayOf("-v", "quiet", "-print_format", "compact=print_section=0:nokey=1:escape=csv", "-show_entries", "format=duration", videoFilename)
   val durMax = ffprobe.runAsAppProgram("Obtaining video length", *durArgs).getDone().toTry().orThrow.toDouble().seconds
      .takeIf { it.toMillis()>0 } ?: Double.MAX_VALUE.millis
   val atClipped = at min durMax
   val args = arrayOf(
      "-y", "-i", videoFilename, "-vframes", "1", "-ss",
      "${atClipped.toHours().toInt()}:${atClipped.toMinutes().toInt()}:${atClipped.toSeconds().toInt()}",
      "-f", "mjpeg", "-an", thumbFilename
   )
   ffmpeg.runAsAppProgram("Extracting video cover of $videoFilename", *args).getDone()
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
private val ffprobe by lazy {
   ffmpeg.then { it.parentDirOrRoot/"ffprobe" }
}