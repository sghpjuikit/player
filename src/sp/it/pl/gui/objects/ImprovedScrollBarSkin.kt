package sp.it.pl.gui.objects

import javafx.geometry.Orientation.VERTICAL
import javafx.scene.control.ScrollBar
import javafx.scene.control.skin.ScrollBarSkin
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.layout.StackPane
import javafx.util.Duration.millis
import sp.it.pl.util.animation.Anim
import sp.it.pl.util.type.Util.getFieldValue

/** ScrollBar skin that adds animations & improved usability - thumb expands on mouse hover. */
class ImprovedScrollBarSkin(scrollbar: ScrollBar): ScrollBarSkin(scrollbar) {

    init {
        initHoverAnimation()
    }

    fun initHoverAnimation() {
        val thumb = getFieldValue<StackPane>(this, "thumb")!!
        val v = Anim(millis(350.0)) { p -> thumb.scaleX = 1+p*p }
        val h = Anim(millis(350.0)) { p -> thumb.scaleY = 1+p*p }
        var isDragged = false
        skinnable.addEventHandler(MOUSE_ENTERED) { (if (skinnable.orientation==VERTICAL) v else h).playOpen() }
        skinnable.addEventHandler(MOUSE_EXITED) {
            if (!isDragged) {
                (if (skinnable.orientation==VERTICAL) v else h).playClose()
            }
        }
        skinnable.addEventHandler(DRAG_DETECTED) { isDragged = true }
        skinnable.addEventHandler(MOUSE_RELEASED) {
            if (isDragged) {
                isDragged = false
                (if (skinnable.orientation==VERTICAL) v else h).playClose()
            }
        }
    }

}