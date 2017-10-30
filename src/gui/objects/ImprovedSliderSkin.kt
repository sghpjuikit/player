package gui.objects

import javafx.geometry.Orientation.VERTICAL
import javafx.scene.control.Slider
import javafx.scene.control.skin.SliderSkin
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.layout.StackPane
import javafx.util.Duration.millis
import util.animation.Anim
import util.type.Util.getFieldValue

/** SliderSkin skin that adds animations & improved usability - track expands on mouse hover. */
class ImprovedSliderSkin(slider: Slider): SliderSkin(slider) {

    init {
        initHoverAnimation()
    }

    fun initHoverAnimation() {
        val track = getFieldValue<StackPane>(this, "track")!!
        val v = Anim(millis(350.0)) { track.scaleX = 1+it*it }
        val h = Anim(millis(350.0)) { track.scaleY = 1+it*it }
        skinnable.addEventHandler(MOUSE_ENTERED) { (if (skinnable.orientation==VERTICAL) v else h).playOpen() }
        skinnable.addEventHandler(MOUSE_EXITED) { (if (skinnable.orientation==VERTICAL) v else h).playClose() }
    }

}