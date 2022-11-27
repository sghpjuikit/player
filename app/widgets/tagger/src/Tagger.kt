package tagger

import javafx.collections.FXCollections.observableArrayList
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.VPos
import javafx.scene.Cursor.HAND
import javafx.scene.Node
import javafx.scene.control.ColorPicker
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS
import javafx.scene.control.ScrollPane
import javafx.scene.control.ScrollPane.ScrollBarPolicy
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
import javafx.scene.effect.BoxBlur
import javafx.scene.input.DragEvent
import javafx.scene.input.DragEvent.DRAG_DROPPED
import javafx.scene.input.DragEvent.DRAG_OVER
import javafx.scene.input.KeyCode.BACK_SPACE
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseDragEvent.DRAG_DETECTED
import javafx.scene.input.MouseDragEvent.MOUSE_DRAG_RELEASED
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.TransferMode.COPY
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Priority.NEVER
import javafx.scene.layout.Priority.SOMETIMES
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import javafx.util.Callback
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.Metadata.Field.ADDED_TO_LIBRARY
import sp.it.pl.audio.tagging.Metadata.Field.ALBUM
import sp.it.pl.audio.tagging.Metadata.Field.ALBUM_ARTIST
import sp.it.pl.audio.tagging.Metadata.Field.ARTIST
import sp.it.pl.audio.tagging.Metadata.Field.CATEGORY
import sp.it.pl.audio.tagging.Metadata.Field.COLOR
import sp.it.pl.audio.tagging.Metadata.Field.COMMENT
import sp.it.pl.audio.tagging.Metadata.Field.COMPOSER
import sp.it.pl.audio.tagging.Metadata.Field.COVER
import sp.it.pl.audio.tagging.Metadata.Field.CUSTOM1
import sp.it.pl.audio.tagging.Metadata.Field.CUSTOM2
import sp.it.pl.audio.tagging.Metadata.Field.CUSTOM3
import sp.it.pl.audio.tagging.Metadata.Field.CUSTOM4
import sp.it.pl.audio.tagging.Metadata.Field.CUSTOM5
import sp.it.pl.audio.tagging.Metadata.Field.DISC
import sp.it.pl.audio.tagging.Metadata.Field.DISCS_TOTAL
import sp.it.pl.audio.tagging.Metadata.Field.FIRST_PLAYED
import sp.it.pl.audio.tagging.Metadata.Field.GENRE
import sp.it.pl.audio.tagging.Metadata.Field.LAST_PLAYED
import sp.it.pl.audio.tagging.Metadata.Field.LYRICS
import sp.it.pl.audio.tagging.Metadata.Field.MOOD
import sp.it.pl.audio.tagging.Metadata.Field.PLAYCOUNT
import sp.it.pl.audio.tagging.Metadata.Field.PUBLISHER
import sp.it.pl.audio.tagging.Metadata.Field.RATING
import sp.it.pl.audio.tagging.Metadata.Field.TAGS
import sp.it.pl.audio.tagging.Metadata.Field.TITLE
import sp.it.pl.audio.tagging.Metadata.Field.TRACK
import sp.it.pl.audio.tagging.Metadata.Field.TRACKS_TOTAL
import sp.it.pl.audio.tagging.Metadata.Field.YEAR
import sp.it.pl.audio.tagging.readTask
import sp.it.pl.audio.tagging.setOnDone
import sp.it.pl.audio.tagging.write
import sp.it.pl.core.CoreMenus
import sp.it.pl.layout.Widget
import sp.it.pl.main.WidgetTags.LIBRARY
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.feature.SongReader
import sp.it.pl.layout.feature.SongWriter
import sp.it.pl.main.APP
import sp.it.pl.main.AppProgress
import sp.it.pl.main.AppTexts
import sp.it.pl.main.IconFA
import sp.it.pl.main.Widgets.SONG_TAGGER_NAME
import sp.it.pl.main.appProgressIndicator
import sp.it.pl.main.emScaled
import sp.it.pl.main.formIcon
import sp.it.pl.main.getAudio
import sp.it.pl.main.hasAudio
import sp.it.pl.main.installDrag
import sp.it.pl.main.isAudioEditable
import sp.it.pl.main.isImageJaudiotagger
import sp.it.pl.plugin.impl.Notifier
import sp.it.pl.ui.objects.textfield.MoodItemNode
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.image.ThumbnailWithAdd
import sp.it.pl.ui.objects.spinner.Spinner
import sp.it.pl.ui.objects.window.NodeShow.LEFT_CENTER
import sp.it.pl.ui.objects.window.ShowArea.WINDOW_ACTIVE
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.async.runIO
import sp.it.util.collections.mapset.MapSet
import sp.it.util.collections.materialize
import sp.it.util.collections.setTo
import sp.it.util.dev.fail
import sp.it.util.file.div
import sp.it.util.file.type.mimeType
import sp.it.util.functional.asIf
import sp.it.util.functional.getOr
import sp.it.util.functional.runTry
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.text.keys
import sp.it.util.text.nameUi
import sp.it.util.text.pluralUnit
import sp.it.util.type.type
import sp.it.util.ui.borderPane
import sp.it.util.ui.containsMouse
import sp.it.util.ui.createIcon
import sp.it.util.ui.drag.handlerAccepting
import sp.it.util.ui.dsl
import sp.it.util.ui.gridPane
import sp.it.util.ui.gridPaneColumn
import sp.it.util.ui.gridPaneRow
import sp.it.util.ui.hBox
import sp.it.util.ui.image.getImageDim
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.listView
import sp.it.util.ui.lookupId
import sp.it.util.ui.maxSize
import sp.it.util.ui.minPrefMaxWidth
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.scrollPane
import sp.it.util.ui.stackPane
import sp.it.util.ui.textAlignment
import sp.it.util.ui.textArea
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.ui.x2
import sp.it.util.units.em
import sp.it.util.units.version
import sp.it.util.units.year
import java.io.File
import java.net.URI
import java.time.Year
import java.util.ArrayList
import sp.it.pl.ui.objects.textfield.SpitTextField as DTextField
import java.util.concurrent.atomic.AtomicLong
import javafx.scene.layout.GridPane.REMAINING
import kotlin.math.ceil
import kotlin.math.roundToInt
import sp.it.pl.audio.tagging.Metadata.Companion.SEPARATOR_UNIT
import sp.it.pl.main.IconMD
import sp.it.pl.main.WidgetTags.AUDIO
import sp.it.pl.main.autocompleteSuggestionsFor
import sp.it.pl.ui.labelForWithClick
import sp.it.pl.ui.objects.autocomplete.AutoCompletion.Companion.autoComplete
import sp.it.pl.ui.objects.complexfield.StringTagTextField
import sp.it.pl.ui.objects.complexfield.TagTextField
import sp.it.pl.ui.objects.image.ArtworkCover
import sp.it.util.access.focused
import sp.it.util.dev.failCase
import sp.it.util.functional.orNull
import sp.it.util.math.clip
import sp.it.util.math.max
import sp.it.util.reactive.attachFalse
import sp.it.util.reactive.onChange
import sp.it.util.reactive.zip
import sp.it.util.text.capital
import sp.it.util.text.decapital
import sp.it.util.text.splitNoEmpty
import sp.it.util.type.atomic
import sp.it.util.type.property
import sp.it.util.ui.show

