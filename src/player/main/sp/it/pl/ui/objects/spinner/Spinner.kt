package sp.it.pl.ui.objects.spinner

import javafx.animation.Interpolator
import javafx.animation.RotateTransition
import javafx.animation.Transition
import javafx.geometry.Pos
import javafx.geometry.Pos.BOTTOM_RIGHT
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.SkinBase
import javafx.scene.layout.StackPane
import javafx.scene.shape.Arc
import javafx.scene.shape.Circle
import kotlin.math.abs
import sp.it.util.functional.asIs
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.ui.lay
import sp.it.util.ui.maxSize
import sp.it.util.ui.stackPane
import sp.it.util.ui.x
import sp.it.util.units.millis
import sp.it.util.units.seconds

/** Very simple custom [ProgressIndicator]. */
class Spinner: ProgressIndicator {

   constructor(progress: Double = 1.0): super(progress)

   override fun createDefaultSkin() = SpinnerSkin(this)

   class SpinnerSkin(spinner: Spinner): SkinBase<Spinner>(spinner) {
      private val inner: StackPane
      private val outer: StackPane
      private var rt: RotateTransition? = null
      private var playing = false
      private val disposer = Disposer()

      init {
         inner = stackPane {
            lay(BOTTOM_RIGHT) += Arc().apply {
               length = 360.0
               this@stackPane.prefWidthProperty() sync { radiusX = it.toDouble()*0.5 }
               this@stackPane.prefWidthProperty() sync { radiusY = it.toDouble()*0.5 }
               styleClass += "spinner"
               styleClass += "spinner-in"
            }
            clip = Circle(0.0, 0.0, 15.0)
            styleClass += "spinner-in"
            maxSize = USE_PREF_SIZE x USE_PREF_SIZE
         }
         outer = stackPane {
            lay(Pos.TOP_LEFT) += Arc().apply {
               length = 360.0
               this@stackPane.prefWidthProperty() sync { radiusX = it.toDouble()*0.5 }
               this@stackPane.prefWidthProperty() sync { radiusY = it.toDouble()*0.5 }
               styleClass += "spinner"
               styleClass += "spinner-out"
            }
            clip = Circle(0.0, 0.0, 15.0)
            styleClass += "spinner-out"
            maxSize = USE_PREF_SIZE x USE_PREF_SIZE
         }

         inner.rotateProperty() sync { outer.rotate = 360.0 - it.toDouble()/2.0 }
         inner.rotateProperty() sync { inner.clip.asIs<Circle>().radius = inner.prefWidth*(0.8 + 0.3*abs(abs(it.toDouble()).rem(360.0) - 180.0)/180.0) }
         outer.rotateProperty() sync { outer.clip.asIs<Circle>().radius = outer.prefWidth*(0.8 + 0.3*abs(abs(it.toDouble()).rem(360.0) - 180.0)/180.0) }
         children += stackPane(inner, outer)

      }

      override fun install() {
         super.install()

         skinnable!!.also { s ->
            s.progressProperty() attach { update() } on disposer
            s.visibleProperty() attach { update() } on disposer
            s.parentProperty() attach { update() } on disposer
            s.sceneProperty() attach { update() } on disposer
         }

         update()
      }

      override fun dispose() {
         rt?.stop()
         disposer()
         super.dispose()
      }

      private fun update() {
         if (skinnable.progress!=1.0 && skinnable.parent!=null && skinnable.scene!=null && skinnable.isVisible) {
            rt = rt ?: RotateTransition(120.seconds, inner).apply {
               interpolator = Interpolator.LINEAR
               cycleCount = Transition.INDEFINITE
               delay = 0.millis
               byAngle = 360*100.0
            }
            if (!playing) rt?.play()
            playing = true
         } else {
            if (playing) rt?.pause()
            playing = false
         }
      }
   }

}