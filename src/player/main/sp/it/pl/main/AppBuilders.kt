package sp.it.pl.main

import de.jensd.fx.glyphs.GlyphIcons
import java.io.File
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.atomic.AtomicLong
import javafx.animation.ParallelTransition
import javafx.beans.property.Property
import javafx.event.EventHandler
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.TOP_LEFT
import javafx.geometry.Side
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.ContentDisplay
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.OverrunStyle.LEADING_ELLIPSIS
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS
import javafx.scene.control.ScrollPane
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
import javafx.scene.control.Tooltip
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.layout.Priority.NEVER
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import javafx.scene.text.TextBoundsType
import javafx.util.Callback
import javafx.util.Duration
import javafx.util.Duration.ZERO
import kotlin.math.sqrt
import kotlin.reflect.KClass
import kotlin.reflect.KTypeProjection.Companion.invariant
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.jvmName
import kotlin.streams.asSequence
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.ui.LabelWithIcon
import sp.it.pl.ui.objects.SpitText
import sp.it.pl.ui.objects.contextmenu.ValueContextMenu
import sp.it.pl.ui.objects.form.Form.Companion.form
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.spinner.Spinner
import sp.it.pl.ui.objects.table.FieldedTable.UNCONSTRAINED_RESIZE_POLICY_FIELDED
import sp.it.pl.ui.objects.table.FilteredTable
import sp.it.pl.ui.objects.table.buildFieldedCell
import sp.it.pl.ui.objects.tablerow.SpitTableRow
import sp.it.pl.ui.objects.textfield.SpitTextField
import sp.it.pl.ui.objects.window.NodeShow.RIGHT_CENTER
import sp.it.pl.ui.objects.window.ShowArea.WINDOW_ACTIVE
import sp.it.pl.ui.objects.window.Shower
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.pl.ui.objects.window.popup.PopWindow.Companion.popWindow
import sp.it.pl.ui.objects.window.stage.Window
import sp.it.pl.ui.pane.ConfigPane
import sp.it.util.Na
import sp.it.util.Named
import sp.it.util.access.fieldvalue.ColumnField.INDEX
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.access.fieldvalue.ObjectFieldBase
import sp.it.util.access.fieldvalue.ObjectFieldOfDataClass
import sp.it.util.access.toggle
import sp.it.util.access.toggleNext
import sp.it.util.access.vAlways
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.Anim.Companion.animPar
import sp.it.util.animation.Anim.Interpolators.Companion.geomCircular
import sp.it.util.animation.Anim.Interpolators.Companion.geomElastic
import sp.it.util.animation.Anim.Interpolators.Companion.sym
import sp.it.util.async.FX
import sp.it.util.async.IO
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.future.Fut
import sp.it.util.collections.collectionUnwrap
import sp.it.util.collections.getElementType
import sp.it.util.collections.materialize
import sp.it.util.collections.setToOne
import sp.it.util.conf.Config
import sp.it.util.conf.Configurable
import sp.it.util.conf.Constraint.RepeatableAction
import sp.it.util.conf.ValueConfig
import sp.it.util.conf.nonEmpty
import sp.it.util.dev.Dsl
import sp.it.util.file.FileType
import sp.it.util.file.hasExtension
import sp.it.util.file.toFileOrNull
import sp.it.util.file.toURIOrNull
import sp.it.util.file.type.MimeGroup
import sp.it.util.file.type.MimeType
import sp.it.util.file.type.mimeType
import sp.it.util.functional.Option
import sp.it.util.functional.Try
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.math.rangeBigDec
import sp.it.util.math.rangeBigInt
import sp.it.util.named
import sp.it.util.reactive.Unsubscriber
import sp.it.util.reactive.asDisposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachChanges
import sp.it.util.reactive.attachTrue
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncBiFrom
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.syncTo
import sp.it.util.system.browse
import sp.it.util.text.Char16
import sp.it.util.text.Char32
import sp.it.util.text.Jwt
import sp.it.util.text.char32At
import sp.it.util.text.graphemeAt
import sp.it.util.text.lengthInChars
import sp.it.util.text.lengthInCodePoints
import sp.it.util.text.lengthInGraphemes
import sp.it.util.text.toChar32
import sp.it.util.type.VType
import sp.it.util.type.argOf
import sp.it.util.type.createTypeStar
import sp.it.util.type.dataComponentProperties
import sp.it.util.type.isDataClass
import sp.it.util.type.isRecordClass
import sp.it.util.type.isSubtypeOf
import sp.it.util.type.isValueClass
import sp.it.util.type.kTypeNothingNonNull
import sp.it.util.type.raw
import sp.it.util.type.type
import sp.it.util.type.typeOrAny
import sp.it.util.ui.button
import sp.it.util.ui.hBox
import sp.it.util.ui.hyperlink
import sp.it.util.ui.install
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.lookupSiblingUp
import sp.it.util.ui.scrollText
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.setScaleXYByTo
import sp.it.util.ui.show
import sp.it.util.ui.stackPane
import sp.it.util.ui.tableColumn
import sp.it.util.ui.text
import sp.it.util.ui.vBox
import sp.it.util.units.FileSize
import sp.it.util.units.div
import sp.it.util.units.durationOfHMSMs
import sp.it.util.units.em
import sp.it.util.units.millis
import sp.it.util.units.plus
import sp.it.util.units.seconds
import sp.it.util.units.times
import sp.it.util.units.uri

