package sp.it.pl.gui.objects

import javafx.geometry.Orientation.VERTICAL
import javafx.scene.control.Slider
import javafx.scene.control.skin.SliderSkin
import javafx.scene.layout.StackPane
import sp.it.pl.util.animation.Anim
import sp.it.pl.util.graphics.onHoverOrDragEnd
import sp.it.pl.util.graphics.onHoverOrDragStart
import sp.it.pl.util.graphics.setScaleXY
import sp.it.pl.util.math.millis
import sp.it.pl.util.reactive.attach
import sp.it.pl.util.type.Util.getFieldValue

/** SliderSkin skin that adds animations & improved usability - track expands on mouse hover. */
open class ImprovedSliderSkin(slider: Slider): SliderSkin(slider) {
    private var thumbScaleHoverX = 1.0
    private var thumbScaleHoverY = 1.0
    private var thumbScaleFocus = 1.0

    init {
        initFocusAnimation()
        initHoverTrackAnimation()
        initHoverThumbAnimation()
    }

    fun initHoverTrackAnimation() {
        val track = getFieldValue<StackPane>(this, "track")!!
        val a = Anim(millis(350)) {
            val isVertical = skinnable.orientation==VERTICAL
            val p = 1+1*it*it
            track.scaleX = if (isVertical) p else 1.0
            track.scaleY = if (isVertical) 1.0 else p
        }
        a.playAgainIfFinished = false

        skinnable.onHoverOrDragStart { a.playOpen() }
        skinnable.onHoverOrDragEnd { a.playClose() }
    }

    fun initFocusAnimation() {
        val scaling = Anim(millis(350)) { updateThumbScale(fxy = 1+1*it*it) }
        skinnable.focusedProperty() attach { if (it) scaling.playOpenDoClose {} }
    }


    fun initHoverThumbAnimation() {
        val a = Anim(millis(350)) {
            val isVertical = skinnable.orientation==VERTICAL
            val p = 1+3*it*it
            updateThumbScale(hx = if (isVertical) 1.0 else p, hy = if (isVertical) p else 1.0)
        }
        a.delay = millis(350)

        skinnable.onHoverOrDragStart { a.playOpen() }
        skinnable.onHoverOrDragEnd { a.playClose() }
    }

    private fun updateThumbScale(hx: Double = thumbScaleHoverX, hy: Double = thumbScaleHoverY, fxy: Double = thumbScaleFocus) {
        val thumb = getFieldValue<StackPane>(this, "thumb")!!
        thumbScaleHoverX = hx
        thumbScaleHoverY = hy
        thumbScaleFocus = fxy
        thumb.setScaleXY(maxOf(thumbScaleHoverX, thumbScaleFocus), maxOf(thumbScaleHoverY, thumbScaleFocus))
    }

}