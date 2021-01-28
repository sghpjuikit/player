package sp.it.pl.main

import de.jensd.fx.glyphs.GlyphIcons
import javafx.animation.ParallelTransition
import javafx.event.EventHandler
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.TOP_LEFT
import javafx.geometry.Side
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.OverrunStyle.LEADING_ELLIPSIS
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Tooltip
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Priority.NEVER
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import javafx.scene.text.TextBoundsType
import javafx.scene.text.TextFlow
import sp.it.pl.ui.objects.Text
import sp.it.pl.ui.objects.form.Form.Companion.form
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.spinner.Spinner
import sp.it.pl.ui.objects.textfield.DecoratedTextField
import sp.it.pl.ui.objects.window.NodeShow.RIGHT_CENTER
import sp.it.pl.ui.objects.window.ShowArea.WINDOW_ACTIVE
import sp.it.pl.ui.objects.window.popup.PopWindow
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
import sp.it.util.conf.Configurable
import sp.it.util.conf.Constraint.StringNonEmpty
import sp.it.util.conf.ValueConfig
import sp.it.util.dev.Dsl
import sp.it.util.file.toFileOrNull
import sp.it.util.file.toURIOrNull
import sp.it.util.functional.Try
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.functional.supplyIf
import sp.it.util.reactive.Unsubscriber
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachChanges
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.syncTo
import sp.it.util.system.browse
import sp.it.util.text.Char16
import sp.it.util.text.Char32
import sp.it.util.text.char32At
import sp.it.util.text.graphemeAt
import sp.it.util.text.lengthInChars
import sp.it.util.text.lengthInCodePoints
import sp.it.util.text.lengthInGraphemes
import sp.it.util.type.type
import sp.it.util.ui.hBox
import sp.it.util.ui.hyperlink
import sp.it.util.ui.install
import sp.it.util.ui.lay
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.setScaleXYByTo
import sp.it.util.ui.stackPane
import sp.it.util.ui.text
import sp.it.util.ui.vBox
import sp.it.util.units.div
import sp.it.util.units.em
import sp.it.util.units.millis
import sp.it.util.units.plus
import sp.it.util.units.seconds
import sp.it.util.units.times
import sp.it.util.units.uri
import java.io.File
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.jvmName
import sp.it.util.type.kTypeNothingNonNull

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
   content.value = Text(textContent).apply {
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
//fun infoIcon(tooltipText: () -> String): Icon = Icon(IconOC.QUESTION).tooltip(tooltipText())
fun infoIcon(tooltipText: () -> String): Icon = Icon(IconOC.QUESTION)
   .tooltip("Help")
   .action { i ->
      APP.actionStream("Info popup")
      helpPopup(tooltipText()).apply {
         content.value.asIs<Text>().wrappingWidth = 400.emScaled
         headerIconsVisible.value = false
         show(RIGHT_CENTER(i))
      }
   }

/** @return standardized icon associated with a form that invokes an action */
fun formIcon(icon: GlyphIcons, text: String, action: () -> Unit) = Icon(icon, 25.0).run {
   action(action)
   withText(Side.RIGHT, text)
}

data class BulletBuilder(val icon: Icon = Icon(IconFA.CIRCLE), var description: String? = null)

inline fun bullet(text: String, block: @Dsl BulletBuilder.() -> Unit = {}) = hBox(1.em.emScaled, TOP_LEFT) {
   val bb = BulletBuilder().apply(block)
   lay += bb.icon
   lay += vBox(2.em.emScaled) {
      lay += text(text)
      lay += supplyIf(bb.description!=null) {
         TextFlow().apply {
            children += text(bb.description!!)
         }
      }
   }
}

/** @return standardized hyperlink for a [File] that that [File.browse]s it on click */
fun appHyperlinkFor(f: File) = hyperlink(f.toUi()) {
   textOverrun = LEADING_ELLIPSIS
   onEventDown(MOUSE_CLICKED, PRIMARY) {
      if (it.clickCount==1)
         f.browse()
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
      if (nv.toDouble()==1.0) {
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
   val dName = APP.instanceName[d]
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

fun searchTextField() = DecoratedTextField().apply {
   id = "search-text-field"
   styleClass += "search"
   val isEmpty = textProperty().map { it.isNullOrBlank() }

   onEventDown(KEY_PRESSED, ESCAPE, consume = false) {
      if (!text.isNullOrEmpty()) {
         it.consume()
         clear()
      }
   }

   left.value = Icon().also { i ->
      i.styleclass("search-icon-sign")
      i.isMouseTransparent = true
      i.isFocusTraversable = false
   }
   right.value = Icon().also { i ->
      i.styleClass += "search-clear-button"
      i.isFocusTraversable = false
      i.opacity = 0.0
      i.onClickDo { clear() }
      i.visibleProperty() syncFrom editableProperty()

      val fade = anim(200.millis) { i.opacity = it }.applyNow()
      isEmpty attach { fade.playFromDir(!it) }
   }
}

fun okIcon(action: (Icon) -> Unit) = Icon().apply {
   id = "okButton"
   styleclass("form-ok-button")
   onClickDo(action)
}

fun <N: Node> showFloating(title: String, content: (PopWindow) -> N): PopWindow = PopWindow().apply {
   this.title.value = title
   this.content.value = content(this)

   show(WINDOW_ACTIVE(CENTER))
}

fun showConfirmation(text: String, action: () -> Unit) {
   PopWindow().apply {
      content.value = vBox(0, CENTER) {
         lay += Text(text).apply {
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

      show(WINDOW_ACTIVE(CENTER))
   }
}

fun <C: Configurable<*>> C.configure(titleText: String, action: (C) -> Any?) {
   PopWindow().apply {
      val form = form(this@configure) {
         val result = action(it)
         if (result is Fut<*>) {
            val progressIndicator = appProgressIndicator({ headerIcons += it }, { headerIcons -= it })
            result.withProgress(progressIndicator)
         }
         result
      }

      form.onExecuteDone = { if (it.isOk && isShowing) hide() }
      content.value = form
      title.value = titleText
      isAutohide.value = false
      show(WINDOW_ACTIVE(CENTER))

      form.focusFirstConfigEditor()
   }
}

fun configureString(title: String, inputName: String, action: (String) -> Any?) {
   ValueConfig(type(), inputName, "", "").addConstraints(StringNonEmpty()).configure(title) {
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
      this.lengthInGraphemes==1 -> this.graphemeAt(0).detectContent()
      else -> null
         ?: this.toShortOrNull()
         ?: this.toIntOrNull()
         ?: this.toLongOrNull()
         ?: this.toFloatOrNull()
         ?: this.toDoubleOrNull()
         ?: this.toBigIntegerOrNull()
         ?: this.toBigDecimalOrNull()
         ?: this.toByteOrNull()
         ?: APP.serializerJson.json.ast(this).orNull()
         ?: runTry { uri(this) }.orNull()?.net { it.toFileOrNull() ?: it }
         ?: runTry { uri(URLEncoder.encode(this, Charsets.UTF_8).replace("+", "%20")) }.orNull()?.net { it.toFileOrNull() ?: it }
         ?: runTry { uri("file:///$this") }.orNull()?.net { it.toFileOrNull() ?: it }
         ?: runTry { uri("file:///" + URLEncoder.encode(this, Charsets.UTF_8).replace("+", "%20")) }.orNull()?.net { it.toFileOrNull() ?: it }
         ?: this
   }
   is Optional<*> -> this.orElse(null).detectContent()
   is Try.Error<*> -> this.value.detectContent()
   is Try.Ok<*> -> this.value.detectContent()
   is Collection<*> -> collectionUnwrap(this).detectContent()
   else -> this
}

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
      if (!a.isRunning()) a.applyAt(1.0)
      a.playCloseDo(action)
   }

   open fun openAndDo(n: Node, action: (() -> Unit)?) {
      val a = n.properties.getOrPut(key) { buildAnimation(n) } as Anim
      if (!a.isRunning()) a.applyAt(0.0)
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