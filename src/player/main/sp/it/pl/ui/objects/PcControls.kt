package sp.it.pl.ui.objects

import javafx.geometry.Orientation
import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Orientation.VERTICAL
import javafx.geometry.Side.LEFT
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconWH
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.JavaLegacy
import sp.it.util.access.StyleableCompanion
import sp.it.util.access.enumConverter
import sp.it.util.access.sv
import sp.it.util.access.svMetaData
import sp.it.util.collections.setToOne
import sp.it.util.reactive.sync
import sp.it.util.ui.pseudoClassChanged

/** Component for controlling pc with actions such as shutdown, restart, etc. */
class PcControls: StackPane() {
   /** Layout orientation */
   val orientation by sv<Orientation>(ORIENTATION)

   init {
      styleClass += "pc-controls"
      orientation sync { pseudoClassChanged("horizontal", it==HORIZONTAL) }
      orientation sync { pseudoClassChanged("vertical", it==VERTICAL) }

      val icons = arrayOf(
         Icon(IconWH.MOON_27).onClickDo { JavaLegacy.suspendWindows(false, false, true) }.withText(LEFT, "Sleep"),
         Icon(IconWH.MOON_THIRD_QUARTER).onClickDo { JavaLegacy.suspendWindows(true, false, true) }.withText(LEFT, "Hibernate"),
         Icon(IconWH.MOON_14).onClickDo { JavaLegacy.shutdownWindows() }.withText(LEFT, "Shutdown"),
         Icon(IconWH.MOON_0).onClickDo { JavaLegacy.restartWindows() }.withText(LEFT, "Restart"),
         Icon(IconFA.LOCK).onClickDo { JavaLegacy.lockWindows() }.withText(LEFT, "Lock"),
         Icon(IconMD.LOGOUT).onClickDo { JavaLegacy.logOffWindows() }.withText(LEFT, "Log off"),
      )

      orientation sync {
         children setToOne when (it) {
            VERTICAL -> VBox(*icons)
            HORIZONTAL -> HBox(*icons)
         }
      }
   }

   override fun getCssMetaData() = classCssMetaData

   companion object: StyleableCompanion() {
      val ORIENTATION by svMetaData<PcControls, Orientation>("-fx-orientation", enumConverter(), VERTICAL, PcControls::orientation)
   }
}