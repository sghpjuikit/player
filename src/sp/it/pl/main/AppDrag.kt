package sp.it.pl.main

import javafx.scene.image.Image
import javafx.scene.input.Dragboard
import mu.KotlinLogging
import sp.it.pl.audio.SimpleSong
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.layout.Component
import sp.it.pl.layout.widget.controller.io.Output
import sp.it.pl.util.async.future.Fut
import sp.it.pl.util.async.future.Fut.Companion.fut
import sp.it.pl.util.async.runNew
import sp.it.pl.util.dev.fail
import sp.it.pl.util.dev.failIf
import sp.it.pl.util.file.AudioFileFormat
import sp.it.pl.util.file.AudioFileFormat.Use
import sp.it.pl.util.file.ImageFileFormat
import sp.it.pl.util.file.Util
import sp.it.pl.util.file.Util.getFilesAudio
import sp.it.pl.util.functional.Util.listRO
import sp.it.pl.util.graphics.drag.DataFormat
import sp.it.pl.util.graphics.drag.contains
import sp.it.pl.util.graphics.drag.get
import java.io.File
import java.io.IOException
import java.net.URI
import kotlin.streams.toList
import javafx.scene.input.DataFormat as DataFormatFX

private val logger = KotlinLogging.logger {}
private val dirTmp = File(System.getProperty("java.io.tmpdir"))
private var dragData: Any? = null   // TODO: avoid memory leak

object Df {
    /** See [DataFormatFX.PLAIN_TEXT] */
    @JvmField val PLAIN_TEXT = DataFormat<String>(DataFormatFX.PLAIN_TEXT)
    /** See [DataFormatFX.HTML] */
    @JvmField val HTML = DataFormat<String>(DataFormatFX.HTML)
    /** See [DataFormatFX.RTF] */
    @JvmField val RTF = DataFormat<String>(DataFormatFX.RTF)
    /** See [DataFormatFX.URL] */
    @JvmField val URL = DataFormat<String>(DataFormatFX.URL)
    /** See [DataFormatFX.IMAGE] */
    @JvmField val IMAGE = DataFormat<Image>(DataFormatFX.IMAGE)
    /** See [DataFormatFX.FILES] */
    @JvmField val FILES = DataFormat<List<File>>(DataFormatFX.FILES)
    /** Data Format for [java.util.List] of [sp.it.pl.audio.Song]. */
    @JvmField val SONGS = DataFormatAppOnly<List<Song>>("application/sp.it.player-items")
    /** Data Format for [sp.it.pl.layout.Component]. */
    @JvmField val COMPONENT = DataFormatAppOnly<Component>("application/sp.it.player-component")
    /** Data Format for widget [sp.it.pl.layout.widget.controller.io.Output] linking. */
    @JvmField val WIDGET_OUTPUT = DataFormatAppOnly<Output<*>>("application/sp.it.player-widget-output")
    /** Data Format for [sp.it.pl.audio.tagging.MetadataGroup]. */
    @JvmField val METADATA_GROUP = DataFormatAppOnly<MetadataGroup>("application/sp.it.player-metadata-group")
}

/** Equivalent to [Dragboard.hasContent], but see [DataFormatAppOnly]. */
operator fun Dragboard.contains(format: DataFormatAppOnly<*>) = hasContent(format.format)

/** Equivalent to [Dragboard.getContent], but see [DataFormatAppOnly]. */
@Suppress("UNCHECKED_CAST")
operator fun <T: Any> Dragboard.get(format: DataFormatAppOnly<T>): T {
    failIf(format !in this) { "No data of $format in dragboard." }
    return dragData as T
}

/** Equivalent to [Dragboard.setContent], but see [DataFormatAppOnly]. */
operator fun <T: Any> Dragboard.set(format: DataFormatAppOnly<out T>, data: T) {
    dragData = data
    setContent(mapOf(format.format to ""))
}

/**
 * Data of this format is not serialized, but stored in memory and only available within the application.
 * Type-safe, see [DataFormat].
 */
class DataFormatAppOnly<T: Any>(id: String) {
    val format = DataFormatFX(id)
}

fun Dragboard.getAny(): Any? = when {
    Df.SONGS in this -> this[Df.SONGS]
    Df.COMPONENT in this -> this[Df.COMPONENT]
    Df.METADATA_GROUP in this -> this[Df.METADATA_GROUP]
    Df.FILES in this -> this[Df.FILES]
    Df.IMAGE in this -> this[Df.IMAGE]
    Df.URL in this -> this[Df.URL]
    Df.PLAIN_TEXT in this -> this[Df.PLAIN_TEXT]
    else -> dragData
}