/**
 * Creates simple help popup designed as a tooltip for help buttons.
 *
 * The popup is
 * * not detached
 * * not detachable
 * * hide on click true,
 * * autohide true
 * * not userResizable
 *
 * Tip: Associate help popups with buttons marked with question mark or similar icon.
 */
@JvmOverloads
fun helpPopup(textContent: String, textTitle: String = "Help"): PopWindow = helpPopup(
   scrollText {
      SpitText(textContent).apply {
         styleClass += "help-pop-window-text"
         wrappingWithNatural.subscribe()
      }
   },
   textTitle
)

@JvmOverloads
fun helpPopup(contentNode: Node, textTitle: String = "Help"): PopWindow = popWindow {
   styleClass += "help-pop-window"
   content.value = contentNode
   title.value = textTitle
   isAutohide.value = true
   isClickHide.value = true
   userResizable.value = true // content may not always be static and right size, though ideally we would use false
   focusOnShow.value = false
}

/** @return standardized icon that opens a help popup with the specified text (eager) */
fun infoIcon(tooltipText: String) = infoIcon { tooltipText }

/** @return standardized icon that opens a help popup with the specified text (lazy)  */
fun infoIcon(tooltipText: () -> String): Icon = Icon(IconOC.QUESTION)
   .tooltip("Help\n\nClick the button to see additional information")
   .action { i ->
      APP.actionStream("Info popup")
      helpPopup(tooltipText()).apply {
         content.value.asIs<ScrollPane>().content.asIs<SpitText>().wrappingWidth = 400.emScaled
         headerIconsVisible.value = false
         show(RIGHT_CENTER(i))
      }
   }

/** @return standardized icon that opens a help popup with the specified text (lazy)  */
fun infoIconWith(content: () -> Node): Icon = Icon(IconOC.QUESTION)
   .tooltip("Help\n\nClick the button to see additional information")
   .action { i ->
      APP.actionStream("Info popup")
      helpPopup(content()).apply {
         headerIconsVisible.value = false
         show(RIGHT_CENTER(i))
      }
   }

/** @return standardized icon associated with a form that invokes an action */
fun formIcon(icon: GlyphIcons, text: String, action: () -> Unit) = Icon(icon, 25.0).run {
   action(action)
   withText(Side.RIGHT, text)
}

/** @return standardized on-top icon associated with the specified window */
fun windowOnTopIcon(window: Window) = Icon().apply {
   styleclass("header-icon")
   styleclass("window-top-icon")
   tooltip("On top\n\nWindow will stay in foreground when other window is being interacted with")
   onClickDo { window.alwaysOnTop.toggle() }
   window.alwaysOnTop sync { icon(it, IconFA.SQUARE, IconFA.SQUARE_ALT) }
}

fun windowPinIcon(autohide: Property<Boolean>) = CheckIcon(autohide).apply {
   isFocusTraversable = false
   styleclass("header-icon")
   styleclass("window-pin-icon")
   tooltip("Pin\n\nWindow will not close when other window is being interacted with")
   icons(IconMD.PIN)
}

fun formEditorsUiToggleIcon(mode: Property<ConfigPane.Layout>) = Icon(IconMD.WRAP).apply {
   onClickDo { mode.toggleNext() }
   tooltip("Editor layout\n\nChanges layout and visibility of descriptions. Has effect across application.")
}

