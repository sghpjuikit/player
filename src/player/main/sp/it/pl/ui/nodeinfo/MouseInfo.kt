package sp.it.pl.ui.nodeinfo

import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.geometry.VPos
import javafx.scene.layout.VBox
import sp.it.pl.core.CoreMouse
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconMD
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.functional.net
import sp.it.util.reactive.attachNonNullWhile
import sp.it.util.reactive.on
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.prefSize
import sp.it.util.ui.times
import sp.it.util.ui.x

/** Basic display for mouse information. */
class MouseInfo: VBox() {

   private val coordL = label { styleClass += "h4" }
   private val speedL = label()

   init {
      padding = Insets(10.0)
      prefSize = -1 x -1
      alignment = CENTER

      lay += hBox {
         styleClass += "h4p-up"
         alignmentProperty() syncFrom this@MouseInfo.alignmentProperty().map { it.hpos*VPos.CENTER }
         lay += Icon(IconMA.MOUSE).apply { isMouseTransparent = true; isFocusTraversable = false }
         lay += coordL
      }
      lay += hBox {
         alignmentProperty() syncFrom this@MouseInfo.alignmentProperty().map { it.hpos*VPos.CENTER }
         lay += Icon(IconMD.SPEEDOMETER).apply { isMouseTransparent = true; isFocusTraversable = false }
         lay += speedL
      }

      sceneProperty().attachNonNullWhile { CoreMouse.observeMousePosition { coordL.text = "${it.x.toInt()} x ${it.y.toInt()}" } } on onNodeDispose
      sceneProperty().attachNonNullWhile { CoreMouse.observeMouseVelocity { speedL.text = "${it.speed.toInt()}px" + it.dir?.toInt()?.net { " $it°" }.orEmpty() } } on onNodeDispose

      coordL.text = "n/a"
      speedL.text = "n/a"
   }

}