typealias Predicate = (String) -> Boolean
typealias Converter = (String) -> String

class Tagger(widget: Widget): SimpleController(widget), SongWriter, SongReader {
   val inputValue = io.i.create<List<Song>>("Edit", listOf()) { read(it) }

   val coverV = ThumbnailWithAdd(IconFA.PLUS, "Change cover")
   val descriptionL = Label()
   val scrollContent = AnchorPane()
   val subroot = buildLayout().apply {
      root.lay += this
   }
   val content: VBox = root.lookupId("content")
   val header: BorderPane = root.lookupId("header")
   val headerProgressI: Spinner = header.lookupId("headerProgressI")
   val scrollRoot: ScrollPane = root.lookupId("scrollRoot")
   val grid: GridPane = scrollContent.lookupId("grid")
   val titleF: DTextField = grid.lookupId("titleF")
   val albumF: DTextField = grid.lookupId("albumF")
   val artistF: DTextField = grid.lookupId("artistF")
   val albumArtistF: DTextField = grid.lookupId("albumArtistF")
   val composerF: DTextField = grid.lookupId("composerF")
   val publisherF: DTextField = grid.lookupId("publisherF")
   val trackF: DTextField = grid.lookupId("trackF")
   val tracksTotalF: DTextField = grid.lookupId("tracksTotalF")
   val discF: DTextField = grid.lookupId("discF")
   val discsTotalF: DTextField = grid.lookupId("discsTotalF")
   val genreF: DTextField = grid.lookupId("genreF")
   val categoryF: DTextField = grid.lookupId("categoryF")
   val yearF: DTextField = grid.lookupId("yearF")
   val ratingF: DTextField = grid.lookupId("ratingF")
   val ratingPF: DTextField = grid.lookupId("ratingPF")
   val playcountF: DTextField = grid.lookupId("playcountF")
   val commentF: TextArea = grid.lookupId("commentF")
   val colorF: DTextField = grid.lookupId("colorF")
   val colorFValue = vn<Color>(null)
   val colorFPicker: ColorPicker = colorF.right[0] as ColorPicker
   val custom1F: DTextField = grid.lookupId("custom1F")
   val custom2F: DTextField = grid.lookupId("custom2F")
   val custom3F: DTextField = grid.lookupId("custom3F")
   val custom4F: DTextField = grid.lookupId("custom4F")
   val custom5F: DTextField = grid.lookupId("custom5F")
   val playedFirstF: DTextField = grid.lookupId("playedFirstF")
   val playedLastF: DTextField = grid.lookupId("playedLastF")
   val addedToLibraryF: DTextField = grid.lookupId("addedToLibraryF")
   val tagsF: TagTextField<String> = grid.lookupId("tagsF")
   val moodF: MoodItemNode = grid.lookupId("moodF")
   val lyricsA: TextArea = scrollContent.lookupId("lyricsA")
   val infoL: Label = root.lookupId("infoL")
   val placeholder: StackPane = subroot.lookupId("placeholder")
   val allSongs = observableArrayList<Song>()!!
   val metadatas = mutableListOf<Metadata>()   // active in gui
   val fields: List<TagField<*,*>>
   var writing = false    // prevents external data change during writing
   var readingAddMode = false
   val validators = mutableListOf<Validation>()