data class BulletBuilder(val text: String, val descriptionLabel: Label, var isReadOnly: Boolean = false, var onClick: () -> Unit = {}, var description: String? = null)

inline fun bullet(text: String, descriptionLabel: Label, block: @Dsl BulletBuilder.() -> Unit = {}) = hBox(1.em.emScaled, TOP_LEFT) {
   val bb = BulletBuilder(text, descriptionLabel).apply(block)
   lay += hBox(1.em.emScaled, CENTER_LEFT) {
      lay += CheckIcon(true).apply {
         editable.value = false
         opacity = 0.0
      }
      lay += label(text) {
         isDisable = bb.isReadOnly
         isFocusTraversable = true
         isWrapText = true
         onEventDown(KEY_PRESSED, ENTER) { bb.onClick() }
         onEventDown(MOUSE_CLICKED, PRIMARY) { bb.onClick() }
         onEventUp(MOUSE_ENTERED) { requestFocus() }
         focusedProperty() attach { lookupSiblingUp<Node>(1).opacity = if (it) 1.0 else 0.0 }
         focusedProperty() attachTrue { bb.descriptionLabel.text = bb.description.orEmpty() }
      }
   }
}

/** @return standardized hyperlink for a [File], that [File.browse]s it on click */
fun appHyperlinkFor(f: File) = hyperlink(f.toUi()) {
   textOverrun = LEADING_ELLIPSIS
   onEventDown(MOUSE_CLICKED, PRIMARY) {
      if (it.clickCount==1)
         f.browse()
   }
}

/** @return standardized hyperlink for a [File], that [File.browse]s it on click */
fun appHyperlinkFor(u: URI) = hyperlink(u.toUi()) {
   textOverrun = LEADING_ELLIPSIS
   onEventDown(MOUSE_CLICKED, PRIMARY) {
      if (it.clickCount==1)
         u.browse()
   }
}

/** @return standardized ui text for the specified data displaying it in the most natural ui form */
fun textColon(named: Named): Node = textColon(named.name, named.value)

/** @return standardized ui text for the specified data displaying it in the most natural ui form */
fun textColon(name: String, data: Any?): Node = when (data) {
   null -> text("$name: ${data.toUi()}")
   is Na -> text("$name: n/a")
   is Path -> textColon(name, data.toFileOrNull() ?: data.toUi())
   is URL -> data.toURIOrNull()?.net { textColon(name, it) } ?: textColon(name, data.toUi())
   is URI -> data.toFileOrNull()?.net { textColon(name, it) } ?: hBox {
      lay(NEVER) += text("$name: ")
      lay += appHyperlinkFor(data)
   }
   is File -> hBox(0.0, CENTER_LEFT) {
      lay(NEVER) += text("$name: ")
      lay += appHyperlinkFor(data)
   }
   is Try.Ok<*> -> textColon(name, data.value)
   is Try.Error<*> -> label("$name: ") {
      contentDisplay = ContentDisplay.RIGHT
      graphic = errorIcon(data.value)
   }
   is Fut<*> -> stackPane {
      lay(CENTER_LEFT) += Spinner(INDETERMINATE_PROGRESS)
      data.onDone(FX) {
         lay.clear()
         lay += textColon(name, it.toTryRaw())
      }
   }
   is FutVal<*> -> stackPane {
      lay(CENTER_LEFT) += hBox(0.0, CENTER_LEFT) {
         lay(NEVER) += text("$name: ")
         lay += hyperlink("Compute...") {
            onEventDown(MOUSE_CLICKED, PRIMARY) {
               data.value().onDone(FX) {
                  this@stackPane.lay.clear()
                  this@stackPane.lay(CENTER_LEFT) += textColon(name, it.toTryRaw())
               }
            }
         }
      }
   }
   else -> text(name + ": " + data.toUi())
}

data class FutVal<T>(val value: () -> Fut<T>)

