package sp.it.pl.ui.objects.balancer

import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.control.Control
import javafx.scene.control.SkinBase
import javafx.scene.control.Slider
import javafx.scene.input.KeyCode.LEFT
import javafx.scene.input.KeyCode.RIGHT
import javafx.scene.input.KeyEvent.KEY_PRESSED
import kotlin.math.abs
import sp.it.pl.audio.playback.BalanceProperty
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.zip

class Balancer: Control {

   /** The step value to increment/decrement balance value by. */
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
         if (to - min.value<0.2) to = min.value
         if (max.value - to<0.2) to = max.value

         super.set(to)
      }
   }

   constructor(balance: BalanceProperty): this(balance.get(), balance.min, balance.max)

   constructor(balance: Double, min: Double, max: Double) {
      styleClass += STYLECLASS
      this.min.value = min
      this.max.value = max
      this.balance.value = balance
   }

   fun incToRight() = balance.set(balance.value + step.value)

   fun incToLeft() = balance.set(balance.value - step.value)

   override fun createDefaultSkin() = BalancerSkin(this)

   companion object {
      var STYLECLASS = "balancer"
   }

}

class BalancerSkin(b: Balancer): SkinBase<Balancer>(b) {
   private val slider = Slider()
   private val disposer = Disposer()

   init {
      children += slider.apply {
         onEventUp(KEY_PRESSED, LEFT) { skinnable.incToLeft() }
         onEventUp(KEY_PRESSED, RIGHT) { skinnable.incToRight() }
      }
   }

   override fun install() {
      super.install()

      skinnable!!.also { b ->
         slider.apply {
            valueProperty() attach { if (!slider.isValueChanging) b.balance.value = it.toDouble() } on disposer
            minProperty() syncFrom b.min on disposer
            maxProperty() syncFrom b.max on disposer
            valueProperty() syncFrom b.balance on disposer
            b.min zip b.max sync { (min, max) -> majorTickUnit = (max.toDouble() - min.toDouble())/2.0 } on disposer
            majorTickUnit = (b.max.value - b.min.value)/2.0
            minorTickCount = 1
            isSnapToTicks = true
            prefWidth = 100.0
         }
      }
   }

   override fun dispose() {
      disposer()
      super.dispose()
   }
}