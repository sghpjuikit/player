package fileInfo

import fileInfo.FileInfo.Sort.ALPHANUMERIC
import fileInfo.FileInfo.Sort.SEMANTIC
import javafx.geometry.Insets
import javafx.geometry.Orientation.VERTICAL
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.OverrunStyle.ELLIPSIS
import javafx.scene.layout.TilePane
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.Metadata.Companion.EMPTY
import sp.it.pl.audio.tagging.Metadata.Field.Companion.ALBUM
import sp.it.pl.audio.tagging.Metadata.Field.Companion.ALBUM_ARTIST
import sp.it.pl.audio.tagging.Metadata.Field.Companion.ARTIST
import sp.it.pl.audio.tagging.Metadata.Field.Companion.BITRATE
import sp.it.pl.audio.tagging.Metadata.Field.Companion.CATEGORY
import sp.it.pl.audio.tagging.Metadata.Field.Companion.COMMENT
import sp.it.pl.audio.tagging.Metadata.Field.Companion.COMPOSER
import sp.it.pl.audio.tagging.Metadata.Field.Companion.COVER
import sp.it.pl.audio.tagging.Metadata.Field.Companion.DISCS_INFO
import sp.it.pl.audio.tagging.Metadata.Field.Companion.DISCS_TOTAL
import sp.it.pl.audio.tagging.Metadata.Field.Companion.ENCODING
import sp.it.pl.audio.tagging.Metadata.Field.Companion.FILENAME
import sp.it.pl.audio.tagging.Metadata.Field.Companion.FILESIZE
import sp.it.pl.audio.tagging.Metadata.Field.Companion.FORMAT
import sp.it.pl.audio.tagging.Metadata.Field.Companion.GENRE
import sp.it.pl.audio.tagging.Metadata.Field.Companion.LENGTH
import sp.it.pl.audio.tagging.Metadata.Field.Companion.PATH
import sp.it.pl.audio.tagging.Metadata.Field.Companion.PLAYCOUNT
import sp.it.pl.audio.tagging.Metadata.Field.Companion.PUBLISHER
import sp.it.pl.audio.tagging.Metadata.Field.Companion.RATING
import sp.it.pl.audio.tagging.Metadata.Field.Companion.TITLE
import sp.it.pl.audio.tagging.Metadata.Field.Companion.TRACKS_TOTAL
import sp.it.pl.audio.tagging.Metadata.Field.Companion.TRACK_INFO
import sp.it.pl.audio.tagging.Metadata.Field.Companion.YEAR
import sp.it.pl.audio.tagging.writeNoRefresh
import sp.it.pl.audio.tagging.writeRating
import sp.it.pl.ui.objects.image.Cover.CoverSource
import sp.it.pl.ui.objects.image.ThumbnailWithAdd
import sp.it.pl.ui.objects.rating.Rating
import sp.it.pl.ui.pane.ImageFlowPane
import sp.it.pl.ui.pane.SlowAction
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.emScaled
import sp.it.pl.main.getAudio
import sp.it.pl.main.hasAudio
import sp.it.pl.main.installDrag
import sp.it.pl.main.toMetadata
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.collections.setTo
import sp.it.util.conf.cCheckList
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.dev.failIfFxThread
import sp.it.util.file.Util.copyFileSafe
import sp.it.util.file.Util.copyFiles
import sp.it.util.math.max
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import java.io.File
import java.net.URI
import java.nio.file.StandardCopyOption
import java.util.ArrayList
import javafx.scene.control.ContentDisplay.RIGHT
import javafx.scene.control.Hyperlink
import kotlin.math.ceil
import kotlin.math.floor
import sp.it.pl.main.WidgetTags
import sp.it.pl.main.appHyperlinkFor
import sp.it.pl.main.detectContent
import sp.it.util.functional.asIf
import sp.it.util.ui.Util
import sp.it.util.units.em

private typealias MField = Metadata.Field<*>