fun appProgressIcon(disposer: Unsubscriber): Node {
   var taskList: PopWindow? = null
   fun Node.toggleTaskList() {
      if (taskList==null) {
         taskList = AppProgress.showTasks(this).apply {
            onHiding += { taskList = null }
         }
      } else {
         taskList?.hide()
      }
   }

   fun Node.installToggleTaskListOnMouseClicked() = onEventDown(MOUSE_CLICKED, PRIMARY) { toggleTaskList() }

   val pB = Icon(IconFA.CIRCLE).apply {
      isFocusTraversable = false
      styleclass("header-icon")
      install(appTooltip("Progress & Tasks"))
      installToggleTaskListOnMouseClicked()

      val a = anim { setScaleXY(sqrt(0.2*it)) }.dur(250.millis).applyNow()
      AppProgress.activeTaskCount sync { a.playFromDir(it==0) } on disposer
   }
   val pI = appProgressIndicator().apply {
      isFocusTraversable = false
      install(appTooltip("Progress & Tasks"))
      installToggleTaskListOnMouseClicked()

      AppProgress.progress sync { progress = it } on disposer
   }
   val pL = appProgressIndicatorTitle(pI).apply {
      isFocusTraversable = false
      installToggleTaskListOnMouseClicked()

      AppProgress.activeTaskCount sync { if (it>0) text = "$it running tasks..." } on disposer
   }

   return hBox(0.0, CENTER_LEFT) {
      lay += stackPane {
         lay += pB
         lay += pI
      }
      lay += pL
   }
}

/** @return standardized progress indicator with start/finish animation and start/finish actions */
@JvmOverloads
fun appProgressIndicator(onStart: (ProgressIndicator) -> Unit = {}, onFinish: (ProgressIndicator) -> Unit = {}) = Spinner().apply {
   val a = anim { setScaleXY(sqrt(it)) }.dur(500.millis).intpl(geomElastic()).applyNow()
   progressProperty() attachChanges { ov, nv ->
      if (ov.toDouble()==1.0) {
         onStart(this)
         a.playOpenDo(null)
      }
      if (nv.toDouble()==1.0 || nv.toDouble().isNaN()) {
         a.playCloseDo { onFinish(this) }
      }
   }
}

/** @return standardized progress indicator label with start/finish animation */
fun appProgressIndicatorTitle(progressIndicator: ProgressIndicator) = hyperlink {
   progressIndicator.scaleXProperty() syncTo scaleXProperty()
   progressIndicator.scaleYProperty() syncTo scaleYProperty()
}

@JvmOverloads
fun appTooltip(text: String = "") = Tooltip(text).apply {
   isHideOnEscape = true
   consumeAutoHidingEvents = true
   showDelay = 1.seconds
   showDuration = 10.seconds
   hideDelay = 200.0.millis
}

fun appTooltipForData(data: () -> Any?) = appTooltip().apply {
   val text = text()
   graphic = text
   onShowing = EventHandler {
      computeDataInfo(data()) ui { text.text = it }
   }
}

