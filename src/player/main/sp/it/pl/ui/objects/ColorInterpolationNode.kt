package sp.it.pl.ui.objects

import javafx.geometry.Insets
import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.paint.Color.TRANSPARENT
import sp.it.pl.main.emScaled
import sp.it.pl.ui.itemnode.ColorCE
import sp.it.pl.ui.itemnode.ObservableListCE
import sp.it.util.access.vn
import sp.it.util.collections.observableList
import sp.it.util.conf.ConfList
import sp.it.util.conf.ConfigDef
import sp.it.util.conf.ListConfig
import sp.it.util.conf.PropertyConfig
import sp.it.util.conf.readOnly
import sp.it.util.type.type
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.separator
import sp.it.util.ui.vBox

class ColorInterpolationNode: StackPane() {
      init {
         val editor = ObservableListCE(ListConfig("Color", ConfigDef("Color"), ConfList(type<Color>(), observableList(Color.WHITE, Color.color(0.5, 0.5, 0.5, 0.5))), "", setOf(), setOf()))
         val result = ColorCE(PropertyConfig(type<Color?>(), "Result", ConfigDef(), setOf(), vn(null), null, "").constrain { readOnly() })
         editor.onChange = { result.config.value = editor.config.value.cross() }
         editor.onChange?.invoke()
         lay += vBox {
            lay += separator(HORIZONTAL) { padding = Insets(12.emScaled) }
            lay += label {
               padding = Insets(12.emScaled)
               isWrapText = true
               text = "Combines the provided colors, in order, starting with Color.TRANSPARENT, as if the colors were stacked on top of each other, and calculates color equal to the result"
            }
            lay += editor.buildNode(false)
            lay += hBox(null, CENTER_LEFT) {
               padding = Insets(12.emScaled)
               lay += label("Result:") { padding = Insets(0.0, 12.emScaled, 0.0, 0.0) }
               lay += result.buildNode(false)
            }
            lay += separator(HORIZONTAL) { padding = Insets(12.emScaled) }
         }
      }
      companion object {
         private fun List<Color>.cross(): Color = fold(TRANSPARENT) { b, fgr -> b.cross(fgr) }
         private fun Color.cross(fgr: Color): Color = Color.color(
            fgr.opq*fgr.red + fgr.opc*opq*red,
            fgr.opq*fgr.green + fgr.opc*opq*green,
            fgr.opq*fgr.blue + fgr.opc*opq*blue,
            fgr.opq + fgr.opc*opq
         )
         private val Color.opq get() = opacity
         private val Color.opc get() = 1-opacity
      }
   }