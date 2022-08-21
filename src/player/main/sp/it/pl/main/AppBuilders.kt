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
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.OverrunStyle.LEADING_ELLIPSIS
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY
import javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY
import javafx.scene.control.Tooltip
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.layout.Priority.NEVER
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import javafx.scene.text.TextBoundsType
import javafx.util.Callback
import kotlin.math.sqrt
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.jvmName
import kotlin.streams.asSequence
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.ui.objects.SpitText
import sp.it.pl.ui.objects.contextmenu.ValueContextMenu
import sp.it.pl.ui.objects.form.Form.Companion.form
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.spinner.Spinner
import sp.it.pl.ui.objects.table.FilteredTable
import sp.it.pl.ui.objects.table.ImprovedTable
import sp.it.pl.ui.objects.table.buildFieldedCell
import sp.it.pl.ui.objects.tablerow.SpitTableRow
import sp.it.pl.ui.objects.textfield.SpitTextField
import sp.it.pl.ui.objects.window.NodeShow.RIGHT_CENTER
import sp.it.pl.ui.objects.window.ShowArea.WINDOW_ACTIVE
import sp.it.pl.ui.objects.window.Shower
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.pl.ui.objects.window.stage.Window
import sp.it.pl.ui.pane.ConfigPane
import sp.it.util.access.fieldvalue.ColumnField.INDEX
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.access.fieldvalue.ObjectFieldOfDataClass
import sp.it.util.access.toggle
import sp.it.util.access.toggleNext
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.Anim.Companion.animPar
import sp.it.util.animation.interpolator.CircularInterpolator
import sp.it.util.animation.interpolator.EasingMode
import sp.it.util.animation.interpolator.ElasticInterpolator
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.future.Fut
import sp.it.util.collections.collectionUnwrap
import sp.it.util.collections.getElementType
import sp.it.util.collections.materialize
import sp.it.util.collections.setToOne
import sp.it.util.collections.toStringPretty
import sp.it.util.conf.Configurable
import sp.it.util.conf.ValueConfig
import sp.it.util.conf.nonEmpty
import sp.it.util.dev.Dsl
import sp.it.util.dev.printIt
import sp.it.util.file.toFileOrNull
import sp.it.util.file.toURIOrNull
import sp.it.util.functional.Option
import sp.it.util.functional.Try
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
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
import sp.it.util.type.dataComponentProperties
import sp.it.util.type.isSubtypeOf
import sp.it.util.type.kTypeNothingNonNull
import sp.it.util.type.raw
import sp.it.util.type.type
import sp.it.util.ui.button
import sp.it.util.ui.hBox
import sp.it.util.ui.hyperlink
import sp.it.util.ui.install
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.lookupSiblingUp
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.setScaleXYByTo
import sp.it.util.ui.show
import sp.it.util.ui.stackPane
import sp.it.util.ui.tableColumn
import sp.it.util.ui.text
import sp.it.util.ui.vBox
import sp.it.util.units.div
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
fun helpPopup(textContent: String, textTitle: String = "Help"): PopWindow = PopWindow().apply {
   styleClass += "help-pop-window"
   content.value = SpitText(textContent).apply {
      styleClass += "help-pop-window-text"
      wrappingWithNatural.subscribe()
   }
   title.value = textTitle
   isAutohide.value = true
   isClickHide.value = true
   userResizable.value = false
   focusOnShow.value = false
}

/** @return standardized icon that opens a help popup with the specified text (eager) */
fun infoIcon(tooltipText: String) = infoIcon { tooltipText }