/** @return future with information inspected by [App.instanceInfo] about the specified data on [IO] executor */
fun computeDataInfo(data: Any?): Fut<String> = (data as? Fut<*> ?: Fut.fut(data)).then(IO) {
   fun Any?.stringUnwrap(): Any? = if (this is String && lengthInGraphemes==1) graphemeAt(0) else this

   val d = collectionUnwrap(it).stringUnwrap()
   val dName = APP.instanceName[d].net {
      val n = 40
      val suffix = " (first $n characters)"
      val firstN = it.lineSequence().flatMap { it.codePoints().asSequence().plus(' '.toChar32().value) }.take(n + 1 + suffix.length).toList()
      if (firstN.size <= n+suffix.length) it else firstN.take(n).joinToString("") { it.toChar32().toString() } + suffix
   }
   val dClass = when (d) {
      null -> Nothing::class
      else -> d::class
   }
   val dType = when (d) {
      null -> kTypeNothingNonNull()
      is List<*> -> List::class.createType(arguments = listOf(invariant(d.getElementType())))
      is Set<*> -> Set::class.createType(arguments = listOf(invariant(d.getElementType())))
      is Map<*, *> -> Map::class.createType(arguments = listOf(invariant(d.keys.getElementType()), invariant(d.values.getElementType())))
      else -> d::class.createTypeStar()
   }
   val dKind = "\nType: ${dType.toUi()}"
   val dKindDev = "\nType (exact): ${dClass.qualifiedName ?: dClass.jvmName}".takeIf { APP.developerMode.value }.orEmpty()
   val dInfo = APP.instanceInfo[d]
      .map { "${it.name}: ${it.value.toUi().replace("\n", "  \n")}" }
      .sorted()
      .joinToString("\n")
      .takeUnless { it.isEmpty() }
      ?.let { "\n$it" } ?: ""

   "Data: $dName$dKind$dKindDev$dInfo"
}
/** @return future with information inspected by [App.instanceInfo] about the specified data on [IO] executor */
fun computeDataInfoUi(data: Any?): Fut<List<Named>> = (data as? Fut<*> ?: Fut.fut(data)).then(IO) {
   fun Any?.stringUnwrap(): Any? = if (this is String && lengthInGraphemes==1) graphemeAt(0) else this

   val d = collectionUnwrap(it).stringUnwrap()
   val dName = APP.instanceName[d].net {
      val n = 40
      val suffix = " (first $n characters)"
      val firstN = it.lineSequence().flatMap { it.codePoints().asSequence().plus(' '.toChar32().value) }.take(n + 1 + suffix.length).toList()
      if (firstN.size <= n+suffix.length) it else firstN.take(n).joinToString("") { it.toChar32().toString() } + suffix
   }
   val dClass = when (d) {
      null -> Nothing::class
      else -> d::class
   }
   val dType = when (d) {
      null -> kTypeNothingNonNull()
      is List<*> -> List::class.createType(arguments = listOf(invariant(d.getElementType())))
      is Set<*> -> Set::class.createType(arguments = listOf(invariant(d.getElementType())))
      is Map<*, *> -> Map::class.createType(arguments = listOf(invariant(d.keys.getElementType()), invariant(d.values.getElementType())))
      else -> d::class.createTypeStar()
   }

   buildList {
      add("Data" named dName)
      add("Type" named dType)
      if (!APP.developerMode.value) add("Type (exact)" named (dClass.qualifiedName ?: dClass.jvmName))
      addAll(APP.instanceInfo[d].sortedBy { it.name })
   }
}

fun contextMenuFor(o: Any?): ContextMenu = ValueContextMenu<Any?>().apply { setItemsFor(o) }

fun <T: Any> tableViewForClassJava(type: KClass<T>, block: FilteredTable<T>.() -> Unit = {}): FilteredTable<T> = tableViewForClass<Any>(type.asIs(), block.asIs()).asIs()

inline fun <reified T: Any> tableViewForClass(type: KClass<T> = T::class, block: FilteredTable<T>.() -> Unit = {}): FilteredTable<T> = object: FilteredTable<T>(type.java, null) {
   override fun computeMainField(field: ObjectField<T, *>?) =
      field ?: fields.firstOrNull { it.type.isSubtypeOf<String>() } ?: fields.firstOrNull()

   override fun computeFieldsAll() =
      (computeFieldsAllRecursively(type) ?: APP.classFields[type].toList()).plus(INDEX)

   val typeExact = type<T>()

   private fun <T: Any> computeFieldsAllRecursively(type: KClass<T>): List<ObjectField<T, *>>? =
      when {
         type == Map.Entry::class ->
            listOf<ObjectField<Map.Entry<Any?,Any?>, Any?>>(
               object: ObjectFieldBase<Map.Entry<Any?,Any?>, Any?>(VType<Any?>(typeExact.type.argOf(Map.Entry::class, 0).typeOrAny), { it -> it.key }, "Key", "Key", { it, or -> it?.toUi() ?: or }) {},
               object: ObjectFieldBase<Map.Entry<Any?,Any?>, Any?>(VType<Any?>(typeExact.type.argOf(Map.Entry::class, 0).typeOrAny), { it -> it.value }, "Value", "Value", { it, or -> it?.toUi() ?: or }) {}
            ).asIs()
         type.isDataClass || type.isRecordClass -> {
            val properties = type.dataComponentProperties()
            if (properties.size==1)
               // data class designed as value class must not have subfields
               null
            else
               properties.map { ObjectFieldOfDataClass(it) { it.toUi() } }
                  .flatMap { f -> computeFieldsAllRecursively(f.type.raw)?.map { f.flatMap(it.asIs()) } ?: listOf(f) }
                  .asIs()
         }
         // value class !have subfields
         type.isValueClass ->
            null
         // ordinary class !have subfields
         else ->
            null
      }
}.apply {
   selectionModel.selectionMode = SelectionMode.MULTIPLE
   rowFactory = Callback {
      SpitTableRow<T>().apply {
         // right click -> show context menu
         onRightSingleClick { r, e ->
            if (!r.isSelected) selectionModel.clearAndSelect(r.index)   // prep selection for context menu
            contextMenuFor(r.tableView.selectionModel.selectedItems.materialize()).show(r.tableView, e)
         }
      }
   }
   setColumnFactory { f ->
      tableColumn<T, Any?> {
         text = f.cName()
         styleClass += if (f.type.isSubtypeOf<String>()) "column-header-align-left" else "column-header-align-right"
         setCellValueFactory { cf -> if (cf.value==null) null else vAlways(f.getOf(cf.value)) }
         setCellFactory { f.buildFieldedCell() }
         userData = f
         isResizable = true
      }
   }
   columnResizePolicy = if (fields.any { it===INDEX }) UNCONSTRAINED_RESIZE_POLICY_FIELDED else CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
   columnState = defaultColumnInfo
   block()
//   sceneProperty().flatMap { it.windowProperty() }.syncNonNullWhile { w -> w.onIsShowing1st { autoResizeColumns() } }
}