   val coverField = object: TagField<ArtworkCover?, File?>(COVER, false) {
      val coverContainer: StackPane = scrollContent.lookupId("coverContainer")
      val coverDescriptionL: Label = scrollContent.lookupId("coverDescriptionL")
      val coverLoadingId = AtomicLong(0)
      override var outputValue: File? = null

      init {
         coverV.onFileDropped = { it ui ::outputValueTo }
         coverV.setContextMenuOn(false)
         coverV.pane.onEventDown(MOUSE_CLICKED, SECONDARY) {
            ContextMenu().apply {
               dsl {
                  item("Add new cover") { coverV.doSelectFile() }
                  if (canBeRemoved) item("Remove cover") { outputValueTo(null) }
                  if (canBeReverted) item("Keep original cover") { outputValueToOriginal() }
                  separator()
               }
               items += CoreMenus.menuItemBuilders[coverV.ContextMenuData()]
               show(coverV.pane, it)
            }
         }
         coverV.pane.onEventDown(DRAG_DETECTED) { coverV.pane.startFullDrag() }
         root.onEventUp(MOUSE_DRAG_RELEASED) {
            if (it.gestureSource==coverV.pane && !coverV.pane.containsMouse(it)) {
               outputValueTo(null)
            }
         }
      }

      override fun clearContent() {
         coverV.loadImage(null)
         coverContainer.isDisable = true
         outputValue = null
         committable = false
      }

      override fun setEditable(v: Boolean) {
         coverContainer.isDisable = readOnly || !v
      }

      override fun complete() {
         val s = state
         outputValue = null
         coverV.loadCover(
            when (s) {
               is ReadState.Same -> s.value
               else -> null
            }
         )
         coverDescriptionL.text = when (s) {
            is ReadState.None -> ""
            is ReadState.Same -> s.value?.description ?: ""
            is ReadState.Multi -> AppTexts.textManyVal
            is ReadState.Init -> fail()
         }
      }

      private val canBeRemoved: Boolean
         get() = (!committable && state !is ReadState.None) || (committable && outputValue!=null)

      private val canBeReverted: Boolean
         get() = committable

      private fun outputValueToOriginal() {
         complete()
         committable = false
      }

      private fun outputValueTo(newCover: File?) {
         if (isEmpty) return

         val f = newCover?.takeIf { it.isImageJaudiotagger() }
         outputValue = f
         committable = true

         if (f==null) {
            coverV.loadImage(null)
            coverDescriptionL.text = ""
         } else {
            coverV.loadFile(f)
            coverDescriptionL.text = "${f.mimeType().name} computing size..."
            val clId = coverLoadingId.getAndIncrement()
            runIO { getImageDim(f) } ui {
               if (clId == coverLoadingId.get() && outputValue==f)
                  coverDescriptionL.text = f.mimeType().name + it.map { " ${it.width}x${it.height}" }.getOr("")
            }
         }
      }
   }
   val tagFieldField = Metadata.Field(type(), { it.getTags() }, { o, or -> o ?: or }, TAGS.name(), TAGS.description())
   val tagField = object: TagField<String?, Set<String>>(tagFieldField, false) {
      private val originalItems = mutableSetOf<String>()
      private val textTag = object: TextTagField<String?>(tagFieldField, tagsF.textField, false, uiConverter = { "" }) {
         override fun handleTextChange() = Unit
         override fun handleLooseFocus() = Unit
         override fun handleMouseClicked() = Unit
         override fun handleBackspacePressed(e: KeyEvent) = Unit
      }

      init {
         tagsF.installDescribeOnHoverIn(descriptionL) { f.description() }
         tagsF.items.onChange {
            val committable = !readOnly && tagsF.items != originalItems
            tagsF.committable = committable
            tagsF.textField.committable = committable
         }
         tagsF.autocompleteSuggestionProvider.value = {
            autocompleteSuggestionsFor(TAGS, it, true)
         }
      }

      override val outputValue get() = tagsF.items.materialize()

      override fun clearContent() {
         textTag.clearContent()
         tagsF.isEditable.value = false
         tagsF.items.clear()
         originalItems.clear()
         tagsF.committable = false
         tagsF.textField.committable = false
      }

      override fun init() {
         super.init()
         textTag.init()
      }

      override fun accumulate(m: Metadata) {
         super.accumulate(m)
         textTag.accumulate(m)
      }

      override fun setEditable(v: Boolean) {
         textTag.setEditable(v)
         tagsF.isEditable.value = !readOnly && v
      }

      override fun complete() {
         val s = state
         textTag.complete()

         when (s) {
            is ReadState.None -> {
               originalItems setTo setOf()
               tagsF.items.clear()
               tagsF.textField.originalPromptText = AppTexts.textNoVal
            }
            is ReadState.Same<String?> -> {
               val items = s.value.orEmpty().splitNoEmpty(SEPARATOR_UNIT.toString()).toSet()
               originalItems setTo items
               tagsF.items setTo items
               tagsF.textField.originalPromptText = ""
            }
            is ReadState.Multi -> {
               originalItems setTo setOf()
               tagsF.items.clear()
               tagsF.textField.originalPromptText = AppTexts.textManyVal
            }
            is ReadState.Init -> failCase(s)
         }
         tagsF.textField.originalText = ""
         tagsF.textField.promptText = tagsF.textField.originalPromptText
      }
   }

   val isEmpty: Boolean
      get() = allSongs.isEmpty()

   val dragDroppedHandler = { e: DragEvent ->
      if (e.dragboard.hasAudio()) {
         e.isDropCompleted = true
         e.consume()

         readingAddMode = e.transferMode==COPY
         val songs = e.dragboard.getAudio()
         if (songs.isNotEmpty()) read(songs)
         readingAddMode = false
      }
   }

