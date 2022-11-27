package sp.it.pl.image

import java.io.File
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.URI
import java.nio.ByteBuffer
import java.util.UUID
import java.util.zip.ZipFile
import javafx.scene.image.Image
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.PixelFormat.Type.BYTE_BGRA_PRE
import javafx.scene.image.WritableImage
import javafx.util.Duration
import javax.imageio.ImageIO
import kotlinx.coroutines.withContext
import mu.KLogging
import org.apache.pdfbox.Loader.loadPDF
import org.apache.pdfbox.rendering.PDFRenderer
import sp.it.pl.audio.SimpleSong
import sp.it.pl.audio.tagging.read
import sp.it.pl.core.logger
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.AppProgress
import sp.it.pl.main.downloadFile
import sp.it.pl.main.ifErrorNotify
import sp.it.pl.main.isAudio
import sp.it.pl.main.reportFor
import sp.it.pl.main.runAsAppProgram
import sp.it.pl.main.withAppProgress
import sp.it.pl.ui.objects.image.Cover.CoverSource
import sp.it.util.Util.filenamizeString
import sp.it.util.async.coroutine.IO
import sp.it.util.async.coroutine.runSuspendingFx
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
import sp.it.util.dev.failIfFxThread
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.deleteOrThrow
import sp.it.util.file.deleteRecursivelyOrThrow
import sp.it.util.file.div
import sp.it.util.file.nameOrRoot
import sp.it.util.file.parentDirOrRoot
import sp.it.util.file.setExecutableOrThrow
import sp.it.util.file.traverseParents
import sp.it.util.file.type.MimeGroup.Companion.audio
import sp.it.util.file.type.MimeGroup.Companion.video
import sp.it.util.file.type.MimeType
import sp.it.util.file.type.MimeType.Companion.`application∕x-krita`
import sp.it.util.file.type.mimeType
import sp.it.util.file.unzip
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.system.Os
import sp.it.util.ui.IconExtractor
import sp.it.util.ui.image.FitFrom
import sp.it.util.ui.image.ImageSize
import sp.it.util.ui.image.Interrupts
import sp.it.util.ui.image.Params
import sp.it.util.ui.image.loadImagePsd
import sp.it.util.ui.image.toBuffered
import sp.it.util.ui.image.toFxAndFlush
import sp.it.util.ui.image.withUrl
import sp.it.util.units.millis
import sp.it.util.units.seconds

interface ImageLoader {

   operator fun invoke(file: File?, size: ImageSize, fit: FitFrom) = invoke(file, size, fit, false)

   /**
    * Loads image file with requested size (aspect ratio remains unaffected).
    *
    * @param file file to load.
    * @param size requested size. Size of <=0 requests original image size. Size > original will be scaled down.
    * @param scaleExact if true, size < original will be scaled up
    * @throws IllegalArgumentException when on fx thread
    */
   operator fun invoke(file: File?, size: ImageSize, fit: FitFrom, scaleExact: Boolean) = if (file==null) null else invoke(Params(file, size, fit, file.mimeType(), scaleExact))

   operator fun invoke(file: File?) = invoke(file, ImageSize(0.0, 0.0), FitFrom.OUTSIDE)

   operator fun invoke(p: Params): Image?

   /** @return loader that uses disk cache to improve performance */
   fun memoized(cacheKey: UUID): ImageLoader = memoize(this, cacheKey)

   companion object {

      /** @return memoized image loader that caches */
      private fun memoize(loader: ImageLoader, cacheKey: UUID): ImageLoader = object: ImageLoader {
         private val imgCacheDir: File = File(System.getProperty("user.home")).absoluteFile / cacheKey.toString()

         override fun invoke(p: Params): Image? {
            val sizeKey = "${p.size.width.toInt() max 0}x${p.size.height.toInt() max 0}x${p.fit}"
            val imgCachedDir: File = imgCacheDir / sizeKey
            val imgCachedFile = imgCachedDir / buildString {
               // To enable multiple size versions, we requested image size
               // To prevent invalid data, we embed file size
               // To prevent conflict we embed file name, file path length and file path fragments
               // There is a risk to run into file path limit on some platforms, so we shorten path fragments
               append(p.file.length()).append("-")
               append(p.file.path.length).append("-")
               append(p.file.traverseParents().drop(1).toList().reversed().asSequence().map { it.nameOrRoot.dropLastWhile { it=='\\' || it==':' } }.joinToString("-") { "${it.firstOrNull()?.toString()}${it.lastOrNull()}" }).append("-")
               append(p.file.nameWithoutExtension).append(".png")
            }
            return null
               // load from cache
               ?: when {
                  Interrupts.isInterrupted -> null
                  !imgCachedFile.exists() -> null
                  else -> runTry { ImageFxObjectStreamSerializer.deserialize(imgCachedFile) }
                     .ifError { logger.warn(it) { "Failed to deserialize cache file=${p.file} from $imgCachedFile" } }
                     .orNull()
               }
               // load normally
               ?: when {
                  Interrupts.isInterrupted -> null
                  else -> loader(p.copy(scaleExact = true))?.apply {
                     runTry { ImageFxObjectStreamSerializer.serialize(this, imgCachedDir, imgCachedFile) }
                        .ifError { logger.warn(it) { "Failed to serialize cache file=${p.file} to $imgCachedFile" } }
                  }
               }
         }
      }

   }