fun resizeIcon(): Icon = Icon(IconMD.RESIZE_BOTTOM_RIGHT).apply {
   cursor = Cursor.SE_RESIZE
   isAnimated.value = false
   isFocusTraversable = false
   styleclass("resize-content-icon")
}

fun fileIcon(file: File, type: FileType): GlyphIcons = when (type) {
   FileType.DIRECTORY -> when {
      APP.location.skins==file.parentFile -> IconFA.PAINT_BRUSH
      APP.location.widgets==file.parentFile -> IconFA.GE
      file.isAbsolute && file.name.isEmpty() -> IconMD.HARDDISK
      else -> IconUN(0x1f4c1)
   }
   FileType.FILE -> when {
      file hasExtension "css" -> IconFA.CSS3
      file.isWidgetFile() -> IconFA.GE
      else -> when (val mime = file.mimeType()) {
         MimeType.`application∕zip`, MimeType.`application∕x-bzip`, MimeType.`application∕x-bzip2`, MimeType.`application∕x-gtar` -> IconFA.FILE_ARCHIVE_ALT
         MimeType.`text∕css` -> if (file.isSkinFile()) IconFA.PAINT_BRUSH else IconFA.CSS3
         MimeType.`application∕pdf` -> IconFA.FILE_PDF_ALT
         else -> when (mime.group) {
            MimeGroup.text -> IconFA.FILE_TEXT_ALT
            MimeGroup.audio -> IconFA.FILE_AUDIO_ALT
            MimeGroup.image -> IconFA.FILE_IMAGE_ALT
            MimeGroup.video -> IconFA.FILE_VIDEO_ALT
            else -> IconFA.FILE
         }
      }
   }
}

fun Font.rowHeight(): Double {
   var h = (size*1.5).toLong()  // decimal number helps pixel alignment
   h = if (h%2==0L) h else h + 1   // even number helps layout symmetry
   return h.toDouble()
}

fun listBox(block: VBox.() -> Unit) = vBox(null, CENTER_LEFT) { styleClass += "list-box"; isFillWidth = true; block() }

fun listBoxRow(glyph: GlyphIcons, text: String = "", block: LabelWithIcon.() -> Unit) = LabelWithIcon(glyph, text).apply { styleClass += "list-box-row"; block() }

fun searchTextField() = SpitTextField().apply {
   id = "search-text-field"
   styleClass += "search"

   onEventDown(KEY_PRESSED, ESCAPE, consume = false) {
      if (!text.isNullOrEmpty()) {
         it.consume()
         clear()
      }
   }

   left setToOne Icon().also { i ->
      i.styleclass("search-icon-sign")
      i.isMouseTransparent = true
      i.isFocusTraversable = false
   }
   right setToOne Icon().also { i ->
      i.styleClass += "search-clear-button"
      i.isFocusTraversable = false
      i.opacity = 0.0
      i.onClickDo { clear() }
      i.visibleProperty() syncFrom editableProperty()

      val fade = anim(200.millis) { i.opacity = it }.applyNow()
      textProperty() map { it.isNullOrBlank() } attach { fade.playFromDir(!it) }
   }
}

fun okIcon(text: String = "", action: () -> Unit) = button(text) {
   isDefaultButton = true
   onAction = EventHandler { action() }
   styleClass += "ok-button"
   graphic = Icon().apply {
      isFocusTraversable = false
      isMouseTransparent = true
      focusOwner.value = this@button
   }
}

