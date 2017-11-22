package sp.it.pl.gui.objects.seeker

import javafx.scene.control.Slider
import javafx.scene.control.skin.SliderSkin
import javafx.scene.input.MouseDragEvent.MOUSE_DRAG_RELEASED
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.layout.StackPane
import javafx.util.Duration.millis
import sp.it.pl.util.animation.Anim
import sp.it.pl.util.type.Util.getFieldValue

/** Custom skin for [Slider] adding more animations. Otherwise identical to official impl.  */
class SeekerSkin(slider: Slider): SliderSkin(slider) {

    init {
        initHoverScaleAnimation()
    }

    private fun initHoverScaleAnimation() {
        val thumb = getFieldValue<StackPane>(this, "thumb")!!
        val scaling = Anim(millis(350.0)) { thumb.scaleX = 1+4.0*it*it }
        thumb.addEventFilter(MOUSE_ENTERED) { scaling.playOpen() }
        thumb.addEventFilter(MOUSE_EXITED) { scaling.playClose() }
        skinnable.addEventFilter(DRAG_DETECTED) { scaling.playOpen() }
        skinnable.addEventFilter(MOUSE_DRAG_RELEASED) { scaling.playClose() }
    }

}