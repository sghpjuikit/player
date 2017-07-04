package gui.objects

import javafx.geometry.Orientation.VERTICAL
import javafx.scene.control.ScrollBar
import javafx.scene.control.skin.ScrollBarSkin
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.layout.StackPane
import javafx.util.Duration.millis
import util.animation.Anim
import util.type.Util.getFieldValue

/** ScrollBar skin that adds animations & improved usability - thumb expands on mouse hover. */
class ImprovedScrollBarSkin(scrollbar: ScrollBar): ScrollBarSkin(scrollbar) {
    private var isDragOn = false

    init {

        // install hover animation
        val thumb = getFieldValue<StackPane>(this, "thumb")!!
        val ah = Anim(millis(350.0)) { thumb.scaleY = 1+it*it }
        val av = Anim(millis(350.0)) { thumb.scaleX = 1+it*it }
        scrollbar.addEventHandler(MOUSE_ENTERED) { (if (scrollbar.orientation==VERTICAL) av else ah).playOpen() }
        scrollbar.addEventHandler(MOUSE_EXITED) {
            if (!isDragOn) {
                (if (scrollbar.orientation==VERTICAL) av else ah).playClose()
            }
        }
        scrollbar.addEventHandler(DRAG_DETECTED) { isDragOn = true }
        scrollbar.addEventHandler(MOUSE_RELEASED) {
            if (isDragOn) {
                isDragOn = false
                (if (scrollbar.orientation==VERTICAL) av else ah).playClose()
            }
        }
    }

}