fun errorIcon(error: Any?) = Icon().apply {
   onClickDo { APP.ui.actionPane.show(error) }
   isFocusTraversable = false
   isMouseTransparent = true
}

fun <N: Node> showFloating(title: String, shower: Shower = WINDOW_ACTIVE(CENTER), content: (PopWindow) -> N): PopWindow = popWindow {
   this.title.value = title
   this.content.value = content(this)

   show(shower)
}

fun showConfirmation(text: String, shower: Shower = WINDOW_ACTIVE(CENTER), action: () -> Unit) {
   popWindow {
      content.value = vBox(0, CENTER) {
         lay += SpitText(text).apply {
            boundsType = TextBoundsType.LOGICAL_VERTICAL_CENTER
            textAlignment = TextAlignment.CENTER
            wrappingWithNatural.subscribe()
         }
         lay += okIcon {
            action()
            if (isShowing) hide()
         }

         applyCss()
      }
      headerVisible.value = false
      isAutohide.value = true

      show(shower)
   }
}

/**
 * Shows popup with [form] content and run action icon.
 * When action returns [Fut], it is considered asynchronous.
 * Asynchronous action displays progress indicator.
 * The popup closes upon successful action completion, unless [RepeatableAction] constraint is used (supported only if this is [Config].
 *
 * @param action action which can be invoked by user once input data pass validation. The action takes this configurable and returns any result.
 */
fun <C: Configurable<*>> C.configure(titleText: String, shower: Shower = WINDOW_ACTIVE(CENTER), action: (C) -> Any?) {
   PopWindow().apply {
      val form = form(this@configure) {
         ignoreAsOwner = true
         val result = action(it)
         if (result is Fut<*>) {
            val progressIndicator = appProgressIndicator({ headerIcons += it }, { headerIcons -= it })
            result.withProgress(progressIndicator).withAppProgress(titleText)
         }
         result
      }.apply {
         editorUi syncBiFrom APP.ui.formLayout on onHidden.asDisposer()
         onExecuteDone = {
            val needsClose = it.isOk && isShowing && !(this@configure is Config<*> && this@configure.hasConstraint<RepeatableAction>())
            if (needsClose) hide()
         }
      }

      content.value = form
      title.value = titleText
      isAutohide.value = false
      headerIcons += formEditorsUiToggleIcon(form.editorUi)
      show(shower)

      form.focusFirstConfigEditor()
   }
}

/** Calls [configure] with simple [ValueConfig] and [nonEmpty] */
fun configureString(titleText: String, inputName: String, shower: Shower = WINDOW_ACTIVE(CENTER), action: (String) -> Any?) {
   ValueConfig(type(), inputName, "", "").constrain { nonEmpty() }.configure(titleText) {
      action(it.value)
   }
}

fun Any?.detectContent(): Any? = when (this) {
    null -> null
    is Char16 -> toString().toByteOrNull() ?: this
    is Char32 -> toString().toByteOrNull() ?: this
    is String -> when {
      this=="null" -> null
      this=="true" -> true
      this=="false" -> false
      this.lengthInChars==1 -> this[0].detectContent()
      this.lengthInCodePoints==1 -> this.char32At(0).detectContent()
      this.lengthInGraphemes==1 -> this.graphemeAt(0)
      else -> null
         ?: this.toBigIntegerOrNull()?.let {
            when (it) {
               in Byte.rangeBigInt -> it.toByte()
               in Short.rangeBigInt -> it.toShort()
               in Int.rangeBigInt -> it.toInt()
               in Long.rangeBigInt -> it.toLong()
               else -> it
            }
         }
         ?: this.toBigDecimalOrNull()?.let {
            when (it) {
               in Float.rangeBigDec -> it.toFloat()
               in Double.rangeBigDec -> it.toDouble()
               else -> it
            }
         }
         ?: this.toDoubleOrNull()
         ?: this.takeIf { it.startsWith("U+") || it.startsWith("\\u") }?.let { it.substring(2).toIntOrNull(16)?.toChar32() }
         ?: FileSize.ofS(this).orNull()
         ?: durationOfHMSMs(this).orNull()
         ?: APP.serializerJson.json.ast(this).orNull()
         ?: Jwt.ofS(substringAfter("Bearer ")).orNull()
         ?: runTry { uri(this) }.orNull()?.let { it.toFileOrNull()?.takeIf { it.isAbsolute } ?: it.takeIf { it.scheme!=null } }
         ?: runTry { uri(URLEncoder.encode(this, Charsets.UTF_8).replace("+", "%20")) }.orNull()?.toFileOrNull()?.takeIf { it.isAbsolute }
         ?: runTry { uri("file:///$this") }.orNull()?.toFileOrNull()?.takeIf { it.isAbsolute }
         ?: runTry { uri("file:///" + URLEncoder.encode(this, Charsets.UTF_8).replace("+", "%20")) }.orNull()?.toFileOrNull()?.takeIf { it.isAbsolute }
         ?: this
   }
   is Optional<*> -> this.orElse(null).detectContent()
   is Option.None -> null.detectContent()
   is Option.Some<*> -> this.value.detectContent()
   is Try.Error<*> -> this.value.detectContent()
   is Try.Ok<*> -> this.value.detectContent()
   is Collection<*> -> when (size) {
      0 -> null.detectContent()
      1 -> first().detectContent()
      else -> this
   }
   else -> this
}