   init {
      root.minSize = 0 x 0
      root.prefSize = 650.emScaled x 700.emScaled
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()

      val isBetween0And1 = { it: String -> it.toDoubleOrNull()?.let { it in 0.0..1.0 } ?: false }
      val isPastYearS = { it: String -> it.toIntOrNull()?.let { it in 0..Year.now().value } ?: false }
      val isIntS = { it: String -> it.toIntOrNull()!=null }
      val isHexColor = { it: String -> it.startsWith("0x") && APP.converter.general.isValid<Color>(it) }

      fields = listOf(
         TextTagField(TITLE, titleF),
         TextTagField(ALBUM, albumF),
         TextTagField(ARTIST, artistF),
         TextTagField(ALBUM_ARTIST, albumArtistF),
         TextTagField(COMPOSER, composerF),
         TextTagField(PUBLISHER, publisherF),
         TextTagField(TRACK, trackF, valCond = isIntS),
         TextTagField(TRACKS_TOTAL, tracksTotalF, valCond = isIntS),
         TextTagField(DISC, discF, valCond = isIntS),
         TextTagField(DISCS_TOTAL, discsTotalF, valCond = isIntS),
         TextTagField(GENRE, genreF),
         TextTagField(CATEGORY, categoryF),
         TextTagField(YEAR, yearF, valCond = isPastYearS),
         TextTagField(RATING_RAW_OF, ratingF, readOnly = true),
         TextTagField(RATING, ratingPF, valCond = isBetween0And1, uiConverter = { it.padEnd(5, '0').substring(0, 5) }),
         TextTagField(PLAYCOUNT, playcountF, valCond = isIntS),
         TextTagField(COMMENT, commentF),
         TextTagField(MOOD, moodF),
         TextTagField(COLOR, colorF, valCond = isHexColor),
         TextTagField(CUSTOM1, custom1F),
         TextTagField(CUSTOM2, custom2F, readOnly = true),
         TextTagField(CUSTOM3, custom3F),
         TextTagField(CUSTOM4, custom4F),
         TextTagField(CUSTOM5, custom5F, readOnly = true),
         TextTagField(FIRST_PLAYED, playedFirstF, readOnly = true),
         TextTagField(LAST_PLAYED, playedLastF, readOnly = true),
         TextTagField(ADDED_TO_LIBRARY, addedToLibraryF, readOnly = true),
         tagField,
         TextTagField(LYRICS, lyricsA),
         coverField
      )

      // deselect text fields on click
      root.onEventDown(MOUSE_PRESSED) { root.requestFocus() }

      root.installDrag(
         IconFA.MUSIC,
         "Edit audio files",
         { it.dragboard.hasAudio() },
         { dragDroppedHandler(it) }
      )

      // init color TagField
      var colorUpdateLock = false
      colorFValue attach { colorF.text = if (it==null) "" else APP.converter.general.toS(it) }
      colorFValue sync { colorFPicker.pseudoClassChanged("empty", it==null) }
      colorFValue sync {
         colorUpdateLock = true
         colorFPicker.value = it ?: Color(0.0, 0.0, 0.0, 0.0)
         colorUpdateLock = false
      }
      colorFPicker.valueProperty() attach {
         if (!colorUpdateLock)
            colorFValue.value = it
      }
      colorF.textProperty() attach {
         if (it.isEmpty()) colorFValue.value = null
         if (isHexColor(it)) APP.converter.general.ofS<Color?>(it).ifOk { colorFValue.value = it }
      }

      // init rating TagField
      ratingPF.setOnKeyReleased { updateRatingRaw() }
      ratingPF.setOnMousePressed { updateRatingRaw() }

      read(inputValue.value)
   }

   private fun updateRatingRaw() = runTry {
      val valueMax = ratingF.promptText.split("/")[1].toInt()
      val value = ratingPF.text.toDouble()
      ratingF.promptText = "${(value*valueMax).toInt()}/$valueMax"
      ratingF.text = ratingF.promptText
   }

   /**
    * Reads metadata on the specified songs and fills the data for tagging.
    * If song is [Metadata], reading is skipped.
    */
   override fun read(songs: List<Song>) {
      inputValue.value = songs

      val unique = MapSet(songs) { it.uri }  // remove duplicates
      allSongs setTo unique
      if (readingAddMode) readAdd(unique, false) else readSet(unique)
   }

   fun readFromDisc(songsToRead: List<Song> = allSongs) = read(songsToRead.map { it.toSimple() })

   private fun readSet(songsToSet: Set<Song>) {
      metadatas.clear()
      if (songsToSet.isEmpty()) {
         showProgressReading()
         populate(listOf())
      } else {
         readAdd(songsToSet, true)
      }
   }

   private fun readAdd(added: Set<Song>, readAll: Boolean) {
      if (added.isEmpty()) return

      showProgressReading()

      val ready = ArrayList<Metadata>()
      val needsRead = ArrayList<Song>()
      added.asSequence()
         .filter { !it.isCorrupt() && it.isFileBased() && it.getFile()!!.isAudioEditable() }
         .forEach {
            if (!readAll && it is Metadata) ready.add(it)
            else needsRead.add(it)
         }

      // read metadata
      val task = Song.readTask(needsRead)
      AppProgress.start(task)
      task.setOnDone { ok, result ->
         if (ok) {
            val unique = MapSet<URI, Metadata> { it.uri }   // remove duplicates
            unique.addAll(metadatas)
            unique.addAll(ready)
            unique.addAll(result)

            metadatas setTo unique
            populate(metadatas.materialize())
         }
      }
      runIO(task)
   }

   private fun readRem(songsToRemove: Set<Song>) {
      if (songsToRemove.isEmpty()) return

      showProgressReading()

      val unique = MapSet(songsToRemove) { it.uri }
      metadatas.removeIf { it in unique }
      populate(metadatas.materialize())
   }

