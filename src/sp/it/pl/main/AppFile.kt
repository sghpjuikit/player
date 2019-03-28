package sp.it.pl.main

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.stage.FileChooser
import mu.KotlinLogging
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.layout.widget.WidgetUse
import sp.it.pl.layout.widget.feature.ImageDisplayFeature
import sp.it.pl.layout.widget.feature.ImagesDisplayFeature
import sp.it.pl.util.file.AudioFileFormat
import sp.it.pl.util.file.childOf
import sp.it.pl.util.file.listChildren
import sp.it.pl.util.file.parentDirOrRoot
import sp.it.pl.util.system.Os
import sp.it.pl.util.system.open
import java.io.File
import java.io.IOException
import java.util.stream.Stream
import javax.imageio.ImageIO
import kotlin.streams.asStream

private val logger = KotlinLogging.logger { }

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
val imageExtensions12Monkey = setOf(
        // 12monkey
        "bmp",
        "jpg", "jpe", "jpeg", "jp2", "j2k", "jpf", "jpx", "jpm", "mj2",
        "pnm", "pbm", "pgm", "ppm",
        "psd",
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
        // jdk image io
        "png",
        "gif"
)

/** See [imageExtensions12Monkey]. */
fun File.isImage12Monkey() = extension.toLowerCase() in imageExtensions12Monkey

/** Lowercase image (read) file extensions supported by this application. */
val imageExtensionsRead = imageExtensions12Monkey + setOf("kra")

/** See [imageExtensionsRead]. */
fun File.isImage() = extension.toLowerCase() in imageExtensionsRead

/** See [imageExtensionsRead]. */
fun String.isImage() = toLowerCase() in imageExtensionsRead

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

// TODO: document failure, return Try
/** Saves specified image to a specified file. */
fun writeImage(img: Image, file: File) {
    if (!file.isImageWrite()) {
        logger.error { "Could not save image to file=$file. Format=${file.extension} not supported." }
        return
    }

    try {
        ImageIO.write(SwingFXUtils.fromFXImage(img, null), file.extension, file)
    } catch (e: IOException) {
        logger.error(e) { "Could not save image to file=$file" }
    }
}

fun File.openInApp() {
    when {
        AudioFileFormat.isSupported(this, AudioFileFormat.Use.PLAYBACK) -> PlaylistManager.use { it.addUri(toURI()) }
        isImage() -> APP.widgetManager.widgets.use<ImageDisplayFeature>(WidgetUse.NO_LAYOUT) { it.showImage(this) }
        else -> open()
    }
}

fun openInApp(files: List<File>) {
    if (files.isEmpty()) {
        return
    } else if (files.size==1) {
        files[0].openInApp()
    } else {
        val audio = files.filter { AudioFileFormat.isSupported(it, AudioFileFormat.Use.PLAYBACK) }
        val images = files.filter { it.isImage() }

        if (!audio.isEmpty())
            PlaylistManager.use { it.addUris(audio.map { it.toURI() }) }

        if (images.size==1) {
            APP.widgetManager.widgets.use<ImageDisplayFeature>(WidgetUse.NO_LAYOUT) { it.showImage(images[0]) }
        } else if (images.size>1) {
            APP.widgetManager.widgets.use<ImagesDisplayFeature>(WidgetUse.NO_LAYOUT) { it.showImages(images) }
        }
    }
}

enum class FileFlatter(@JvmField val flatten: (Collection<File>) -> Stream<File>) {
    NONE({ it.stream().distinct() }),
    DIRS({
        it.asSequence().distinct()
                .flatMap { sequenceOf(it).filter { it.isFile }+it.walk().filter { it.isDirectory } }
                .asStream()
    }),
    TOP_LVL({ it.stream().distinct().flatMap { it.listChildren() } }),
    TOP_LVL_AND_DIRS({
        it.stream().distinct()
                .flatMap { it.listChildren() }
                .flatMap { (sequenceOf(it).filter { it.isFile }+it.walk().filter { it.isDirectory }).asStream() }
    }),
    TOP_LVL_AND_DIRS_AND_WITH_COVER({

        fun File.hasCover(cache: HashSet<FastFile>): Boolean {
            val p = parentDirOrRoot
            val n = nameWithoutExtension
            return imageExtensionsRead.any { cache.contains(p.childOf("$n.$it")) }
        }

        fun File.walkDirsAndWithCover(): Sequence<File> {
            return if (isDirectory) {
                val dir = this
                val cmdDirs = """cmd /U /c dir /s /b /ad "${dir.absolutePath}" 2>nul"""
                val cmdFiles = """cmd /U /c dir /s /b /a-d "${dir.absolutePath}" 2>nul"""

                val dirs = try {
                    Runtime.getRuntime().exec(cmdDirs)
                            .inputStream.bufferedReader(Charsets.UTF_16LE)
                            .useLines { it.map { FastFile(it, true, false) }.toList() }
                } catch (e: Throwable) {
                    logger.error(e) { "Failed to read files in $this using command $cmdDirs" }
                    listOf<FastFile>()
                }
                val files = try {
                    Runtime.getRuntime().exec(cmdFiles)
                            .inputStream.bufferedReader(Charsets.UTF_16LE)
                            .useLines { it.map { FastFile(it, false, true) }.toList() }
                } catch (e: Throwable) {
                    logger.error(e) { "Failed to read files in $this using command $cmdFiles" }
                    listOf<FastFile>()
                }

                val cache = (dirs+files).toHashSet()
                cache.asSequence().filter { it.isDirectory || it.hasCover(cache) }
            } else {
                sequenceOf(this)
            }
        }

        it.asSequence().distinct().flatMap { it.walkDirsAndWithCover() }.asStream()
    }),
    ALL({ it.asSequence().distinct().flatMap { it.asFileTree() }.asStream() });
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
                                .inputStream.bufferedReader(Charsets.UTF_16LE)
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