   /** Stateless image cache [Image] serializer that uses .png image file as cache format */
   object ImageFxImageFileSerializer {
      fun serialize(img: Image, dir: File, file: File) {
         dir.mkdirs()
         img.toBuffered().ifNotNull { ImageIO.write(it, "png", file) }
      }
      fun deserialize(loader: ImageLoader, file: File): Image? {
          return loader(Params(file, ImageSize(0, 0), FitFrom.OUTSIDE, MimeType.`image∕png`, false))
      }
   }

   /** Stateless image cache [Image] serializer that uses img binary data as cache format. Incurs no image processing cost. */
   object ImageFxObjectStreamSerializer {
      fun serialize(img: Image, dir: File, file: File) {
         if (img.pixelReader==null) return
         dir.mkdirs()
         ObjectOutputStream(file.outputStream().buffered()).use { s ->
            s.writeDouble(img.width)
            s.writeDouble(img.height)
            s.writeUTF(img.url)
            s.writeObject(BYTE_BGRA_PRE)
            ByteBuffer.allocate(img.width.toInt()*img.height.toInt()*4).apply {
               img.pixelReader.getPixels(0, 0, img.width.toInt(), img.height.toInt(), img.pixelReader.pixelFormat.asIs(), this, img.width.toInt()*4)
               s.writeInt(capacity())
               s.write(array())
            }
         }
      }
      fun deserialize(file: File): Image {
         ObjectInputStream(file.inputStream().buffered()).use { s ->
            val w = s.readDouble()
            val h = s.readDouble()
            val url = s.readUTF()
            val pixelFormatType = s.readObject().asIs<PixelFormat.Type>()
            val pixelFormat = pixelFormatType.net { failIf(it!=BYTE_BGRA_PRE) { "Unsupported format=$it" }; PixelFormat.getByteBgraPreInstance() }
            val bufferCapacity = s.readInt()
            val buffer = ByteBuffer.wrap(s.readNBytes(bufferCapacity))
            return WritableImage(PixelBuffer(w.toInt(), h.toInt(), buffer, pixelFormat.asIs())).withUrl(url)
         }
      }
   }
}



/** Standard image loader attempting the best possible quality and broad file type support. */
object ImageStandardLoader: KLogging(), ImageLoader {

   override fun invoke(p: Params): Image? {
      logger.debug { "Loading img $p" }
      failIfFxThread()

      return when (p.mime.group) {
         audio -> {
            if (p.file.isAudio()) SimpleSong(p.file).read().getCover(CoverSource.ANY).getImage(p.size, p.fit)
            else null
         }
         video -> {
            val imgDir: File = File(System.getProperty("user.home")).absoluteFile / "video-covers"
            val imgName = buildString {
               // To enable multiple size versions, we requested image size
               // To prevent invalid data, we embed file size
               // To prevent conflict we embed file name, file path length and file path fragments
               // There is a risk to run into file path limit on some platforms, so we shorten path fragments
               append(p.file.length()).append("-")
               append(p.size.width.toInt() max 0).append("x").append(p.size.height.toInt() max 0).append("-")
               append(p.file.path.length).append("-")
               append(p.file.traverseParents().drop(1).toList().reversed().joinToString("") {
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
            "image/vnd.adobe.photoshop" -> loadImagePsd(p, highQuality = true)
            "application/x-msdownload",
            "application/x-ms-shortcut" -> IconExtractor.getFileIcon(p.file)
            `application∕x-krita`.name -> {
               try {
                  ZipFile(p.file).use {
                     val entry = it.getEntry("mergedimage.png") ?: fail { "No mergedimage.png found" }
                     loadImagePsd(it.getInputStream(entry), p, highQuality = false)
                  }
               } catch (e: IOException) {
                  if (!Interrupts.isInterrupted)  logger.error(e) { "Unable to load image from ${p.file}" }
                  null
               }
            }
            "application/pdf" -> {
               runTry {
                  loadPDF(p.file).use {
                     PDFRenderer(it).renderImageWithDPI(0, p.size.height.toFloat()/8.27f).toFxAndFlush(p.file)
                  }
               } orNull {
                  if (!Interrupts.isInterrupted) logger.error(it) { "Unable to load pdf image preview for=${p.file}" }
               }
            }
            else -> loadImagePsd(p, highQuality = false)
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
   val ffmpegLink = when (os) {
      Os.WINDOWS -> URI("https://ffmpeg.zeranoe.com/builds/win64/static/$ffmpegVersion.zip")
      Os.OSX -> URI("https://ffmpeg.zeranoe.com/builds/macos64/static/$ffmpegVersion.zip")
      else -> fail { "Video cover extraction using ffmpeg is not supported on $os" }
   }
   runSuspendingFx {
      AppProgress.start("Obtaining ffmpeg").reportFor { task ->
         withContext(IO) {
            if (!ffmpegBinary.exists()) {
               if (ffmpegDir.exists()) ffmpegDir.deleteRecursivelyOrThrow()
               downloadFile(ffmpegLink, ffmpegZip, task)
               ffmpegZip.unzip(ffmpegDir) { it.substringAfter("$ffmpegVersion/") }
               ffmpegBinary.setExecutableOrThrow(true)
               ffmpegZip.deleteOrThrow()
            }

            failIf(!ffmpegBinary.exists()) { "Ffmpeg executable=$ffmpegBinary does not exist" }
            failIf(!ffmpegBinary.canExecute()) { "Ffmpeg executable=$ffmpegBinary must be executable" }
            ffmpegBinary
         }
      }
   }.withAppProgress("Obtaining ffmpeg").onDone {
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