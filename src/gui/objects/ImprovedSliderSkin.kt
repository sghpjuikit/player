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

/** SliderSkin skin that adds animations & improved usability. */
class ImprovedSliderSkin(slider: Slider): SliderSkin(slider) {

    init {
        // install hover animation
        val track = getFieldValue<StackPane>(this, "track")!!
        val ah = Anim(millis(350.0)) { track.scaleY = 1+it*it }
        val av = Anim(millis(350.0)) { track.scaleX = 1+it*it }
        slider.addEventHandler(MOUSE_ENTERED) { (if (slider.orientation==VERTICAL) av else ah).playOpen() }
        slider.addEventHandler(MOUSE_EXITED) { (if (slider.orientation==VERTICAL) av else ah).playClose() }
    }
}