fun autocompleteSuggestionsFor(f: Metadata.Field<*>, text: String, ignoreCase: Boolean) = APP.db.itemUniqueValuesByField[f].orEmpty().filter { it.contains(text, ignoreCase) }

@Suppress("UNUSED_VARIABLE")
fun animShowNodes(nodes: List<Node>, block: (Int, Node, Double) -> Unit): ParallelTransition {
   val total = 0.4.seconds
   val delayAbs = total/nodes.size // use for consistent total length
   val delayRel = 200.0.millis // use for consistent frequency
   return animPar(nodes) { i, node ->
      anim {
         block(i, node, it)
         node.opacity = it
      }.apply {
         dur(0.5.seconds)
         delay(150.millis + delayAbs*i)
      }
   }
}

fun animShowNodes(nodes: List<Node>) = animShowNodes(nodes) { _, node, at ->
   node.opacity = at
   node.setScaleXY(sqrt(at))
}

abstract class AnimationBuilder {
   protected open val key = "ANIMATION_OPEN_CLOSE"

   open fun applyAt(n: Node, position: Double) {
      val a = n.properties.getOrPut(key) { buildAnimation(n) } as Anim
      a.applyAt(position)
   }

   open fun closeAndDo(n: Node, action: (() -> Unit)?): Anim {
      val a = n.properties.getOrPut(key) { buildAnimation(n) } as Anim
      if (!a.isPlaying()) a.applyAt(1.0)
      a.delay(ZERO)
      a.playCloseDo(action)
      return a
   }

   open fun openAndDo(n: Node, action: (() -> Unit)?): Anim {
      val a = n.properties.getOrPut(key) { buildAnimation(n) } as Anim
      if (!a.isPlaying()) a.applyAt(0.0)
      a.delay(computeDelay())
      a.playOpenDo(action)
      return a
   }

   open fun computeDelay(): Duration = ZERO

   protected abstract fun buildAnimation(n: Node): Anim

}

object AppAnimator: AnimationBuilder() {
   public override fun buildAnimation(n: Node): Anim {
      val scaleI = geomCircular.sym()
      return anim(300.millis) {
         n.isMouseTransparent = it!=1.0
         n.opacity = 1 - (1 - it)*(1 - it)
         n.setScaleXYByTo(scaleI(it), -50.0, 0.0)
      }.apply {
         playAgainIfFinished = false
      }
   }
}

class DelayAnimator: AnimationBuilder() {
   override val key = "ANIMATION_OPEN_CLOSE_DELAYED"
   private val animDelay = AtomicLong(0)
   private val animDelayResetter = EventReducer.toLast<Void>(200.0) { animDelay.set(0) }

   override fun computeDelay(): Duration = (animDelay.get()*300.0).millis

   override fun closeAndDo(n: Node, action: (() -> Unit)?): Anim {
      val a = super.closeAndDo(n, action)
      animDelayResetter.push(null)
      return a
   }

   override fun openAndDo(n: Node, action: (() -> Unit)?): Anim {
      val a = super.openAndDo(n, action)
      animDelay.incrementAndGet()
      animDelayResetter.push(null)
      return a
   }

   override fun buildAnimation(n: Node) = AppAnimator.buildAnimation(n)
}