fun Dragboard.getAnyFut(): Any? = when {
    Df.SONGS in this -> this[Df.SONGS]
    Df.COMPONENT in this -> this[Df.COMPONENT]
    Df.METADATA_GROUP in this -> this[Df.METADATA_GROUP]
    Df.FILES in this -> this[Df.FILES]
    Df.IMAGE in this -> this[Df.IMAGE]
    Df.URL in this -> when {
        ImageFileFormat.isSupported(url) -> futUrl(url)
        else -> this[Df.URL]
    }
    Df.PLAIN_TEXT in this -> this[Df.PLAIN_TEXT]
    else -> dragData
}

/** @return [Df.PLAIN_TEXT] or [Df.RTF] in dragboard */
fun Dragboard.getText(): String = when {
    Df.PLAIN_TEXT in this -> this[Df.PLAIN_TEXT]
    Df.RTF in this -> this[Df.RTF]
    else -> fail { "Dragboard must contain text" }
}

/** @return whether dragboard contains [Df.PLAIN_TEXT] or [Df.RTF] */
fun Dragboard.hasText(): Boolean = hasString() || hasRtf()

/** Sets both [Df.SONGS] and [Df.FILES]. */
fun Dragboard.setSongsAndFiles(items: List<Song>) {
    this[Df.SONGS] = items
    this.setContent(
            mapOf(
                    Df.SONGS.format to "",
                    Df.FILES.format to items.mapNotNull { it.getFile() }
            )
    )
}

/**
 * Returns true if dragboard contains one or more songs, audio files, urls or directories.
 * Files denoting directories are considered, but not visited. Hence true does not guarantee presence of any song.
 *
 * Inspects [Df.SONGS], [Df.FILES], [Df.URL] in this exact order
 *
 * @return true iff contains at least 1 audio file or audio url or (any) directory
 */
fun Dragboard.hasAudio(): Boolean = Df.SONGS in this ||
        (hasFiles() && Util.containsAudioFileOrDir(files, Use.APP)) ||
        (hasUrl() && AudioFileFormat.isSupported(url, Use.APP))

/** @return list of songs as specified in [Dragboard.hasAudio] */
fun Dragboard.getAudio(): List<Song> = when {
    Df.SONGS in this -> this[Df.SONGS]
    hasFiles() -> getFilesAudio(files, Use.APP, Integer.MAX_VALUE).map { SimpleSong(it) }.toList()
    hasUrl() -> {
        val url = url
        if (AudioFileFormat.isSupported(url, Use.APP)) listOf(SimpleSong(URI.create(url))) else listOf()
    }
    else -> fail { "Dragboard must contain audio" }
}

// TODO: clearly specify (and fix) contracts for getImage family

fun Dragboard.hasImageFile(): Boolean = (hasFiles() && Util.containsImageFiles(files))

fun Dragboard.getImageFile(): File? = Util.getImageFiles(files).firstOrNull()

/**
 * Returns true if dragboard contains an image file/s. True guarantees the presence of the image.
 * Files denoting directories are ignored.
 *
 * @return true iff contains at least 1 img file or an img url
 */
fun Dragboard.hasImageFileOrUrl(): Boolean = (hasFiles() && Util.containsImageFiles(files)) || (hasUrl() && ImageFileFormat.isSupported(url))

/** @return supplier of 1st found image according to [Dragboard.hasImageFileOrUrl] */
fun Dragboard.getImageFileOrUrl(): Fut<File?> = when {
    hasFiles() -> fut(Util.getImageFiles(files).firstOrNull())
    hasUrl() && ImageFileFormat.isSupported(url) -> futUrl(url)
    else -> fail { "Dragboard must contain image file" }
}

/**
 * Returns supplier of image files in the dragboard.
 * Always call [.hasImage] before this
 * method to check the content.
 *
 * The supplier supplies:
 * - If there was an url, single image will be downloaded on background thread, stored as temporary file and returned
 *   as singleton list. If any error occurs, empty list is returned.
 * - if there were files, all image files
 * - Empty list otherwise
 *
 * @return supplier, never null
 */
fun Dragboard.hasImageFilesOrUrl(): Fut<List<File>> {
    if (hasFiles()) {
        val images = Util.getImageFiles(files)
        if (!images.isEmpty())
            return fut(images)
    }
    if (hasUrl() && ImageFileFormat.isSupported(url)) {
        val url = url
        return runNew {
            try {
                val f = Util.saveFileTo(url, dirTmp)
                f.deleteOnExit()
                return@runNew listOf(f)
            } catch (ex: IOException) {
                return@runNew listRO<File>()
            }
        }
    }
    return fut(listOf())
}

private fun futUrl(url: String): Fut<File?> = runNew {
    try {
        // TODO: this can all fail when the certificate is not trusted. Security is fine, but user does not care
        // if a site he uses wont work due to this... E.g. anime-pictures.net
        // https://code.google.com/p/jsslutils/wiki/SSLContextFactory
        val f = Util.saveFileTo(url, dirTmp)
        f.deleteOnExit()
        f
    } catch (e: IOException) {
        logger.error(e) { "Could not download file from url=$url" }
        null
    }
}