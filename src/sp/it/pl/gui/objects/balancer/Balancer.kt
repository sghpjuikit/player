package sp.it.pl.gui.objects.balancer

import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.control.Control
import javafx.scene.control.SkinBase
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.shape.Rectangle
import sp.it.pl.util.reactive.sync
import java.lang.Math.abs

class Balancer: Control {

    /** The step value to inctement/decrement balance value by. */
    val step = SimpleDoubleProperty(this, "max", 0.2)
    /** The maximum balance value. */
    val max = SimpleDoubleProperty(this, "max", 1.0)
    /** The minimum balance value. */
    val min = SimpleDoubleProperty(this, "min", -1.0)
    /** The balance value, in inclusive <[Balancer.min],[Balancer.max]> interval. */
    val balance = object: SimpleDoubleProperty(this, "balance", 0.0) {
        override fun set(newValue: Double) {
            var to = value

            if (to>max.value) to = max.value
            if (to<min.value) to = min.value
            if (abs(to)<0.2) to = 0.0
            if (to-min.value<0.2) to = min.value
            if (max.value-to<0.2) to = max.value

            super.set(to)
        }
    }

    /** Creates balancer with specified balance, min, max values. */
    constructor(balance: Double, min: Double, max: Double) {
        styleClass.setAll(STYLECLASS)
        this.min.value = min
        this.max.value = max
        this.balance.value = balance
    }

    fun incToRight() = balance.set(balance.value+step.value)

    fun incToLeft() = balance.set(balance.value-step.value)

    override fun createDefaultSkin() = BalancerSkin(this)

    companion object {
        var STYLECLASS = "balancer"
    }

}

class BalancerSkin(b: Balancer): SkinBase<Balancer>(b) {
    private val bgrContainer: ImageView
    private val fgrContainer: ImageView
    private val fgrClipRect: Rectangle

    init {
        registerChangeListener(b.balance) { updateClip() }
        registerChangeListener(b.max) { updateClip() }
        registerChangeListener(b.min) { updateClip() }

        // create graphics
        bgrContainer = ImageView()
        bgrContainer.isPreserveRatio = false
        bgrContainer.fitHeightProperty() sync skinnable.prefHeightProperty()
        bgrContainer.fitWidthProperty() sync skinnable.prefWidthProperty()
        bgrContainer.styleClass += "balancer-bgr"
        children += bgrContainer

        fgrContainer = ImageView()
        fgrContainer.isPreserveRatio = false
        fgrContainer.fitHeightProperty() sync skinnable.prefHeightProperty()
        fgrContainer.fitWidthProperty() sync skinnable.prefWidthProperty()

        fgrContainer.styleClass += "balancer-fgr"
        fgrContainer.isMouseTransparent = true
        children += fgrContainer

        fgrClipRect = Rectangle()
        fgrContainer.clip = fgrClipRect

        skinnable.addEventHandler(KEY_PRESSED) {
            if (it.code==KeyCode.RIGHT) {
                skinnable.incToRight()
                it.consume()
            } else if (it.code==KeyCode.LEFT) {
                skinnable.incToLeft()
                it.consume()
            }
        }
        skinnable.addEventHandler(MOUSE_DRAGGED) {
            val x = it.x-fgrContainer.layoutX
            if (skinnable.contains(x, it.y))
                skinnable.balance.value = (x/fgrContainer.fitWidth-0.5)*2
            it.consume()
        }
        skinnable.addEventHandler(MOUSE_PRESSED) {
            val x = it.x-fgrContainer.layoutX
            if (skinnable.contains(x, it.y))
                skinnable.balance.value = (x/fgrContainer.fitWidth-0.5)*2
            it.consume()
        }

        updateClip()
    }

    private fun updateClip(value: Double = skinnable.balance.value) {
        val control = skinnable
        val w = control.prefWidth // - (snappedLeftInset() + snappedRightInset());
        val h = control.prefHeight // - (snappedTopInset() + snappedBottomInset());

        val start = if (value<0) 0.0 else value*w/2
        val end = if (value>0) w else w/2+(value+1)*w/2

        fgrClipRect.relocate(start, 0.0)
        fgrClipRect.width = end-start
        fgrClipRect.height = h
    }
}
