package sp.it.pl.main

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Insets
import javafx.geometry.Side
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Tooltip
import javafx.scene.text.Font
import sp.it.pl.gui.objects.Text
import sp.it.pl.gui.objects.form.Form.Companion.form
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.popover.PopOver
import sp.it.pl.gui.objects.popover.ScreenPos
import sp.it.pl.gui.objects.spinner.Spinner
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.interpolator.CircularInterpolator
import sp.it.util.animation.interpolator.EasingMode
import sp.it.util.animation.interpolator.ElasticInterpolator
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.future.Fut
import sp.it.util.conf.Configurable
import sp.it.util.conf.ValueConfig
import sp.it.util.reactive.attachChanges
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.setScaleXYByTo
import sp.it.util.ui.text
import sp.it.util.units.millis
import sp.it.util.units.seconds
import sp.it.util.validation.Constraint
import java.util.concurrent.atomic.AtomicLong

/**
 * Creates simple help popover designed as a tooltip for help buttons.
 *
 * The popover is
 * * not detached
 * * not detachable
 * * hide on click true,
 * * autohide true
 * * not userResizable
 *
 * Tip: Associate help popovers with buttons marked with question mark or similar icon.
 */
@JvmOverloads
fun helpPopOver(textContent: String, textTitle: String = "Help"): PopOver<Text> {
    val t = Text(textContent).apply {
        styleClass += "help-popover-text"
        wrappingWithNatural.value = true
    }
    return PopOver(t).apply {
        skinn.contentPadding = Insets(15.0) // TODO: use css instead
        styleClass += "help-popover"
        title.value = textTitle
        isAutoHide = true
        isHideOnClick = true
        isAutoFix = true
        userResizable.value = false
        detachable.value = false
    }
}

/** @return standardized icon that opens a help popup with the specified text (eager) */
fun infoIcon(tooltipText: String) = infoIcon { tooltipText }

/** @return standardized icon that opens a help popup with the specified text (lazy)  */
fun infoIcon(tooltipText: () -> String): Icon = Icon(IconFA.INFO)
    .tooltip("Help")
    .onClick { e ->
        APP.actionStream("Info popup")
        helpPopOver(tooltipText()).apply {
            contentNode.value.wrappingWidth = 400.scaleEM()
            skinn.setTitleAsOnlyHeaderContent(false)
            showInCenterOf(e.source as Node)
        }
    }

/** @return standardized icon associated with a form that invokes an action */
fun formIcon(icon: GlyphIcons, text: String, action: () -> Unit) = Icon(icon, 25.0).onClick(action).withText(text, Side.RIGHT)

@JvmOverloads
fun appProgressIndicator(onStart: (ProgressIndicator) -> Unit = {}, onFinish: (ProgressIndicator) -> Unit = {}) = Spinner().apply {
    val a = anim { setScaleXY(it*it) }.dur(500.millis).intpl(ElasticInterpolator()).applyNow()
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
    setOnShowing {
        computeDataInfo(data()) ui { text.text = it }
    }
}

fun computeDataInfo(data: Any?): Fut<String> = (data as? Fut<*> ?: Fut.fut(data)).then {
    val dName = APP.instanceName.get(it)
    val dKind = APP.className.get(it?.javaClass ?: Void::class.java)
    val dInfo = APP.instanceInfo.get(it)
        .entries.asSequence()
        .map { "${it.key}: ${it.value}" }
        .sorted()
        .joinToString("\n")
        .takeUnless { it.isEmpty() }
        ?.let { "\n$it" } ?: ""

    "Data: $dName\nType: $dKind$dInfo"
}

fun resizeButton(): Icon = Icon(IconMD.RESIZE_BOTTOM_RIGHT).apply {
    cursor = Cursor.SE_RESIZE
    isAnimated.value = false
    styleclass("resize-content-icon")
}

fun Font.rowHeight(): Double {
    var h = (size*1.5).toLong()  // decimal number helps pixel alignment
    h = if (h%2==0L) h else h + 1   // even number helps layout symmetry
    return h.toDouble()
}

fun <T, C: Configurable<T>> C.configure(title: String, action: (C) -> Unit) {
    lateinit var hidePopup: () -> Unit
    val form = form(this) { action(it); hidePopup() }
    val popup = PopOver(form)
    hidePopup = { if (popup.isShowing) popup.hide() }

    popup.title.value = title
    popup.isAutoHide = true
    popup.show(ScreenPos.APP_CENTER)
    popup.contentNode.value.focusFirstConfigField()
}

fun configureString(title: String, inputName: String, action: (String) -> Unit) {
    ValueConfig(String::class.java, inputName, "")
        .constraints(Constraint.StringNonEmpty())
        .configure(title) { action(it.value) }
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