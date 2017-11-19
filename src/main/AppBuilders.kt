package main

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
import gui.objects.Text
import gui.objects.icon.Icon
import gui.objects.popover.PopOver
import gui.objects.spinner.Spinner
import javafx.geometry.Insets
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Tooltip
import main.App.APP
import util.animation.Anim
import util.animation.interpolator.ElasticInterpolator
import util.async.FX
import util.async.future.Fut
import util.functional.invoke
import util.graphics.setScaleXY
import util.math.millis
import util.math.seconds
import util.reactive.changes
import java.util.function.Consumer

private typealias C<T> = Consumer<T>

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
        skinn.contentPadding = Insets(15.0) // use css instead
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
                    contentNode.wrappingWidth = 400.0
                    skinn.setTitleAsOnlyHeaderContent(false)
                    show(e.source as Node)
                }
        }

@JvmOverloads
fun appProgressIndicator(onStart: C<ProgressIndicator> = C {}, onFinish: C<ProgressIndicator> = C {}) = Spinner().apply {
    val a = Anim { setScaleXY(it*it) }.dur(500.0).intpl(ElasticInterpolator())
    a.applier(0.0)
    progressProperty() changes { ov, nv ->
        if (ov.toDouble()==1.0 && nv.toDouble()!=1.0) {
            onStart(this)
            a.playOpenDo { }
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
    showDelay = seconds(1.0)        // TODO: make configurable
    showDuration = seconds(10.0)    // TODO: make configurable
    hideDelay = millis(200.0)       // TODO: make configurable
}

fun appTooltipForData(data: () -> Any?) = appTooltip().apply {
    val text = Text()
    graphic = text
    setOnShowing {
        computeDataInfo(data()).use(FX, C { text.text = it })
    }
}

fun computeDataInfo(data: Any?): Fut<String> = futureWrap(data).map {
    val dName = App.APP.instanceName.get(it)
    val dKind = App.APP.className.get(it?.javaClass ?: Void::class.java)
    val dInfo = App.APP.instanceInfo.get(it)
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
    styleclass("resize-content-icon")
}