   /** Writes edited data to tag, reloads the data and refreshes gui. */
   fun write() {
      val error = validators.find { it.isInValid() }
      if (error!=null) {
         PopWindow().apply {
            content.value = Text(error.text)
            show(WINDOW_ACTIVE(CENTER))
         }
         return
      }

      writing = true
      showProgressWriting()

      metadatas.write(
         { w ->
            if (titleF.committable) w.setTitle(titleF.text)
            if (albumF.committable) w.setAlbum(albumF.text)
            if (artistF.committable) w.setArtist(artistF.text)
            if (albumArtistF.committable) w.setAlbum_artist(albumArtistF.text)
            if (composerF.committable) w.setComposer(composerF.text)
            if (publisherF.committable) w.setPublisher(publisherF.text)
            if (trackF.committable) w.setTrack(trackF.text)
            if (tracksTotalF.committable) w.setTracksTotal(tracksTotalF.text)
            if (discF.committable) w.setDisc(discF.text)
            if (discsTotalF.committable) w.setDiscsTotal(discsTotalF.text)
            if (genreF.committable) w.setGenre(genreF.text)
            if (categoryF.committable) w.setCategory(categoryF.text)
            if (yearF.committable) w.setYear(yearF.text)
            if (ratingPF.committable) w.setRatingPercent(ratingPF.text)
            if (playcountF.committable) w.setPlaycount(playcountF.text)
            if (commentF.committable) w.setComment(commentF.text)
            if (moodF.committable) w.setMood(moodF.text)
            if (colorF.committable) w.setColor(colorFValue.value)
            if (custom1F.committable) w.setCustom1(custom1F.text)
            // if (custom2F.committable) w.setCustom2(custom2F.text)
            if (custom3F.committable) w.setCustom3(custom3F.text)
            if (custom4F.committable) w.setCustom4(custom4F.text)
            // if (custom5F.committable) w.setCustom5(custom5F.text)
            if (tagsF.committable) w.setTags(tagField.outputValue)
            // if ((boolean)playedFirstF.getUserData())  w.setPla(playedFirstF.getText());
            // if ((boolean)playedLastF.getUserData())   w.setCustom1(playedLastF.getText());
            // if ((boolean)addedToLibF.getUserData())   w.setCustom1(addedToLibF.getText());
            if (lyricsA.committable) w.setLyrics(lyricsA.text)
            if (coverField.committable) w.setCover(coverField.outputValue)
         },
         { songs ->
            writing = false
            populate(songs)
            APP.plugins.use<Notifier> { it.showTextNotification(SONG_TAGGER_NAME, "Tagging complete") }
         }
      )
   }

   private fun populate(songsToPopulate: List<Metadata>) {
      if (writing) {
         hideProgress()
         return
      }

      val totallyEmpty = allSongs.isEmpty()
      content.isVisible = !totallyEmpty
      placeholder.isVisible = totallyEmpty
      if (totallyEmpty) return

      fields.forEach { it.clearContent() }

      if (songsToPopulate.isEmpty()) {
         infoL.text = "No songs loaded"
         infoL.graphic = null
         hideProgress()
      } else {
         infoL.text = "song".pluralUnit(songsToPopulate.size) + " loaded"
         infoL.graphic = createIcon(if (songsToPopulate.size==1) IconFA.TAG else IconFA.TAGS)

         fields.forEach { it.setEditable(true) }

         runIO {
            val formats = songsToPopulate.asSequence().map { it.getFormat() }.toSet()
            fields.forEach { it.init() }
            fields.forEach { f -> songsToPopulate.forEach { f.accumulate(it) } }
            formats
         } ui { formats ->
            fields.forEach { it.complete() }
            fields.forEach { it.setEditable(formats.all { it.isAudioEditable() }) }
            hideProgress()
         }
      }
   }

   private fun showProgressReading() {
      headerProgressI.progress = INDETERMINATE_PROGRESS
      headerProgressI.isVisible = true
      scrollContent.isMouseTransparent = true
      scrollContent.effect = BoxBlur(1.0, 1.0, 1)
      scrollContent.opacity = 0.8
   }

   private fun showProgressWriting() {
      headerProgressI.progress = INDETERMINATE_PROGRESS
      headerProgressI.isVisible = true
      scrollContent.isMouseTransparent = true
      scrollContent.effect = BoxBlur(1.0, 1.0, 1)
      scrollContent.opacity = 0.8
   }

   private fun hideProgress() {
      headerProgressI.progress = 0.0
      headerProgressI.isVisible = false
      scrollContent.isMouseTransparent = false
      scrollContent.effect = null
      scrollContent.opacity = 1.0
   }

