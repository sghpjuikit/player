package main

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
import gui.objects.Text
import gui.objects.icon.Icon
import gui.objects.spinner.Spinner
import javafx.scene.Cursor
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Tooltip
import util.animation.Anim
import util.animation.interpolator.ElasticInterpolator
import util.async.Async
import util.async.future.Fut
import util.graphics.setScaleXY
import util.math.millis
import util.math.seconds
import java.util.function.Consumer

private typealias C<T> = Consumer<T>


@JvmOverloads
fun appProgressIndicator(onStart: C<ProgressIndicator> = C {}, onFinish: C<ProgressIndicator> = C {}) = Spinner().apply {
    val a = Anim { setScaleXY(it*it) }.dur(500.0).intpl(ElasticInterpolator())
    a.applier.accept(0.0)
    progressProperty().addListener { _, ov, nv ->
        if (ov.toDouble()==1.0 && nv.toDouble()!=1.0) {
            onStart.accept(this)
            a.playOpenDo { }
        }
        if (nv.toDouble()==1.0) {
            a.playCloseDo { onFinish.accept(this) }
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
        computeDataInfo(data()).use(Async.FX, C { text.text = it })
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