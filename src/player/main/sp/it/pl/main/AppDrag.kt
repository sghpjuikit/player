package sp.it.pl.main

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Bounds
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.input.Clipboard
import javafx.scene.input.DragEvent
import javafx.scene.input.DragEvent.DRAG_DROPPED
import javafx.scene.input.DragEvent.DRAG_OVER
import javafx.scene.input.Dragboard
import javafx.stage.Window
import mu.KotlinLogging
import sp.it.pl.audio.SimpleSong
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.ui.objects.placeholder.DragPane
import sp.it.pl.layout.Component
import sp.it.pl.layout.controller.io.Output
import sp.it.util.async.future.Fut
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.async.onlyIfMatches
import sp.it.util.async.runLater
import sp.it.util.async.runNew
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
import sp.it.util.file.Util
import sp.it.util.functional.Util.listRO
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.onItemSyncWhile
import sp.it.util.reactive.syncNonNullIntoWhile
import sp.it.util.ui.drag.DataFormat
import sp.it.util.ui.drag.contains
import sp.it.util.ui.drag.get
import sp.it.util.ui.drag.handlerAccepting
import sp.it.util.units.uri
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Supplier
import javafx.scene.input.DataFormat as DataFormatFX

private val logger = KotlinLogging.logger {}
private val dirTmp = File(System.getProperty("java.io.tmpdir"))
private var dragData: Any? = null
private var dragDataId = AtomicLong(0).apply {
   Window.getWindows().onItemSyncWhile {
      it.sceneProperty().syncNonNullIntoWhile(Scene::rootProperty) {
         it.onEventUp(DRAG_DROPPED) {
            runLater(onlyIfMatches(this) { dragData = null })
         }
      }
   }
}

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

/** Equivalent to [Clipboard.getSystemClipboard]. */
val sysClipboard: Clipboard = Clipboard.getSystemClipboard()

/** Equivalent to [Clipboard.hasContent], but see [DataFormatAppOnly]. */
operator fun Clipboard.contains(format: DataFormatAppOnly<*>) = hasContent(format.format)

/** Equivalent to [Clipboard.getContent], but see [DataFormatAppOnly]. */
@Suppress("UNCHECKED_CAST")
operator fun <T: Any> Clipboard.get(format: DataFormatAppOnly<T>): T {
   failIf(format !in this) { "No data of $format in dragboard." }
   return dragData as T
}

/** Equivalent to [Clipboard.setContent], but see [DataFormatAppOnly]. */
operator fun <T: Any> Clipboard.set(format: DataFormatAppOnly<out T>, data: T) {
   dragData = data
   dragDataId.incrementAndGet()
   setContent(mapOf(format.format to ""))
}

/**
 * Data of this format is not serialized, but stored in memory and only available within the application.
 * Type-safe, see [DataFormat].
 */
class DataFormatAppOnly<T: Any>(id: String) {
   val format = DataFormatFX(id)
}

fun Clipboard.getAny(): Any? = when {
   Df.SONGS in this -> this[Df.SONGS]
   Df.COMPONENT in this -> this[Df.COMPONENT]
   Df.METADATA_GROUP in this -> this[Df.METADATA_GROUP]
   Df.FILES in this -> this[Df.FILES]
   Df.IMAGE in this -> this[Df.IMAGE]
   Df.URL in this -> this[Df.URL]
   Df.PLAIN_TEXT in this -> this[Df.PLAIN_TEXT]
   else -> dragData
}

fun Clipboard.getAnyFut(): Any? = when {
   Df.SONGS in this -> this[Df.SONGS]
   Df.COMPONENT in this -> this[Df.COMPONENT]
   Df.METADATA_GROUP in this -> this[Df.METADATA_GROUP]
   Df.FILES in this -> this[Df.FILES]
   Df.IMAGE in this -> this[Df.IMAGE]
   Df.URL in this -> when {
      url.isImage() -> futUrl(url)
      else -> this[Df.URL]
   }
   Df.PLAIN_TEXT in this -> this[Df.PLAIN_TEXT]
   else -> dragData
}