   open inner class TextTagField<T>(
      f: Metadata.Field<T>,
      private val c: TextInputControl,
      readOnly: Boolean = false,
      valCond: Predicate? = null,
      private val uiConverter: Converter = { it }
   ): TagField<T, String>(f, readOnly) {

      override val outputValue: String
         get() = c.text

      init {
         c.minSize = 0 x 0
         c.prefSize = -1 x -1

         if (valCond!=null && c is DTextField) {
            val v = Validation(c, valCond, "$f field does not contain valid text.")
            validators += v
            val warnI = lazy { Icon(IconFA.EXCLAMATION_TRIANGLE, 11.0) }
            c.textProperty() attach {
               if (v.isValid()) c.right -= warnI.orNull()
               else c.right += warnI.value
            }
         }

         clearContent()

         c.focused attachFalse { handleLooseFocus() }
         c.textProperty() sync { handleTextChange() }

         // label for
         val cLabel = scrollContent.lookupId<Label>(c.descriptionNodeId.dropLast(1) + "L")
         cLabel.labelForWithClick setTo c

         // show description
         cLabel.installDescribeOnHoverIn(descriptionL) { f.description() }
         c.installDescribeOnHoverIn(descriptionL) { f.description() }

         // if not committable yet, enable committable & set text to tag value on click
         c.onEventDown(MOUSE_CLICKED, PRIMARY) { handleMouseClicked() }

         // disable committable if empty and backspace key pressed
         c.onEventUp(KEY_PRESSED) { e ->
            if (e.code==BACK_SPACE || e.code==ESCAPE)
               handleBackspacePressed(e)
         }

         // auto-completion
         if (c is TextField && f.isAutoCompletable()) {
            autoComplete(c) {
               autocompleteSuggestionsFor(f, it, true)
            }
         }
      }

      override fun setEditable(v: Boolean) {
         c.isEditable = !readOnly && v
      }

      final override fun clearContent() {
         c.text = ""
         c.promptText = ""
         c.originalText = ""
         c.originalPromptText = ""
         c.isEditable = false
      }

      protected open fun handleTextChange() {
         c.committable = !readOnly && c.text!=c.originalText
      }

      protected open fun handleLooseFocus() {
         if (c.committable)
            c.committable = c.text!=c.originalText
      }

      protected open fun handleMouseClicked() {
         if (!c.committable) {
            c.text = c.originalText
            c.committable = !readOnly && true
            if (!readOnly) c.selectAll()
         }
      }

      protected open fun handleBackspacePressed(e: KeyEvent) {
         val setToInitial = c.text.isEmpty()
         if (setToInitial) {
            c.committable = false

            if (c==ratingF) ratingPF.committable = false
            if (c==ratingPF) ratingF.committable = false

            e.consume()
         }
      }

      override fun complete() {
         val s = state

         if (f==COLOR) {
            colorFValue.value = s.asIf<ReadState.Same<Color?>>()?.value  // sets c.text, so override it below
            c.text = ""
         }

         c.originalText = when (s) {
            is ReadState.Same<T?> -> f.toS(s.value, "")
            else -> ""
         }
         c.originalPromptText = when (s) {
            is ReadState.None -> AppTexts.textNoVal
            is ReadState.Same<T?> -> f.toS(s.value, "")
            is ReadState.Multi -> AppTexts.textManyVal
            else -> failCase(s)
         }
         c.promptText = c.originalPromptText
      }
   }

   private fun showItemsPopup() {
      val list = listView<Song> {
         cellFactory = Callback {
            object: ListCell<Song>() {
               private var cb = CheckIcon()

               init {
                  // allow user to de/activate item
                  cb.setOnMouseClicked {
                     val song = item
                     // avoid nulls & respect lock
                     if (song!=null) {
                        if (cb.selected.value)
                           readAdd(setOf(song), false)
                        else
                           readRem(setOf(song))
                     }
                  }
               }

               public override fun updateItem(song: Song?, empty: Boolean) {
                  super.updateItem(song, empty)

                  if (empty || song==null) {
                     text = null
                     graphic = null
                  } else {
                     val index = index + 1
                     text = index.toString() + "   " + song.getFilenameFull()
                     val unTaggable = song.isCorrupt() || !song.isFileBased() || !song.getFile()!!.isAudioEditable()
                     pseudoClassChanged("corrupt", unTaggable)
                     cb.selected.value = !unTaggable
                     cb.isDisable = unTaggable

                     if (graphic==null) graphic = cb
                  }
               }
            }
         }
         items = allSongs
         addEventHandler(DRAG_OVER, handlerAccepting { it.dragboard.hasAudio() })
         addEventHandler(DRAG_DROPPED, dragDroppedHandler)
      }

      PopWindow().apply {
         content.value = list
         title.value = "Active Items"
         show(LEFT_CENTER(infoL))
      }
   }