/** @return standardized icon that opens a help popup with the specified text (lazy)  */
fun infoIcon(tooltipText: () -> String): Icon = Icon(IconOC.QUESTION)
   .tooltip("Help")
   .action { i ->
      APP.actionStream("Info popup")
      helpPopup(tooltipText()).apply {
         content.value.asIs<SpitText>().wrappingWidth = 400.emScaled
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

fun formEditorsUiToggleIcon(mode: Property<ConfigPane.Layout>) = Icon(IconMD.WRAP).onClickDo { mode.toggleNext() }.tooltip("Toggle editor layout. Initial value can be set globally.")

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
fun textColon(name: String, data: Any?): Node = when (data) {
   null -> text(name + ": " + data.toUi())
   is Path -> textColon(name, data.toFileOrNull() ?: data.toUi())
   is URL -> textColon(name, data.toURIOrNull() ?: data.toUi())
   is URI -> textColon(name, data.toFileOrNull() ?: data.toUi())
   is File -> hBox {
      lay(NEVER) += text("$name: ")
      lay += appHyperlinkFor(data)
   }
   else -> text(name + ": " + data.toUi())
}

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
   val a = anim { setScaleXY(sqrt(it)) }.dur(500.millis).intpl(ElasticInterpolator()).applyNow()
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

fun computeDataInfo(data: Any?): Fut<String> = (data as? Fut<*> ?: Fut.fut(data)).then {
   fun Any?.stringUnwrap(): Any? = if (this is String && lengthInGraphemes==1) graphemeAt(0) else this
   fun KClass<*>.estimateType() = createType(typeParameters.map { KTypeArg.STAR })

   val d = collectionUnwrap(it).stringUnwrap()
   val dName = APP.instanceName[d].net {
      val first41 = it.lineSequence().flatMap { it.codePoints().asSequence().plus(' '.toChar32().value) }.take(41).toList()
      if (first41.size<=40) it else first41.take(40).joinToString("") { it.toChar32().toString() } + " (first 40 characters)"
   }
   val dClass = when (d) {
      null -> Nothing::class
      else -> d::class
   }
   val dType = when (d) {
      null -> kTypeNothingNonNull()
      is List<*> -> List::class.createType(arguments = listOf(KTypeArg.invariant(d.getElementType())))
      is Set<*> -> Set::class.createType(arguments = listOf(KTypeArg.invariant(d.getElementType())))
      is Map<*, *> -> Map::class.createType(arguments = listOf(KTypeArg.invariant(d.keys.getElementType()), KTypeArg.invariant(d.values.getElementType())))
      else -> d::class.estimateType()
   }
   val dKind = "\nType: ${dType.toUi()}"
   val dKindDev = "\nType (exact): ${dClass.qualifiedName ?: dClass.jvmName}".takeIf { APP.developerMode.value }.orEmpty()
   val dInfo = APP.instanceInfo[d]
      .map { "${it.name}: ${it.value}" }
      .sorted()
      .joinToString("\n")
      .takeUnless { it.isEmpty() }
      ?.let { "\n$it" } ?: ""

   "Data: $dName$dKind$dKindDev$dInfo"
}

fun contextMenuFor(o: Any?): ContextMenu = ValueContextMenu<Any?>().apply { setItemsFor(o) }

fun <T: Any> tableViewForClass(type: KClass<T>, block: FilteredTable<T>.() -> Unit = {}): FilteredTable<T> = object: FilteredTable<T>(type.java, null) {
   override fun computeMainField(field: ObjectField<T, *>?) = field ?: fields.first { it.type.isSubtypeOf<String>() } ?: fields.firstOrNull()
   override fun computeFieldsAll() = computeFieldsAllRecursively(type)?.plus(INDEX)?.apply { toStringPretty().printIt() } ?: APP.classFields[type].toList().plus(INDEX)
   private fun <T: Any> computeFieldsAllRecursively(type: KClass<T>): List<ObjectField<T, *>>? =
      if (type.isData)
         type.dataComponentProperties()
            .map { ObjectFieldOfDataClass(it) { it.toUi() } }
            .flatMap { f -> computeFieldsAllRecursively(f.type.raw)?.map { f.flatMap(it.asIs()) } ?: listOf(f) }
      else null
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
         styleClass.add(if (f.type.isSubtypeOf<String>()) "column-header-align-left" else "column-header-align-right")
         setCellValueFactory { cf -> if (cf.value==null) null else ImprovedTable.PojoV(f.getOf(cf.value)) }
         setCellFactory { f.buildFieldedCell() }
         userData = f
         isResizable = true
      }
   }
   columnResizePolicy = if (fields.any { it!==INDEX }) UNCONSTRAINED_RESIZE_POLICY else CONSTRAINED_RESIZE_POLICY
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

fun Font.rowHeight(): Double {
   var h = (size*1.5).toLong()  // decimal number helps pixel alignment
   h = if (h%2==0L) h else h + 1   // even number helps layout symmetry
   return h.toDouble()
}

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

fun <N: Node> showFloating(title: String, shower: Shower = WINDOW_ACTIVE(CENTER), content: (PopWindow) -> N): PopWindow = PopWindow().apply {
   this.title.value = title
   this.content.value = content(this)

   show(shower)
}

fun showConfirmation(text: String, shower: Shower = WINDOW_ACTIVE(CENTER), action: () -> Unit) {
   PopWindow().apply {
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

fun <C: Configurable<*>> C.configure(titleText: String, shower: Shower = WINDOW_ACTIVE(CENTER), action: (C) -> Any?) {
   PopWindow().apply {
      val form = form(this@configure) {
         val result = action(it)
         if (result is Fut<*>) {
            val progressIndicator = appProgressIndicator({ headerIcons += it }, { headerIcons -= it })
            result.withProgress(progressIndicator).withAppProgress(titleText)
         }
         result
      }.apply {
         editorUi syncBiFrom APP.ui.formLayout on onHidden.asDisposer()
         onExecuteDone = { if (it.isOk && isShowing) hide() }
      }

      content.value = form
      title.value = titleText
      isAutohide.value = false
      headerIcons += formEditorsUiToggleIcon(form.editorUi)
      show(shower)

      form.focusFirstConfigEditor()
   }
}

fun configureString(title: String, inputName: String, action: (String) -> Any?) {
   ValueConfig(type(), inputName, "", "").constrain { nonEmpty() } .configure(title) {
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
               in Byte.MIN_VALUE.toInt().toBigInteger()..Byte.MAX_VALUE.toInt().toBigInteger() -> it.toByte()
               in Short.MIN_VALUE.toInt().toBigInteger()..Short.MAX_VALUE.toInt().toBigInteger() -> it.toShort()
               in Int.MIN_VALUE.toBigInteger()..Int.MAX_VALUE.toBigInteger() -> it.toInt()
               in Long.MIN_VALUE.toBigInteger()..Long.MAX_VALUE.toBigInteger() -> it.toLong()
               else -> it
            }
         }
         ?: this.toBigDecimalOrNull()?.let {
            when (it) {
               in Float.MIN_VALUE.toBigDecimal()..Float.MAX_VALUE.toBigDecimal() -> it.toFloat()
               in Double.MIN_VALUE.toBigDecimal()..Double.MAX_VALUE.toBigDecimal() -> it.toFloat()
               else -> it
            }
         }
         ?: this.toDoubleOrNull()
         ?: this.takeIf { it.startsWith("U+") || it.startsWith("\\u") }?.let { it.substring(2).toIntOrNull(16)?.toChar32() }
         ?: APP.serializerJson.json.ast(this).orNull()
         ?: Jwt.ofS(this).orNull()
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

   open fun closeAndDo(n: Node, action: (() -> Unit)?) {
      val a = n.properties.getOrPut(key) { buildAnimation(n) } as Anim
      if (!a.isPlaying()) a.applyAt(1.0)
      a.playCloseDo(action)
   }

   open fun openAndDo(n: Node, action: (() -> Unit)?) {
      val a = n.properties.getOrPut(key) { buildAnimation(n) } as Anim
      if (!a.isPlaying()) a.applyAt(0.0)
      a.playOpenDo(action)
   }

   protected abstract fun buildAnimation(n: Node): Anim

}

object AppAnimator: AnimationBuilder() {
   public override fun buildAnimation(n: Node): Anim {
      val scaleI = CircularInterpolator(EasingMode.EASE_OUT)
      return anim(300.millis) {
         n.isMouseTransparent = it!=1.0
         n.opacity = 1 - (1 - it)*(1 - it)
         n.setScaleXYByTo(scaleI.interpolate(0.0, 1.0, it), -50.0, 0.0)
      }.apply {
         playAgainIfFinished = false
      }
   }
}

class DelayAnimator: AnimationBuilder() {
   override val key = "ANIMATION_OPEN_CLOSE_DELAYED"
   private val animDelay = AtomicLong(0)
   private val animDelayResetter = EventReducer.toLast<Void>(200.0) { animDelay.set(0) }

   override fun closeAndDo(n: Node, action: (() -> Unit)?) {
      super.closeAndDo(n, action)
      animDelay.incrementAndGet()
      animDelayResetter.push(null)
   }

   override fun openAndDo(n: Node, action: (() -> Unit)?) {
      super.openAndDo(n, action)
      animDelay.incrementAndGet()
      animDelayResetter.push(null)
   }

   override fun buildAnimation(n: Node) = AppAnimator.buildAnimation(n).delay((animDelay.get()*300.0).millis)
}