@Widget.Info(
   author = "Martin Polakovic",
   name = "File Info",
   description = "Displays song information. Supports rating change.",
   howto = """    Displays metadata of a particular song. Song can be set manually, e.g., by drag & drop, or the widget can follow the playing or table selections (playlist, etc.).

      Available actions:
          Cover left click : Browse file system & set cover
          Cover right click : Opens context menu
          Rater left click : Rates displayed song
          Drag&Drop songs : Displays information for the first song
          Drag&Drop image on cover: Sets images as cover
      """,
   version = "1.0.0",
   year = "2015",
   tags = [ WidgetTags.AUDIO ]
)
class FileInfo(widget: Widget): SimpleController(widget), SongReader {
   private val cover = ThumbnailWithAdd()
   private val tiles: TilePane = FieldsPane()
   private val layout = ImageFlowPane(cover, tiles)
   private val gap1 = Label(" ")
   private val gap2 = Label(" ")
   private val gap3 = Label(" ")
   private val labels: MutableList<Label> = ArrayList()

   private var data: Metadata = EMPTY
   private val dataReading = EventReducer.toLast<Song?>(200.0) { setValue(it) }
   val dataIn = io.i.create<Song?>("To display", null, dataReading::push)
   val dataOut = io.o.create<Metadata>("Displayed", EMPTY)

   val minColumnWidth by cv(150.0).attach { tiles.requestLayout() }.def(name = "Column width", info = "Minimal width for field columns.")
   val coverSource by cv(CoverSource.ANY).attach { setCover(it) }.def(name = "Cover source", info = "Source for cover image.")
   val overrunStyle by cv(ELLIPSIS).def(name = "Text clipping method", info = "Style of clipping text when too long.")
   val showEmptyFields by cv(true).attach { update() }.def(name = "Show empty fields", info = "Show empty fields.")
   val groupFields by cv(SEMANTIC).attach { update() }.def(name = "Group fields", info = "Use gaps to separate fields into group.")
   val allowNoContent by cv(false).def(name = "Allow no content", info = "Otherwise shows previous content when the new content is empty.")

   private val fieldsM by cCheckList(
      TITLE, TRACK_INFO, DISCS_INFO, LENGTH, ARTIST,
      ALBUM, ALBUM_ARTIST, YEAR, GENRE, COMPOSER,
      PUBLISHER, CATEGORY, RATING, PLAYCOUNT, COMMENT,
      FILESIZE, FILENAME, FORMAT, BITRATE, ENCODING, PATH, COVER
   )
   private val fieldsAll = fieldsM.all.mapIndexed { semanticIndex, field ->
      when (field) {
         RATING -> RatingField(semanticIndex)
         COVER -> CoverField()
         PATH -> LocationField(semanticIndex)
         COMMENT -> CommentField(semanticIndex)
         else -> LField(semanticIndex, field)
      }
   }
   private val fieldCover = fieldsAll.filterIsInstance<CoverField>().first()
   private val fieldRating = fieldsAll.filterIsInstance<RatingField>().first()
   private val fieldsL = fieldsAll.filterIsInstance<LField>()
   private val fieldConfigs = fieldsAll.associateBy { it.field }

   fun isEmpty() = data===EMPTY

   override fun read(song: Song?) {
      dataIn.value = song
   }

   override fun read(songs: List<Song>) = read(songs.firstOrNull())

   private fun setValue(song: Song?) = when (song) {
      null -> setValue(EMPTY)
      is Metadata -> setValue(song)
      else -> song.toMetadata { setValue(it) }
   }

   private fun setValue(m: Metadata) {
      if (!allowNoContent.value && m===EMPTY) return

      data = m
      dataOut.value = m
      fieldsAll.forEach { if (it.shouldBeVisible) it.update(m) }
      update()
   }

   private fun update() {
      labels.clear()
      cover.pane.isDisable = isEmpty()

      when (groupFields.value) {
         SEMANTIC -> {
            labels += fieldsL.sortedBy { it.semanticIndex }
            labels.add(4, gap1)
            labels.add(10, gap2)
            labels.add(17, gap3)
         }
         ALPHANUMERIC -> {
            labels += fieldsL.sortedBy { it.name }
         }
      }

      fieldsL.forEach { it.setHide() }
      tiles.children setTo labels
      tiles.requestLayout()
   }

   private fun setCover(source: CoverSource) {
      val id = data
      runIO {
         id.getCover(source).getImage()
      } ui {
         if (id===data) cover.loadImage(it)
      }
   }

