package sp.it.pl.ui.objects

import javafx.geometry.Orientation.VERTICAL
import javafx.scene.control.Slider
import javafx.scene.control.skin.SliderSkin
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.layout.StackPane
import javafx.stage.Popup
import javafx.stage.PopupWindow.AnchorLocation.WINDOW_TOP_LEFT
import javafx.util.StringConverter
import kotlin.math.pow
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.dev.fail
import sp.it.util.functional.net
import sp.it.util.math.min
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachWhileTrue
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.zip
import sp.it.util.type.Util.getFieldValue
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.onHoverOrDrag
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.stackPane
import sp.it.util.ui.toP
import sp.it.util.ui.valueRel
import sp.it.util.ui.xy
import sp.it.util.units.millis

/** SliderSkin skin that adds animations & improved usability - track expands on mouse hover. */
open class SpitSliderSkin(slider: Slider): SliderSkin(slider) {
   private var thumbScaleHoverX = 1.0
   private var thumbScaleHoverY = 1.0
   private var thumbScaleFocus = 1.0
   private val onDispose = Disposer()

   init {
      initIds()
      initFill()
      initFocusAnimation()
      initHoverTrackAnimation()
      initHoverThumbAnimation()
      initValueChangingFix()
      initValueChangingInfo()
   }

   override fun dispose() {
      onDispose()
      super.dispose()
   }

   fun initIds() {
      getFieldValue<StackPane>(this, "thumb")!!.id = "thumb"
      getFieldValue<StackPane>(this, "track")!!.id = "track"
   }

   fun initFill() {
      val thumb = getFieldValue<StackPane>(this, "thumb")!!
      val track = getFieldValue<StackPane>(this, "track")!!
      val fill = stackPane {
         id = "fill"
         styleClass += "fill"
         isManaged = false
         isMouseTransparent = true
         thumb.boundsInParentProperty() zip track.boundsInParentProperty() sync { (_, _) ->
            val isVertical = skinnable.orientation==VERTICAL
            resizeRelocate(
               track.boundsInParent.minX,
               track.boundsInParent.minY,
               if (isVertical) thumb.boundsInParent.centerY else thumb.boundsInParent.centerX,
               track.boundsInParent.height
            )
         } on onDispose
      }
      children.add(1, fill)
   }

   fun initHoverTrackAnimation() {
      val track = getFieldValue<StackPane>(this, "track")!!
      val a = anim(350.millis) {
         val isVertical = skinnable.orientation==VERTICAL
         val p = 1 + it*it
         track.scaleX = if (isVertical) p else 1.0
         track.scaleY = if (isVertical) 1.0 else p
      }
      a.playAgainIfFinished = false

      skinnable.onHoverOrDrag { a.playFromDir(it) } on onDispose
      onDispose += a::stop
   }

   fun initFocusAnimation() {
      val scaling = anim(350.millis) { updateThumbScale(fxy = 1 + it*it) }
      skinnable.focusedProperty() attach { if (it) scaling.playOpenDoClose(null) } on onDispose
      onDispose += scaling::stop
   }

   fun initHoverThumbAnimation() {
      val a = anim(350.millis) {
         val isVertical = skinnable.orientation==VERTICAL
         val p = 1 + it*it

         updateThumbScale(hx = if (isVertical) 1.0 else p, hy = if (isVertical) p else 1.0)
      }
      a.delay = 350.millis

      skinnable.onHoverOrDrag { a.playFromDir(it) } on onDispose
      onDispose += a::stop
   }

   private fun updateThumbScale(hx: Double = thumbScaleHoverX, hy: Double = thumbScaleHoverY, fxy: Double = thumbScaleFocus) {
      val thumb = getFieldValue<StackPane>(this, "thumb")!!
      thumbScaleHoverX = hx
      thumbScaleHoverY = hy
      thumbScaleFocus = fxy
      thumb.setScaleXY(maxOf(thumbScaleHoverX, thumbScaleFocus), maxOf(thumbScaleHoverY, thumbScaleFocus))
   }

    // fixes JavaFX bug/design issue, where when dragging does not start on thumb, isValueChanging does not change
   fun initValueChangingFix() {
      skinnable.onEventDown(MOUSE_DRAGGED) { if (!skinnable.isValueChanging) skinnable.isValueChanging = true } on onDispose
      skinnable.onEventDown(MOUSE_RELEASED) { if (skinnable.isValueChanging) skinnable.isValueChanging = false } on onDispose
   }

   fun initValueChangingInfo() {
      onDispose += skinnable.valueChangingProperty() attachWhileTrue {
         val d = Disposer()

         Popup().apply {

            anchorLocation = WINDOW_TOP_LEFT
            content += stackPane {
               styleClass += listOf("popup", "slider-value-popup")

               lay += label {
                  val labelFormatter = skinnable.labelFormatter ?: labelFormatter(false, skinnable.min, skinnable.max)
                  skinnable.valueProperty() sync { text = labelFormatter.toString(it.toDouble()) } on d
               }
            }

            show(skinnable, 0.0, 0.0)

            d += ::hide
            d += skinnable.valueProperty() sync {
               xy = skinnable.localToScreen(skinnable.layoutBounds.width*skinnable.valueRel - width/2.0, 0.0).toP()
            }
         }

         Subscription { d() }
      }
   }

   companion object {

      /** Converter for [Slider.labelFormatter] that takes into account min and max values and type of value to display the ideal precision */
      fun labelFormatter(isDecimal: Boolean, min: Double, max: Double) = object: StringConverter<Double>() {
         val precisionMax = 15
         val precisionBy = 2
         val precision: Int = (max - min).net {
            when {
               it==0.0 -> 0
               it>=1 -> precisionBy
               else -> (1..precisionMax).firstOrNull { d -> it*10.0.pow(d)>=1 }?.net { (it + precisionBy) min precisionMax } ?: precisionMax
            }
         }
         override fun fromString(string: String) = fail()
         override fun toString(`object`: Double): String = when {
            isDecimal -> `object`.toLong().toString()
            else -> "%.${precision}f".format(`object`)
         }
      }

   }

}