   private fun buildLayout() = stackPane {
      minSize = 0 x 0

      lay += vBox(0.0, CENTER) {
         id = "content"
         minSize = 0 x 0

         lay += borderPane {
            id = "header"
            prefHeight = 25.0
            padding = Insets(0.0, 10.0, 0.0, 10.0)

            left = label("Summary") {
               id = "infoL"
               BorderPane.setAlignment(this, CENTER_LEFT)
               onEventDown(MOUSE_CLICKED) { showItemsPopup() }
               cursor = HAND
            }
            right = appProgressIndicator().apply {
               id = "headerProgressI"
               isVisible = false
            }
         }
         lay(ALWAYS) += scrollPane {
            id = "scrollRoot"
            isFitToWidth = true
            hbarPolicy = ScrollBarPolicy.NEVER

            content = scrollContent.apply {
               id = "scrollContent"
               padding = Insets(0.0, 10.0, 10.0, 10.0)

               lay(0, 210, null, 0) += gridPane {
                  id = "grid"

                  columnConstraints += gridPaneColumn {
                     isFillWidth = false
                     hgrow = NEVER
                     minWidth = Double.NEGATIVE_INFINITY
                  }
                  columnConstraints += gridPaneColumn {
                     hgrow = NEVER
                     percentWidth = 30.0
                  }

                  val rowHeight = v(0.0)
                  APP.ui.font sync { rowHeight.value = 2.em.emScaled } on onClose
                  repeat(25) {
                     rowConstraints += gridPaneRow {
                        isFillHeight = false
                        vgrow = NEVER
                        if (it!=13 && it!=24) prefHeightProperty() syncFrom rowHeight
                     }
                  }

                  listOf(
                     "Title", "Album", "Artist", "Album Artist", "Composer", "Publisher", "Track", "Disc", "Genre",
                     "Category", "Year", "Rating", "Playcount", "Comment", "Mood", "Color", "Custom 1", "Custom 2",
                     "Custom 3", "Custom 4", "Custom 5", "Played First", "Played Last", "Added to Library", "Tags"
                  ).forEachIndexed { i, text ->
                     lay(row = i, column = 0, hAlignment = HPos.RIGHT) += label(text) {
                        id = computeIdFromText()
                        padding = Insets(0.0, 10.0, 0.0, 0.0)
                     }
                  }

                  fun dTextField(block: DTextField.() -> Unit) = DTextField().apply {
                     minSize = 0 x 0
                     maxSize = Double.MAX_VALUE.x2
                     alignment = CENTER_LEFT
                     styleClass += "tag-field"
                     block()
                  }
                  fun layTextField(id: String, row: Int, column: Int = 1, block: DTextField.() -> Unit = {}) {
                     lay(row = row, column = column, colSpan = REMAINING, hAlignment = HPos.LEFT, vAlignment = VPos.CENTER) += dTextField {
                        this.id = id
                        block()
                     }
                  }

                  layTextField("titleF", 0)
                  layTextField("albumF", 1)
                  layTextField("artistF", 2)
                  layTextField("albumArtistF", 3)
                  layTextField("composerF", 4)
                  layTextField("publisherF", 5)
                  lay(row = 6, column = 1, colSpan = REMAINING) += gridPane {
                     minSize = 0 x 0
                     maxSize = Double.MAX_VALUE.x2
                     columnConstraints += listOf(
                        gridPaneColumn { hgrow = SOMETIMES },
                        gridPaneColumn { hgrow = SOMETIMES; minWidth = 10.0; prefWidth = 10.0; maxWidth = 10.0 },
                        gridPaneColumn { hgrow = SOMETIMES }
                     )
                     rowConstraints += gridPaneRow { vgrow = SOMETIMES; minHeight = 10.0; prefHeight = 30.0 }

                     lay(row = 0, column = 0, hAlignment = HPos.LEFT) += dTextField {
                        id = "trackF"
                        alignment = CENTER_LEFT
                     }
                     lay(row = 0, column = 1, hAlignment = HPos.CENTER) += label("/")
                     lay(row = 0, column = 2, hAlignment = HPos.LEFT) += dTextField {
                        id = "tracksTotalF"
                        descriptionNodeId = "trackF"
                        alignment = CENTER_LEFT
                     }
                  }
                  lay(row = 7, column = 1, colSpan = REMAINING) += gridPane {
                     minSize = 0 x 0
                     maxSize = Double.MAX_VALUE.x2
                     columnConstraints += listOf(
                        gridPaneColumn { hgrow = SOMETIMES },
                        gridPaneColumn { hgrow = SOMETIMES; minWidth = 10.0; prefWidth = 10.0; maxWidth = 10.0 },
                        gridPaneColumn { hgrow = SOMETIMES }
                     )
                     rowConstraints += gridPaneRow { vgrow = SOMETIMES; minHeight = 10.0; prefHeight = 30.0 }

                     lay(row = 0, column = 0, hAlignment = HPos.LEFT) += dTextField {
                        id = "discF"
                        alignment = CENTER_LEFT
                     }
                     lay(row = 0, column = 1, hAlignment = HPos.CENTER) += label("/")
                     lay(row = 0, column = 2, hAlignment = HPos.LEFT) += dTextField {
                        id = "discsTotalF"
                        descriptionNodeId = "discF"
                        alignment = CENTER_LEFT
                     }
                  }
                  layTextField("genreF", 8)
                  layTextField("categoryF", 9)
                  layTextField("yearF", 10)
                  layTextField("ratingF", 11, 1) {
                     prefColumnCount = 0
                  }
                  layTextField("ratingPF", 11, 2) {
                     descriptionNodeId = "ratingF"
                  }
                  layTextField("playcountF", 12)
                  lay(row = 13, column = 1, hAlignment = HPos.LEFT, colSpan = REMAINING) += textArea {
                     id = "commentF"
                     minSize = 0 x 0
                     maxSize = Double.MAX_VALUE.x2
                     styleClass += "tag-field"
                     isWrapText = true
                     textProperty() zip prefColumnCountProperty() sync { (txt, columns) ->
                        prefRowCount = (txt.orEmpty().lineSequence().sumOf { 1 max ceil(it.length.toDouble()/columns.toDouble()).roundToInt() } - 1).clip(0,6)
                     }
                  }
                  lay(row = 14, column = 1, hAlignment = HPos.LEFT, colSpan = REMAINING) += MoodItemNode().apply {
                     id = "moodF"
                     minSize = 0 x 0
                     maxSize = Double.MAX_VALUE.x2
                     styleClass += "tag-field"
                  }
                  lay(row = 15, column = 1, hAlignment = HPos.RIGHT, colSpan = REMAINING) += stackPane {
                     minSize = 0 x 0
                     maxSize = Double.MAX_VALUE.x2

                     lay += dTextField {
                        id = "colorF"
                        styleClass += "value-text-field"
                        styleClass += "color-text-field"
                        minSize = 0 x 0
                        maxSize = Double.MAX_VALUE.x2
                        alignment = CENTER_LEFT

                        right += ColorPicker().apply {
                           id = "colorFPicker"
                           minPrefMaxWidth = 40.emScaled
                        }
                     }
                  }
                  layTextField("custom1F", 16)
                  layTextField("custom2F", 17)
                  layTextField("custom3F", 18)
                  layTextField("custom4F", 19)
                  layTextField("custom5F", 20)
                  layTextField("playedFirstF", 21)
                  layTextField("playedLastF", 22)
                  layTextField("addedToLibraryF", 23)
                  lay += label()
                  lay(row = 24, column = 1, colSpan = REMAINING, hAlignment = HPos.LEFT, vAlignment = VPos.CENTER) += StringTagTextField().apply {
                     id = "tagsF"
                     styleClass += "tag-field"
                     textField.descriptionNodeId = "tagsF"
                  }
               }
               lay(0, 0, null, null) += vBox(5, CENTER) {
                  installDescribeOnHoverIn(descriptionL) { COVER.description() }

                  lay += label("Cover") {
                     id = computeIdFromText()
                  }
                  lay += stackPane {
                     id = "coverContainer"

                     lay += label(AppTexts.textNoVal) {
                        id = "coverEmptyL"
                     }
                     lay += borderPane {
                        prefSize = 200.emScaled x 200.emScaled

                        center = coverV.apply {
                           onHighlight = { parent.parent.lookupId<Label>("coverEmptyL").isVisible = !it }
                           view.fitHeight = 200.emScaled
                           view.fitWidth = 200.emScaled
                        }.pane.apply {
                           prefSize = 200.emScaled x 200.emScaled
                           maxSize = prefSize
                           style = """
                                -fx-border-insets: -2;
                                -fx-border-color: rgba(255,255,255, 0.2);
                                -fx-border-style: segments(10, 15, 15, 15);
                                -fx-border-radius: 0;
                                -fx-border-width: 2;
                           """.trimIndent()
                        }
                     }
                  }
                  lay += label("Cover description") {
                     id = "coverDescriptionL"
                  }
               }
               lay(255.emScaled + 5, 0, 0, null) += vBox(5, CENTER) {
                  prefSize = 200.emScaled x -1

                  lay += label("Lyrics") {
                     id = computeIdFromText()
                  }
                  lay(ALWAYS) += textArea {
                     id = "lyricsA"
                     prefSize = 200.emScaled x -1
                     isWrapText = true
                     textAlignment = TextAlignment.CENTER
                     styleClass += "tag-field"
                  }
               }
            }
         }
         lay += hBox(10.0, CENTER) {
            lay += formIcon(IconFA.REFRESH, "Read") {
               readFromDisc()
            }.apply {
               installDescribeOnHoverIn(descriptionL) {
                  "Reads all fields of the songs from disk, also removing any unsaved values"
               }
            }
            lay += formIcon(IconFA.CHECK, "Write") {
               write()
            }.apply {
               installDescribeOnHoverIn(descriptionL) {
                  "Writes all unsaved values to tags of the songs"
               }
            }
         }
         lay += descriptionL.apply {
            id = "descriptionL"
            padding = Insets(10.emScaled, 0.0, 0.0, 0.0)
         }
      }
      lay += stackPane {
         id = "placeholder"

         lay += label("Drag & drop songs to edit")
      }
   }