   private fun setAsCover(file: File?, setAsCover: Boolean) {
      failIfFxThread()
      if (file==null || !data.isFileBased()) return

      if (setAsCover) copyFileSafe(file, data.getLocation(), "cover")
      else copyFiles(listOf(file), data.getLocation(), StandardCopyOption.REPLACE_EXISTING)
      runFX { setCover(coverSource.value) }
   }

   private fun tagAsCover(file: File?, includeAlbum: Boolean) {
      failIfFxThread()
      if (file==null || !data.isFileBased()) return

      val items = when {
         includeAlbum -> APP.db.songs.o.value.asSequence()
            .filter { it.getAlbum()!=null && it.getAlbum()==data.getAlbum() }
            .toHashSet() + data
         else -> setOf(data)
      }
      items.writeNoRefresh { it.setCover(file) }
      APP.audio.refreshSongs(items)
   }

   enum class Sort {
      SEMANTIC, ALPHANUMERIC
   }

   private interface XField {
      val field: MField
      var shouldBeVisible: Boolean
      fun update(m: Metadata)
   }

   private inner class CoverField: XField {
      override val field = COVER
      override var shouldBeVisible = true
      override fun update(m: Metadata) {
         setCover(coverSource.value)
      }
   }

   private inner class RatingField(semanticIndex: Int): LField(semanticIndex, RATING) {
      private val rater = Rating()

      init {
         contentDisplay = RIGHT
         graphic = rater.apply {
            icons syncFrom APP.ui.ratingIconCount on onClose
            partialRating syncFrom APP.ui.ratingIsPartial on onClose
            editable.value = true
            onRatingEdited = data::writeRating
         }
      }

      override fun update(m: Metadata) {
         super.update(m)
         rater.rating.value = m.getRatingPercent()
      }

      override fun setHide() {
         isDisable = false
         if (!shouldBeVisible || (!showEmptyFields.value && isValueEmpty)) labels.remove(this)
      }
   }

   private inner class CommentField(semanticIndex: Int): LField(semanticIndex, COMMENT) {

      init {
         contentDisplay = RIGHT
         graphic = null
      }

      override fun update(m: Metadata) {
         super.update(m)
         val vText = m.getFieldS(field, "").replace('\r', ' ').replace('\n', ',')
         val v = vText.detectContent()
         when {
            m == EMPTY -> {
               graphic = null
               text = "$name: "
            }
            v is URI -> {
               text = "$name: "
               graphic = appHyperlinkFor(v).apply { maxWidth = this@CommentField.maxWidth }// - (this@LocationField.graphicTextGap max 0.0) - this@LocationField.width }
            }
            else -> {
               text = "$name: $vText"
               graphic = null
            }
         }
      }

      override fun setHide() {
         isDisable = false
         if (!shouldBeVisible || (!showEmptyFields.value && isValueEmpty)) labels.remove(this)
      }
   }

   private inner class LocationField(semanticIndex: Int): LField(semanticIndex, PATH) {

      init {
         contentDisplay = RIGHT
         graphic = null
      }

      override fun update(m: Metadata) {
         super.update(m)
         graphic = when {
            m == EMPTY -> null
            m.isFileBased() -> appHyperlinkFor(m.getLocation()!!).apply { maxWidth = this@LocationField.maxWidth }// - (this@LocationField.graphicTextGap max 0.0) - this@LocationField.width }
            else -> appHyperlinkFor(m.uri).apply { maxWidth = this@LocationField.maxWidth }// - (this@LocationField.graphicTextGap max 0.0) - this@LocationField.width }
         }
      }

      override fun setHide() {
         isDisable = false
         if (!shouldBeVisible || (!showEmptyFields.value && isValueEmpty)) labels.remove(this)
      }
   }

