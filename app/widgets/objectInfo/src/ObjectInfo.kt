package objectInfo

import com.drew.imaging.ImageMetadataReader
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicLong
import javafx.scene.control.TextArea
import javafx.scene.image.Image
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode.V
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.TransferMode.MOVE
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Priority.SOMETIMES
import kotlin.reflect.KClass
import mu.KLogging
import org.jaudiotagger.tag.wav.WavTag
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.PlaylistSongGroup
import sp.it.pl.audio.tagging.readAudioFile
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.feature.Opener
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconUN
import sp.it.pl.main.WidgetTags.DEVELOPMENT
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.main.computeDataInfo
import sp.it.pl.main.detectContent
import sp.it.pl.main.emScaled
import sp.it.pl.main.getAny
import sp.it.pl.main.installDrag
import sp.it.pl.main.tableViewForClass
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.pl.ui.objects.table.FilteredTable
import sp.it.pl.ui.pane.ImageFlowPane
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.async.FX
import sp.it.util.collections.collectionUnwrap
import sp.it.util.collections.getElementClass
import sp.it.util.dev.Blocks
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.json.JsValue
import sp.it.util.file.json.toPrettyS
import sp.it.util.file.toFileOrNull
import sp.it.util.file.type.MimeGroup
import sp.it.util.file.type.mimeType
import sp.it.util.functional.asIs
import sp.it.util.functional.getOr
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.text.Jwt
import sp.it.util.text.ifNotBlank
import sp.it.util.ui.image.toFX
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year

class ObjectInfo(widget: Widget): SimpleController(widget), Opener {
   private val inputItems = io.i.create("To display", null, ::open)
   private val info = TextArea()
   private val dataRepsPane = HBox(15.emScaled)
   private val dataRepThumb = Thumbnail()
   private val dataRepTextArea = TextArea()
   private var dataRepTable: FilteredTable<Any>? = null
   private val openId = AtomicLong(1L)

   init {
      root.prefSize = 600.emScaled x 300.emScaled
      root.consumeScrolling()
      root.lay += ImageFlowPane(null, null).apply {
         dataRepThumb.image sync { setImageVisible(it!=null) }
         setGap(15.emScaled)
         setImage(dataRepThumb)
         setContent(
            dataRepsPane.apply {
               lay += info.apply {
                  isEditable = false
               }
            }
         )
      }

      root.onEventUp(KEY_PRESSED, V, false) { e ->
         if (e.isShortcutDown) {
            val data = Clipboard.getSystemClipboard().getAny()
            openAndDetect(data, !e.isShiftDown)
         }
      }
      root.installDrag(
         IconFA.INFO,
         "Display information about the object",
         { true },
         { openAndDetect(it.dragboard.getAny(), it.transferMode==MOVE) }
      )
   }

   @Blocks
   fun audioMetadata(d: File): String =
      d.readAudioFile().map { it.audioHeader to it.tag }.map { (header, tag) ->
         "\nHeader %s:\n%s\nTag%s:%s".format(
            "(" + header::class.toUi() + ")",
            header.toString().lineSequence().map { it.trimStart() }.joinToString("\n\t"),
            when (tag) {
               null -> ""
               is WavTag -> " (" + tag.activeTag::class.toUi() + ")"
               else -> " (" + tag::class.toUi() + ")"
            },
            tag?.net { it.fields.asSequence().joinToString("") { "\n\t${it.id}:$it" } } ?: " <none>"
         )
      }.getOr("")


   @Blocks
   fun imageMetadata(f: File): String =
      runTry {
         ImageMetadataReader.readMetadata(f)
            .directories.asSequence()
            .filter { it.name!="File" }
            .flatMap { it.tags }
            .joinToString("\n") { "${it.directoryName} > ${it.tagName}: ${it.description.toUi()}" }
      }.getOr("")

   fun openAndDetect(data: Any?, detectContent: Boolean) =
      open(if (detectContent) data.detectContent() else data)

   override fun open(data: Any?) {
      val d = collectionUnwrap(data)
      val id = openId.incrementAndGet()

      computeDataInfo(d).then {
         when {
            d is File && d.mimeType().group==MimeGroup.image -> it + imageMetadata(d).ifNotBlank { "\n" + it }
            d is File && d.mimeType().group==MimeGroup.audio -> it + audioMetadata(d).ifNotBlank { "\n" + it }
            d is Song -> it + d.getFile()?.net { audioMetadata(it) }.orEmpty().ifNotBlank { "\n" + it }
            else -> it
         }
      }.onDone(FX) {
         if (id==openId.get()) {
            info.text = it.toTry().getOr("Failed to obtain data information.")
         }
      }

		val dataAsS: String? = when {
         d == null -> null
         d is String -> d
         d is Throwable -> d.stacktraceAsString
         d is JsValue -> d.toPrettyS()
         d is Jwt || d::class.isData -> d.toUi()
         else -> null
      }
      val dataAsC: Collection<Any?>? = when {
         dataAsS!=null -> null
         d is MetadataGroup && d.grouped.size>1 -> d.grouped
         d is PlaylistSongGroup && d.songs.size>1 -> d.songs
         d is Collection<*> -> d
         else -> null
      }

      dataRepsPane.lay -= dataRepTextArea
      dataRepsPane.lay -= info
      dataRepTable.ifNotNull { dataRepsPane.lay -= it.root }
      if (dataAsS!=null) {
         dataRepTextArea.isEditable = false
         dataRepTextArea.text = dataAsS
         dataRepsPane.lay(SOMETIMES) += dataRepTextArea
      }
      if (dataAsC!=null) {
         val type = dataAsC.getElementClass().kotlin.asIs<KClass<Any>>()
         val t = tableViewForClass(type) {
            setItemsRaw(dataAsC)
         }
         dataRepTable = t
         dataRepsPane.lay(SOMETIMES) += t.root
      }
      dataRepsPane.lay(ALWAYS) += info

      when (d) {
         is Image -> dataRepThumb.loadImage(d)
         is BufferedImage -> dataRepThumb.loadImage(d.toFX())
         else -> dataRepThumb.loadFile(
            when (d) {
               is File -> d
               is URI -> d.toFileOrNull()
               is URL -> d.toFileOrNull()
               is Path -> d.toFile()
               is Song -> d.getFile()
               is MetadataGroup -> d.grouped.takeIf { it.size==1 }?.firstOrNull()?.getFile()
               is PlaylistSongGroup -> d.playlist.takeIf { it.size==1 }?.firstOrNull()?.getFile()
               else -> null
            }
         )
      }

   }

   override fun focus() = Unit

   companion object: WidgetCompanion, KLogging() {
      override val name = "Object Info"
      override val description = "Displays information about or preview of an object"
      override val descriptionLong = "$description."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 1, 1)
      override val isSupported = true
      override val year = year(2020)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(UTILITY, DEVELOPMENT)
      override val summaryActions = listOf(
         ShortcutPane.Entry("Data", "Set data", "Drag & drop object"),
      )
   }
}