   companion object: WidgetCompanion {
      override val name = SONG_TAGGER_NAME
      override val description = "Song tag editor"
      override val descriptionLong = "Tag editor for audio files. Supports reading and writing. Taggable songs can be unselected in selective list mode."
      override val icon = IconMD.BOOKMARK_MUSIC
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2015)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(AUDIO, LIBRARY)
      override val summaryActions = listOf(
         ShortcutPane.Entry("Tagger > Cover", "Remove", "Drag cover away"),
         ShortcutPane.Entry("Tagger > Cover", "Add", "Drag & drop image file"),
         ShortcutPane.Entry("Tagger > Song list", "Set", "Drag & drop songs"),
         ShortcutPane.Entry("Tagger > Song list", "Edit", keys("Songs icon +${PRIMARY.nameUi}")),
         ShortcutPane.Entry("Tagger > Song list", "Add", keys("Drag & drop songs+SHIFT")),
      )

      private val RATING_RAW_OF = Metadata.Field(type(), { "${it.getRating() ?: AppTexts.textNoVal}/${it.getRatingMax()}" }, { o, or -> o ?: or }, "Rating (raw)", "Song rating value in tag. Maximal value depends on tag type")

      private fun Label.computeIdFromText() = text.split(" ").joinToString("") { it.capital() }.decapital() + "L"

      private fun Node.installDescribeOnHoverIn(label: Label, text: () -> String) {
         onEventUp(MOUSE_ENTERED) { label.text = text() }
         onEventUp(MOUSE_EXITED) { label.text = "" }
      }

      private var Node.committable: Boolean
         get() {
            return properties["committable"] as Boolean? ?: false
         }
         set(value) {
            pseudoClassChanged("committable", value)
            properties["committable"] = value
            if (this is TextInputControl) promptText = if (value) "" else originalPromptText
         }

      private var TextInputControl.originalText: String by property("promptTextTmp") { "" }
      private var TextInputControl.originalPromptText: String by property("originalPromptText") { "" }
      private var Node.descriptionNodeId: String by property("descriptionNodeId") { id }
   }

   abstract class TagField<I, O>(val f: Metadata.Field<I>, val readOnly: Boolean = false) {
      var state: ReadState<I?> by atomic(ReadState.Init)
      var committable = false
      abstract val outputValue: O

      abstract fun clearContent()

      abstract fun setEditable(v: Boolean)

      open fun init() {
         state = ReadState.Init
      }

      open fun accumulate(m: Metadata) {
         val s = state

         if (s is ReadState.Init)
            state = if (f.isFieldEmpty(m)) ReadState.None else ReadState.Same(f.getOf(m))

         if (s is ReadState.None && !f.isFieldEmpty(m))
            state = ReadState.Multi

         if (s is ReadState.Same<I?> && s.value!=f.getOf(m))
            state = ReadState.Multi
      }

      abstract fun complete()
   }

   sealed class ReadState<out T> {
      /** Initial state */
      object Init: ReadState<Nothing>()
      /** No value in all songs */
      object None: ReadState<Nothing>()
      /** Same value in all songs */
      data class Same<T>(val value: T): ReadState<T>()
      /** Multiple value in all songs */
      object Multi: ReadState<Nothing?>()
   }

   class Validation(val field: TextInputControl, val condition: Predicate, val text: String) {

      fun isValid(): Boolean = field.text.let { it.isNullOrEmpty() || condition(it) }

      fun isInValid(): Boolean = !isValid()

   }

}