   private open inner class LField(semanticIndex: Int, field: MField): Label(), XField {
      override val field = field
      override var shouldBeVisible = true
      protected var isValueEmpty: Boolean = false
      val semanticIndex = semanticIndex
      val name = when (field) {
         DISCS_TOTAL -> "disc"
         TRACKS_TOTAL -> "track"
         PATH -> "location"
         else -> field.name().lowercase()
      }

      init {
         textOverrunProperty() syncFrom overrunStyle
      }

      override fun update(m: Metadata) {
         isValueEmpty = field.isFieldEmpty(m)
         val v = when {
            m===EMPTY || field==RATING || field==PATH -> ""
            else -> m.getFieldS(field, "").replace('\r', ' ').replace('\n', ',')
         }
         text = "$name: $v"
      }

      open fun setHide() {
         isDisable = isValueEmpty
         if (!shouldBeVisible || (!showEmptyFields.value && isValueEmpty)) labels.remove(this)
      }
   }

   @Suppress("MoveVariableDeclarationIntoWhen")
   private inner class FieldsPane: TilePane(VERTICAL, 10.0, 0.0) {
      override fun layoutChildren() {
         val width = width
         val height = height
         val cellH = 2.em.emScaled + vgap
         val rows = 1 max floor((height max cellH)/cellH).toInt()
         val columns = 1 max ceil(labels.size.toDouble()/rows.toDouble()).toInt()
         var cellW = when (columns) {
            1, 0 -> width
            else -> (width + hgap)/columns - hgap
         }

         // adhere to requested minimum size
         cellW = cellW max minColumnWidth.value
         val w = cellW
         prefTileWidth = w
         labels.forEach { it.maxWidth = w }
         super.layoutChildren()

         // hyperlinks width fix
         labels.forEach {
            it.graphic?.asIf<Hyperlink>()?.maxWidth = w - it.graphicTextGap - Util.computeTextWidth(it.font, it.text)
         }
      }
   }

   init {
      root.prefSize = 400.0.emScaled x 400.0.emScaled

      // keep updated content (unless the content is scheduled for change, then this could cause invalid content)
      APP.audio.onSongRefresh { refreshed -> if (!dataReading.hasEventsQueued()) refreshed.ifHasE(data, ::read) } on onClose

      fieldsM.onChangeAndNow {
         fieldsM.forEach { (field, selected) -> fieldConfigs[field]?.shouldBeVisible = selected }
         layout.setImageVisible(fieldCover.shouldBeVisible)
         layout.setContentVisible(fieldsL.any { it.shouldBeVisible })
      }
      fieldsM.onChange {
         update()
      }

      cover.pane.isDisable = true
      cover.isBackgroundVisible = false
      cover.isBorderToImage = false
      cover.onFileDropped = { file ->
         if (data.isFileBased())
            APP.ui.actionPane.orBuild.show(File::class.java, file, true,
               SlowAction(
                  "Copy and set as album cover",
                  "Sets image as cover. Copies file to destination and renames it to 'cover' so it is recognized as album" +
                     " cover. Any previous cover file will be preserved by renaming.\n\nDestination: ${data.getLocation()!!.path}",
                  IconFA.PASTE,
                  { setAsCover(it, true) }
               ),
               SlowAction(
                  "Copy to location",
                  "Copies image to destination. Any such existing file is overwritten.\n\nDestination: ${data.getLocation()!!.path}",
                  IconFA.COPY,
                  { setAsCover(it, false) }
               ),
               SlowAction(
                  "Write to tag (single)", "Writes image as cover to song tag. Other songs of the song's album remain untouched.",
                  IconFA.TAG,
                  { tagAsCover(it, false) }
               ),
               SlowAction(
                  "Write to tag (album)",
                  "Writes image as cover to all songs in this song's album. Only songs in the library are considered." +
                     " Songs with no album are ignored. At minimum the displayed song will be updated (even if not in" +
                     " library or has no album).",
                  IconFA.TAGS,
                  { tagAsCover(it, true) }
               )
            )
      }
      layout.setMinContentSize(200.0, 120.0)
      layout.setGap(15.emScaled)
      tiles.padding = Insets(5.0)
      root.lay += layout

      // align tiles from left top & tile content to center left
      tiles.alignment = Pos.TOP_LEFT
      tiles.tileAlignment = Pos.CENTER_LEFT

      root.installDrag(
         IconMA.DETAILS,
         "Display",
         { it.dragboard.hasAudio() },
         { it.dragboard.getAudio().firstOrNull()?.let { read(it) } }
      )

      root.sync1IfInScene { dataIn.bindDefaultIf1stLoad(APP.audio.playing.o) } on onClose
   }
}