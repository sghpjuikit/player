package sp.it.pl.main

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
import javafx.geometry.Insets
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Tooltip
import sp.it.pl.gui.objects.Text
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.popover.PopOver
import sp.it.pl.gui.objects.spinner.Spinner
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.animation.Anim
import sp.it.pl.util.animation.Anim.Companion.anim
import sp.it.pl.util.animation.interpolator.ElasticInterpolator
import sp.it.pl.util.async.FX
import sp.it.pl.util.async.executor.EventReducer
import sp.it.pl.util.async.future.Fut
import sp.it.pl.util.functional.invoke
import sp.it.pl.util.graphics.setScaleXY
import sp.it.pl.util.math.millis
import sp.it.pl.util.math.seconds
import sp.it.pl.util.reactive.changes
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer

private typealias In<T> = Consumer<in T>
private typealias Progress = ProgressIndicator

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
        getSkinn().contentPadding = Insets(15.0) // use css instead
        styleClass += "help-popover"
        title.value = textTitle
        isAutoHide = true
        isHideOnClick = true
        isAutoFix = true
        userResizable.value = false
        detachable.value = false
    }
}

fun createInfoIcon(text: String): Icon = Icon(FontAwesomeIcon.INFO)
        .tooltip("Help")
        .onClick { e ->
            e.consume()
            APP.actionStream.push("Info popup")
            helpPopOver(text).apply {
                    contentNode.value.wrappingWidth = 400.0
                    getSkinn().setTitleAsOnlyHeaderContent(false)
                    show(e.source as Node)
                }
        }

@JvmOverloads
fun appProgressIndicator(onStart: In<Progress> = In {}, onFinish: In<Progress> = In {}) = Spinner().apply {
    val a = anim({ setScaleXY(it*it) }).dur(500.0).intpl(ElasticInterpolator()).applyNow()
    progressProperty() changes { ov, nv ->
        if (ov.toDouble()==1.0 && nv.toDouble()!=1.0) {
            onStart(this)
            a.playOpenDo(null)
        }
        if (nv.toDouble()==1.0) {
            a.playCloseDo(Runnable { onFinish(this) })
        }
    }
}

@JvmOverloads
fun appTooltip(text: String = "") = Tooltip(text).apply {
    isHideOnEscape = true
    consumeAutoHidingEvents = true
    showDelay = seconds(1.0)
    showDuration = seconds(10.0)
    hideDelay = millis(200.0)
}

fun appTooltipForData(data: () -> Any?) = appTooltip().apply {
    val text = Text()
    graphic = text
    setOnShowing {
        computeDataInfo(data()).use(FX, In { text.text = it })
    }
}

fun computeDataInfo(data: Any?): Fut<String> = futureWrap(data).map {
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

fun resizeButton(): Icon = Icon(MaterialDesignIcon.RESIZE_BOTTOM_RIGHT).apply {
    cursor = Cursor.SE_RESIZE
    isAnimated.value = false
    styleclass("resize-content-icon")
}

fun nodeAnimation(n: Node) = anim(millis(300), { n.opacity = it*it }).apply { playAgainIfFinished = false }

open class AnimationBuilder {
    protected open val key = "ANIMATION_OPEN_CLOSE"

    open fun closeAndDo(n: Node, action: Runnable?) {
        val a = n.properties.getOrPut(key) { buildAnimation(n) } as Anim
        if (!a.isRunning()) a.applyAt(1.0)
        a.playCloseDo(action)
    }

    open fun openAndDo(n: Node, action: Runnable?) {
        val a = n.properties.getOrPut(key) { buildAnimation(n) } as Anim
        if (!a.isRunning()) a.applyAt(0.0)
        a.playOpenDo(action)
    }

    protected open fun buildAnimation(n: Node) = nodeAnimation(n)
}

object AppAnimator: AnimationBuilder()

class DelayAnimator: AnimationBuilder() {
    override val key = "ANIMATION_OPEN_CLOSE_DELAYED"
    private val animDelay = AtomicLong(0)
    private val animDelayResetter = EventReducer.toLast<Void>(200.0, Runnable { animDelay.set(0) })

    override fun closeAndDo(n: Node, action: Runnable?) {
        super.closeAndDo(n, action)
        animDelay.incrementAndGet()
        animDelayResetter.push(null)
    }

    override fun openAndDo(n: Node, action: Runnable?) {
        super.openAndDo(n, action)
        animDelay.incrementAndGet()
        animDelayResetter.push(null)
    }

    override fun buildAnimation(n: Node) = super.buildAnimation(n).delay(millis(animDelay.get()*300.0))
}