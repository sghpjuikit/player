package sp.it.pl.ui.objects

import java.time.Instant
import javafx.geometry.Orientation.VERTICAL
import javafx.scene.control.ScrollBar
import javafx.scene.control.ScrollPane
import javafx.scene.control.skin.ScrollPaneSkin
import javafx.scene.input.ScrollEvent
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.paint.Color.BLACK
import javafx.scene.paint.Color.TRANSPARENT
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop
import javafx.scene.shape.Rectangle
import kotlin.math.sign
import kotlin.math.sqrt
import sp.it.pl.main.emScaled
import sp.it.util.access.v
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.Anim.Interpolators.Companion.easeOut
import sp.it.util.animation.Anim.Interpolators.Companion.interpolator
import sp.it.util.animation.Anim.Interpolators.Companion.rev
import sp.it.util.animation.Anim.Interpolators.Companion.sym
import sp.it.util.dev.fail
import sp.it.util.functional.net
import sp.it.util.math.P
import sp.it.util.math.clip
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.time.isOlderThanFx
import sp.it.util.type.Util.getFieldValue
import sp.it.util.ui.Util
import sp.it.util.ui.size
import sp.it.util.ui.x
import sp.it.util.units.millis

/** ScrollPaneSkin skin that adds animations, fade effect and consumes scroll events (if the content does not fit). */
open class SpitScrollPaneSkin(scrollPane: ScrollPane): ScrollPaneSkin(scrollPane) {
   val onDispose = Disposer()

   init {
      initConsumeScrolling()
      initFadeEffect()
      initScrollAnimationEffect()
   }

   override fun dispose() {
      onDispose()
      super.dispose()
   }

   fun initFadeEffect() {
      getFieldValue<Rectangle>(this, "clipRect").apply {
         fun update() {
            val o = orient()
            fill = if (!skinnable.isPannable && o is OrientUni && true==skinnable.content?.net { it.layoutBounds.width>width || it.layoutBounds.height>height }) {
               val v = skinnable.hvalue x skinnable.vvalue o o
               val vMin = skinnable.hmin x skinnable.vmin o o
               val vMax = skinnable.hmax x skinnable.vmax o o
               LinearGradient(
                  0.0, 0.0, 1 x 0 o o, 0 x 1 o o, true, CycleMethod.NO_CYCLE,
                  Stop(0.0, if (v<=vMin) BLACK else TRANSPARENT),
                  Stop(0.1, BLACK),
                  Stop(0.9, BLACK),
                  Stop(1.0, if (v>=vMax) BLACK else TRANSPARENT)
               )
            } else {
               BLACK
            }
         }

         skinnable.pannableProperty() attach { update() } on onDispose
         skinnable.fitToHeightProperty() attach { update() } on onDispose
         skinnable.fitToWidthProperty() attach { update() } on onDispose
         skinnable.hvalueProperty() attach { update() } on onDispose
         skinnable.vminProperty() attach { update() } on onDispose
         skinnable.vmaxProperty() attach { update() } on onDispose
         skinnable.vvalueProperty() attach { update() } on onDispose
         layoutBoundsProperty() sync { update() } on onDispose
      }
   }

   fun initConsumeScrolling() {
      node.onEventDown(ScrollEvent.ANY) {
         fun ScrollBar?.isScrollingNeeded() = this!=null && visibleAmount<max
         val vsb = Util.getScrollBar(skinnable, VERTICAL)
         val hsb = Util.getScrollBar(skinnable, VERTICAL)

         if (vsb.isScrollingNeeded() || hsb.isScrollingNeeded())
            it.consume()
      }
   }


   fun initScrollAnimationEffect() {

      val a = v(anim { }) to v(anim { })
      val oRaw = orient()
      var isMinMaxAt = Instant.MIN

      skinnable.onEventUp(SCROLL) {
         if (!skinnable.isPannable && (it.deltaY!=0.0 || it.deltaX!=0.0)) {
            val scrollAmount = 0.25
            val isVertical = !it.isShiftDown
            val o = oRaw.net { if (it is OrientUni) it else if (isVertical) Orient.VER else Orient.HOR }
            val isNecessary = true==skinnable.content?.net { it.layoutBounds.size o o > skinnable.size o o }
            val size = skinnable.layoutBounds.size o o
            val sizeContent = skinnable.content?.net { it.layoutBounds.size o o } ?: size
            val vDir = -(it.deltaX.sign + it.deltaY.sign)
            val v = skinnable.hvalueProperty() to skinnable.vvalueProperty() o o
            val vMin = skinnable.hminProperty() to skinnable.vminProperty() o o
            val vMax = skinnable.hmaxProperty() to skinnable.vmaxProperty() o o
            val vFrom = v.value
            val vTo = (vFrom + vDir*(vMax.value-vMin.value)*(size*scrollAmount/sizeContent)).clip(vMin.value, vMax.value)
            val t = skinnable.content?.net { it.translateXProperty() to it.translateYProperty() o o }

            if (isNecessary) {
               val isMin = vDir<0 && v.value==vMin.value
               val isMax = vDir>0 && v.value==vMax.value
               if (isMin || isMax) {
                  if (isMinMaxAt.isOlderThanFx(500.millis)) {
                     isMinMaxAt = Instant.now()
                     (a o o).value.stop()
                     (a o o).value = anim(100.millis) { t?.value = vDir*5.emScaled*it }.intpl(interpolator { it*it }.sym().rev())
                     (a o o).value.play()
                  }
               } else {
                  (a o o).value.stop()
                  (a o o).value = anim(200.millis) { v.value = vFrom + it*(vTo-vFrom) }.intpl(interpolator { sqrt(it) }.easeOut())
                  (a o o).value.play()
               }
            }
         }
      }
   }

   private infix fun <A> Pair<A,A>.o(o: OrientUni): A = when (o) { Orient.HOR -> first; Orient.VER -> second }
   private infix fun P.o(o: OrientUni): Double = when (o) { Orient.HOR -> x; Orient.VER -> y }

   private fun orient(): Orient =
           if ( skinnable.isFitToWidth &&  skinnable.isFitToHeight) Orient.NONE
      else if (!skinnable.isFitToWidth &&  skinnable.isFitToHeight) Orient.HOR
      else if ( skinnable.isFitToWidth && !skinnable.isFitToHeight) Orient.VER
      else if (!skinnable.isFitToWidth && !skinnable.isFitToHeight) Orient.BOTH
      else fail { "Forbidden" }

   private sealed interface OrientUni
   private sealed interface Orient {
      data object NONE: Orient
      data object HOR: Orient, OrientUni
      data object VER: Orient, OrientUni
      data object BOTH: Orient
   }

}