/** @return [Df.PLAIN_TEXT] or [Df.RTF] in dragboard */
fun Clipboard.getText(): String = when {
   Df.PLAIN_TEXT in this -> this[Df.PLAIN_TEXT]
   Df.RTF in this -> this[Df.RTF]
   else -> fail { "Dragboard must contain text" }
}

/** @return whether dragboard contains [Df.PLAIN_TEXT] or [Df.RTF] */
fun Clipboard.hasText(): Boolean = hasString() || hasRtf()

/** Sets both [Df.SONGS] and [Df.FILES]. */
fun Clipboard.setSongsAndFiles(items: List<Song>) {
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
fun Dragboard.hasAudio(): Boolean = Df.SONGS in this || (hasUrl() && url.isAudio()) || (hasFiles() && files.any { it.isAudio() || it.isDirectory })

/** @return list of songs as specified in [Dragboard.hasAudio] */
fun Dragboard.getAudio(): List<Song> = when {
   Df.SONGS in this -> this[Df.SONGS]
   hasFiles() -> findAudio(files).map { SimpleSong(it) }.toList()
   hasUrl() -> {
      when {
         url.isAudio() -> listOf(SimpleSong(uri(url)))
         else -> listOf()
      }
   }
   else -> fail { "Dragboard must contain audio" }
}

// TODO: clearly specify (and fix) contracts for getImage family

fun Dragboard.hasImageFile(): Boolean = (hasFiles() && files.any { it.isImage() })

fun Dragboard.getImageFile(): File? = files.firstOrNull { it.isImage() }

/**
 * Returns true if dragboard contains an image file/s. True guarantees the presence of the image.
 * Files denoting directories are ignored.
 *
 * @return true iff contains at least 1 img file or an img url
 */
fun Dragboard.hasImageFileOrUrl(): Boolean = hasImageFile() || (hasUrl() && url.isImage())

/** @return supplier of 1st found image according to [Dragboard.hasImageFileOrUrl] */
fun Dragboard.getImageFileOrUrl(): Fut<File?> = when {
   hasFiles() -> fut(getImageFile())
   hasUrl() && url.isImage() -> futUrl(url)
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
      val images = files.filter { it.isImage() }
      if (images.isNotEmpty())
         return fut(images)
   }
   if (hasUrl() && url.isImage()) {
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

/** Sets up drag support with specified characteristics for the specified node. See [DragPane.install]. */
fun Node.installDrag(icon: GlyphIcons, info: String, condition: (DragEvent) -> Boolean, action: (DragEvent) -> Unit) =
   installDrag(icon, info, condition, { false }, action)

/** Sets up drag support with specified characteristics for the specified node. See [DragPane.install]. */
fun Node.installDrag(icon: GlyphIcons, info: String, condition: (DragEvent) -> Boolean, exc: (DragEvent) -> Boolean, action: (DragEvent) -> Unit) =
   installDrag(icon, { info }, condition, exc, action)

/** Sets up drag support with specified characteristics for the specified node. See [DragPane.install]. */
fun Node.installDrag(icon: GlyphIcons, info: Supplier<out String>, condition: (DragEvent) -> Boolean, action: (DragEvent) -> Unit) =
   installDrag(icon, info, condition, { false }, action)

/** Sets up drag support with specified characteristics for the specified node. See [DragPane.install]. */
@JvmOverloads
fun Node.installDrag(icon: GlyphIcons, info: Supplier<out String>, condition: (DragEvent) -> Boolean, exc: (DragEvent) -> Boolean, action: (DragEvent) -> Unit, area: ((DragEvent) -> Bounds)? = null) = let { node ->
   node.addEventHandler(DRAG_OVER, handlerAccepting(condition, exc))
   node.addEventHandler(DRAG_DROPPED) { e ->
      if (condition(e)) {
         action(e)
         e.isDropCompleted = true
         e.consume()
      }
   }
   DragPane.install(node, icon, info, condition, exc, area)
}