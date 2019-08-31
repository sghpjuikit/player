package sp.it.pl.main

import javafx.scene.image.Image
import javafx.stage.FileChooser
import mu.KotlinLogging
import sp.it.pl.audio.tagging.AudioFileFormat
import sp.it.util.access.VarEnum
import sp.it.util.file.Util
import sp.it.util.file.Util.getFilesR
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.file.parentDirOrRoot
import sp.it.util.file.type.MimeGroup.Companion.video
import sp.it.util.file.type.MimeTypes
import sp.it.util.file.type.mimeType
import sp.it.util.functional.Functors
import sp.it.util.functional.Try
import sp.it.util.system.Os
import sp.it.util.ui.image.toBuffered
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.streams.asSequence
import kotlin.text.Charsets.UTF_16LE

private val logger = KotlinLogging.logger { }

fun File.isVideo() = mimeType().group==video

/** Lowercase audio file extensions supported by this application. */
val audioExtensions = setOf(
   "mp3",
   "ogg",
   "flac",
   "wav",
   "m4a",
   "spx",
   "snd",
   "aifc",
   "aif",
   "au",
   "mp1",
   "mp2",
   "aac"
)

/** See [audioExtensionsJaudiotagger]. */
fun File.isAudio() = extension.toLowerCase() in audioExtensions

/** See [audioExtensionsJaudiotagger]. */
fun String.isAudio() = substringAfterLast(".").toLowerCase() in audioExtensions

/** [FileChooser.ExtensionFilter] for [audioExtensions]. */
fun audioExtensionFilter() = FileChooser.ExtensionFilter("Audio files", audioExtensions.map { "*.$it" })

/** Lowercase audio file extensions supported by jaudiotagger library for reading/writing song tags. */
val audioExtensionsJaudiotagger = setOf(
   "mp4",
   "m4a",
   "mp3",
   "ogg",
   "wav",
   "flac"
)

/** See [audioExtensionsJaudiotagger]. */
fun File.isAudioEditable() = extension.toLowerCase() in audioExtensionsJaudiotagger

/** See [audioExtensionsJaudiotagger]. */
fun AudioFileFormat.isAudioEditable() = name.toLowerCase() in audioExtensionsJaudiotagger

fun findAudio(files: Collection<File>, depth: Int = Int.MAX_VALUE) = files.asSequence().flatMap { f -> getFilesR(f, depth) { it.isAudio() }.asSequence() }

// Extracted from jaudiotagger org.jaudiotagger.tag.id3.valuepair.ImageFormats
// image/jpeg
// image/png
// image/gif
// image/bmp
// image/tiff
// image/pdf
// image/x-pict
/** Lowercase image file extensions supported by jaudiotagger library for cover in song tags. */
val imageExtensionsJaudiotagger = setOf(
   "png",
   "jpg", "jpe", "jpeg", "jp2", "j2k", "jpf", "jpx", "jpm", "mj2",
   "gif",
   "bmp",
   "pdf",
   "tiff",
   "pic", "pct"
)

/** See [imageExtensionsJaudiotagger]. */
fun File.isImageJaudiotagger() = extension.toLowerCase() in imageExtensionsJaudiotagger

/** Lowercase image file extensions supported by 23monkey library. */
val imageExtensions12Monkey: Set<String> = linkedSetOf(
   // jdk image io
   "png",
   "gif",
   // 12monkey
   "bmp",
   "jpg", "jpe", "jpeg", "jp2", "j2k", "jpf", "jpx", "jpm", "mj2",
   "psd",
   "pnm", "pbm", "pgm", "ppm",
   "hdr",
   "iff",
   "tiff",
   "pcx",
   "pic", "pct",
   "sgi",
   "tga",
   "icns",
   "ico", "cur",
   "Thumbs.db",
   "svg",
   "wmf"
)

/** See [imageExtensions12Monkey]. */
fun File.isImage12Monkey() = extension.toLowerCase() in imageExtensions12Monkey

/** Lowercase image (read) file extensions supported by this application. */
val imageExtensionsRead = imageExtensions12Monkey + setOf("kra")

/** See [imageExtensionsRead]. */
fun File.isImage() = extension.toLowerCase() in imageExtensionsRead

/** See [imageExtensionsRead]. */
fun String.isImage() = substringAfterLast(".").toLowerCase() in imageExtensionsRead

/** Lowercase image (write) file extensions supported by this application. */
val imageExtensionsWrite = setOf(
   "jpg",
   "bmp",
   "png"
)

/** See [imageExtensionsWrite]. */
fun File.isImageWrite() = extension.toLowerCase() in imageExtensionsWrite

/** [FileChooser.ExtensionFilter] for [imageExtensionsWrite]. */
fun imageWriteExtensionFilter() = FileChooser.ExtensionFilter("Image files", imageExtensionsWrite.map { "*.$it" })

/** Saves specified image to a specified file. */
fun writeImage(img: Image, file: File): Try<Nothing?, Exception> {
   return if (!file.isImageWrite()) {
      logger.error { "Could not save image to file=$file. Format=${file.extension} not supported." }
      Try.error(UnsupportedOperationException("Format=${file.extension} not supported"))
   } else try {
      val i = img.toBuffered()
      if (i==null)
         Try.error(UnsupportedOperationException("Format=${file.extension} not supported"))
      else {
         ImageIO.write(i, file.extension, file)
         Try.ok()
      }
   } catch (e: IOException) {
      logger.error(e) { "Could not save image to file=$file" }
      Try.error(e)
   }
}

/**
 * Pool of file filters intended for simple enum-like file filter selection in UI.
 *
 * Because we can not yet serialize functions (see [sp.it.util.functional.Functors] and
 * [sp.it.util.parsing.Converter]), it is useful to define predicates not from function pool,
 * but hardcoded filters, which are enumerable and we look up by name.
 */
