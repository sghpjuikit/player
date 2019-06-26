package sp.it.pl.gui.objects

import javafx.geometry.Orientation.VERTICAL
import javafx.scene.control.Slider
import javafx.scene.control.skin.SliderSkin
import javafx.scene.layout.StackPane
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.type.Util.getFieldValue
import sp.it.util.ui.onHoverOrDragEnd
import sp.it.util.ui.onHoverOrDragStart
import sp.it.util.ui.setScaleXY
import sp.it.util.units.millis

/** SliderSkin skin that adds animations & improved usability - track expands on mouse hover. */
open class ImprovedSliderSkin(slider: Slider): SliderSkin(slider) {
    private var thumbScaleHoverX = 1.0
    private var thumbScaleHoverY = 1.0
    private var thumbScaleFocus = 1.0
    private val onDispose = Disposer()

    init {
        initFocusAnimation()
        initHoverTrackAnimation()
        initHoverThumbAnimation()
    }

    override fun dispose() {
        onDispose()
        super.dispose()
    }

    fun initHoverTrackAnimation() {
        val track = getFieldValue<StackPane>(this, "track")!!
        val a = anim(350.millis) {
            val isVertical = skinnable.orientation==VERTICAL
            val p = 1 + 1*it*it
            track.scaleX = if (isVertical) p else 1.0
            track.scaleY = if (isVertical) 1.0 else p
        }
        a.playAgainIfFinished = false

        skinnable.onHoverOrDragStart { a.playOpen() } on onDispose
        skinnable.onHoverOrDragEnd { a.playClose() } on onDispose
        onDispose += a::stop
    }

    fun initFocusAnimation() {
        val scaling = anim(350.millis) { updateThumbScale(fxy = 1 + 1*it*it) }
        skinnable.focusedProperty() attach { if (it) scaling.playOpenDoClose(null) } on onDispose
        onDispose += scaling::stop
    }


    fun initHoverThumbAnimation() {
        val a = anim(350.millis) {
            val isVertical = skinnable.orientation==VERTICAL
            val p = 1 + 2*it*it

            updateThumbScale(hx = if (isVertical) 1.0 else p, hy = if (isVertical) p else 1.0)
        }
        a.delay = 350.millis

        skinnable.onHoverOrDragStart { a.playOpen() } on onDispose
        skinnable.onHoverOrDragEnd { a.playClose() } on onDispose
        onDispose += a::stop
    }

    private fun updateThumbScale(hx: Double = thumbScaleHoverX, hy: Double = thumbScaleHoverY, fxy: Double = thumbScaleFocus) {
        val thumb = getFieldValue<StackPane>(this, "thumb")!!
        thumbScaleHoverX = hx
        thumbScaleHoverY = hy
        thumbScaleFocus = fxy
        thumb.setScaleXY(maxOf(thumbScaleHoverX, thumbScaleFocus), maxOf(thumbScaleHoverY, thumbScaleFocus))
    }

}