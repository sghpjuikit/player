package sp.it.pl.main

import javafx.scene.image.Image
import javafx.stage.FileChooser
import mu.KotlinLogging
import sp.it.pl.audio.tagging.AudioFileFormat
import sp.it.util.dev.failIf
import sp.it.util.file.FastFile
import sp.it.util.file.FileType
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.FileType.FILE
import sp.it.util.file.Util.getFilesR
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.file.parentDirOrRoot
import sp.it.util.file.type.MimeGroup.Companion.video
import sp.it.util.file.type.mimeType
import sp.it.util.functional.PF0
import sp.it.util.functional.Try
import sp.it.util.functional.runTry
import sp.it.util.system.Os
import sp.it.util.text.plural
import sp.it.util.type.type
import sp.it.util.ui.image.toBuffered
import java.io.File
import javax.imageio.ImageIO
import kotlin.streams.asSequence
import sp.it.pl.core.Parse
import sp.it.pl.core.Parser
import sp.it.util.access.V
import sp.it.util.conf.but
import sp.it.util.conf.cv
import sp.it.util.functional.asIs
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.net

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
   "wmf",
   "webp",
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
fun writeImage(img: Image, file: File): Try<Unit, Throwable> = runTry {
   failIf(!file.isImageWrite()) { "Format=${file.extension} not supported" }

   val i = img.toBuffered()
   failIf(i==null) { "Format=${file.extension} not supported" }

   val format = file.extension.toLowerCase()
   val noWriter = !ImageIO.write(i, format, file)
   failIf(noWriter) { "No image writer for format: $format" }
}.ifError {
   logger.error(it) { "Could not save image to file=$file" }
}

/**
 * Pool of file filters intended for simple enum-like file filter selection in UI.
 *
 * Because we can not yet serialize functions (see [PF0] and [sp.it.util.parsing.Converter]), it is useful to define
 * predicates not from function pool, but hardcoded filters, which are enumerable and we look up by name.
 */
object FileFilters {

   /** Default file filter. Returns true for every file. */
   val filterPrimary = FileFilter("File - all", type(), type()) { true }

   /** Parser converting [String] to [FileFilter]. */
   val parser: Parse<FileFilter> = Parse.or(
      Parser(type(), listOf("File - all")) { filterPrimary },
      Parser(type(), listOf("File - is audio")) { FileFilter("File - is audio", type(), type()) { it.isAudio() } },
      Parser(type(), listOf("File - is image")) { FileFilter("File - is image", type(), type()) { it.isImage() } },
      Parser(type(), listOf("File - is video")) { FileFilter("File - is video", type(), type()) { it.isVideo() } },
      Parser(type(), listOf("File type - file")) { FileFilter("File type - file", type(), type()) { it.isFile } },
      Parser(type(), listOf("File type - directory")) { FileFilter("File type - directory", type(), type()) { it.isDirectory } },
      Parser(type(), args = listOf("Mime type group - is", String::class)) { it ->
         val group = it[0].asIs<String>()
         FileFilter("Mime type group - is ${group.capitalize()}", type(), type()) { group==it.mimeType().group }
      },
      Parser(type(), listOf("Mime type - is", String::class)) { it ->
         val mimeTypeName = it[0].asIs<String>()
         FileFilter("Mime type - is ${mimeTypeName.capitalize()}", type(), type()) { mimeTypeName==it.mimeType().name }
      },
      Parser(type(), listOf("Type - is", String::class)) { it ->
         val extension = it[0].asIs<String>()
         FileFilter("Type - is $extension", type(), type()) { it.extension.equals(extension, ignoreCase = true) }
      },
   )

   /** @return filter with specified name or primary filter if no such filter */
   fun getOrPrimary(name: String): PF0<File, Boolean> = parser.parse(name).getOrSupply { filterPrimary }

   /** @return [javafx.beans.value.ObservableValue] for [FileFilter] */
   fun vFileFilter(initialValue: String = filterPrimary.name) = FileFilterValue(initialValue)

   /** @return delegated configurable [javafx.beans.value.ObservableValue] for [FileFilter] */
   fun cvFileFilter() = cv(filterPrimary.name) { vFileFilter(it) }.but(parser.toConstraint())

   class FileFilterValue(initialValue: String = filterPrimary.name): V<String>(initialValue) {
      /** @return the filter represented by the current value, which is the name of the returned filter */
      val valueAsFilter: PF0<File, Boolean> = getOrPrimary(value)
   }
}

enum class FileFlatter(val flatten: (Collection<File>) -> Sequence<File>) {
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
            val dirs = windowsCmdDir(this, DIRECTORY)
            val files = windowsCmdDir(this, FILE)
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

private fun File.asFileTree(): Sequence<File> =
   when (Os.current) {
      Os.WINDOWS -> {
         if (isDirectory) {
            windowsCmdDir(this, FILE).asSequence()
         } else {
            sequenceOf(this)
         }
      }
      else -> {
         walk().filter(File::isFile)
      }
   }

private fun windowsCmdDir(dir: File, type: FileType): List<FastFile> {
   val isFile = type==FILE
   val isDir = type==DIRECTORY
   val cmd = when (type) {
      DIRECTORY -> """cmd.exe /c chcp 65001 > nul & cmd /c dir /s /b /on /ad "${dir.absolutePath}" 2>nul"""
      FILE -> """cmd.exe /c chcp 65001 > nul & cmd /c dir /s /b /on /a-d "${dir.absolutePath}" 2>nul"""
   }

   return try {
      Runtime.getRuntime().exec(cmd)
         .inputStream.bufferedReader(Charsets.UTF_8)
         .useLines { it.map { FastFile(it, isDir, isFile) }.toList() }
   } catch (e: Throwable) {
      logger.error(e) { "Failed to read ${type.name.plural()} in $dir using command $cmd" }
      listOf()
   }

}
