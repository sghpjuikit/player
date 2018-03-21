package sp.it.pl.gui.objects.balancer

import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.control.Control
import javafx.scene.control.SkinBase
import javafx.scene.control.Slider
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent.KEY_PRESSED
import sp.it.pl.audio.playback.BalanceProperty
import sp.it.pl.util.reactive.attach
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

    constructor(balance: BalanceProperty): this(balance.get(), balance.min, balance.max)

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
    private val slider = Slider()

    init {
        children += slider.apply {
            valueProperty() attach {
                if (!slider.isValueChanging)
                    b.balance.value = it.toDouble()
            }
            prefWidth = 100.0
        }

        b.max sync { slider.max = it.toDouble() }
        b.min sync { slider.min = it.toDouble() }
        b.balance sync { slider.value = it.toDouble() }
        b.addEventFilter(KEY_PRESSED) {
            when (it.code) {
                KeyCode.RIGHT -> {
                    skinnable.incToRight()
                    it.consume()
                }
                KeyCode.LEFT -> {
                    skinnable.incToLeft()
                    it.consume()
                }
                else -> {}
            }
        }
    }

}