object FileFilters {
   val filterPrimary = Functors.PF0("File - all", File::class.java, Boolean::class.java) { true }
   private val filters = ArrayList<Functors.PF0<File, Boolean>>()

   init {
      filters += filterPrimary
      filters += Functors.PF0("File - is audio", File::class.java, Boolean::class.java) { it.isAudio() }
      filters += Functors.PF0("File - is image", File::class.java, Boolean::class.java) { it.isImage() }
      filters += Functors.PF0("File type - file", File::class.java, Boolean::class.java) { it.isFile }
      filters += Functors.PF0("File type - directory", File::class.java, Boolean::class.java) { it.isDirectory }
      MimeTypes.setOfGroups().forEach { group ->
         filters += Functors.PF0("Mime type group - is ${group.capitalize()}", File::class.java, Boolean::class.java) { group==it.mimeType().group }
      }
      MimeTypes.setOfMimeTypes().forEach { mime ->
         filters += Functors.PF0("Mime type - is ${mime.name}", File::class.java, Boolean::class.java) { it.mimeType()==mime }
      }
      MimeTypes.setOfExtensions().forEach { extension ->
         filters += Functors.PF0("Type - is $extension", File::class.java, Boolean::class.java) { Util.getSuffix(it).equals(extension, ignoreCase = true) }
      }
   }

   /** @return filter with specified name or primary filter if no such filter */
   @JvmStatic
   fun getOrPrimary(name: String) = filters.find { it.name==name } ?: filterPrimary

   /** @return enumerable string value enumerating all available predicate names */
   @JvmStatic
   @JvmOverloads
   fun toEnumerableValue(initialValue: String = FileFilters.filterPrimary.name) = FileFilterValue(initialValue, filters.map { it.name })
}

class FileFilterValue(initialValue: String = FileFilters.filterPrimary.name, enumerated: Collection<String>): VarEnum<String>(initialValue, enumerated) {

   private var filter = FileFilters.getOrPrimary(initialValue)

   override fun setValue(v: String) {
      filter = FileFilters.getOrPrimary(v)
      super.setValue(v)
   }

   /** @return the filter represented by the current value, which is the name of the returned filter */
   fun getValueAsFilter() = filter
}

enum class FileFlatter(@JvmField val flatten: (Collection<File>) -> Sequence<File>) {
   NONE({ it.asSequence().distinct() }),
   DIRS({
      it.asSequence().distinct()
         .flatMap { sequenceOf(it).filter { it.isFile } + it.walk().filter { it.isDirectory } }
   }),
   TOP_LVL({ it.asSequence().distinct().flatMap { it.children() } }),
   TOP_LVL_AND_DIRS({
      it.asSequence().distinct()
         .flatMap { it.children() }
         .flatMap { (sequenceOf(it).filter { it.isFile } + it.walk().filter { it.isDirectory }) }
   }),
   TOP_LVL_AND_DIRS_AND_WITH_COVER({

      fun File.hasCover(cache: HashSet<FastFile>): Boolean {
         val p = parentDirOrRoot
         val n = nameWithoutExtension
         return imageExtensionsRead.any { cache.contains(p/"$n.$it") }
      }

      fun File.walkDirsAndWithCover(): Sequence<File> {
         return if (isDirectory) {
            val dir = this
            val cmdDirs = """cmd /U /c dir /s /b /ad "${dir.absolutePath}" 2>nul"""
            val cmdFiles = """cmd /U /c dir /s /b /a-d "${dir.absolutePath}" 2>nul"""

            val dirs = try {
               Runtime.getRuntime().exec(cmdDirs)
                  .inputStream.bufferedReader(UTF_16LE)
                  .useLines { it.map { FastFile(it, true, false) }.toList() }
            } catch (e: Throwable) {
               logger.error(e) { "Failed to read files in $this using command $cmdDirs" }
               listOf<FastFile>()
            }
            val files = try {
               Runtime.getRuntime().exec(cmdFiles)
                  .inputStream.bufferedReader(UTF_16LE)
                  .useLines { it.map { FastFile(it, false, true) }.toList() }
            } catch (e: Throwable) {
               logger.error(e) { "Failed to read files in $this using command $cmdFiles" }
               listOf<FastFile>()
            }

            val cache = (dirs + files).toHashSet()
            cache.asSequence().filter { it.isDirectory || it.hasCover(cache) }
         } else {
            sequenceOf(this)
         }
      }

      it.asSequence().distinct().flatMap { it.walkDirsAndWithCover() }
   }),
   ALL({ it.asSequence().distinct().flatMap { it.asFileTree() } });
}

class FastFile(path: String, private val isDir: Boolean, private val isFil: Boolean): File(path) {
   override fun isDirectory(): Boolean = isDir
   override fun isFile(): Boolean = isFil
}

private fun File.asFileTree(): Sequence<File> =
   when (Os.current) {
      Os.WINDOWS -> {
         if (isDirectory) {
            val dir = this
            val cmdFiles = """cmd /U /c dir /s /b /a-d "${dir.absolutePath}" 2>nul"""
            try {
               val files = Runtime.getRuntime().exec(cmdFiles)
                  .inputStream.bufferedReader(UTF_16LE)
                  .useLines { it.map { FastFile(it, false, true) }.toList() }
               files.asSequence()
            } catch (e: Throwable) {
               logger.error(e) { "Failed to read files in $this using command $cmdFiles" }
               sequenceOf<File>()
            }
         } else {
            sequenceOf(this)
         }
      }
      else -> {
         walk().filter(File::isFile)
      }
   }