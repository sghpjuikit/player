package sp.it.pl.ui.objects.window

import javafx.css.StyleConverter
import javafx.css.StyleableObjectProperty
import javafx.geometry.Insets
import javafx.scene.layout.StackPane
import sp.it.util.access.StyleableCompanion
import sp.it.util.access.sv
import sp.it.util.access.svMetaData

class WindowRoot: StackPane() {

   /** Gap from edge considered during positioning of this popup in some positions. Default 0.0. */
   val windowPadding: StyleableObjectProperty<Insets> by sv(WINDOW_PADDING)

   override fun getCssMetaData() = classCssMetaData

   companion object: StyleableCompanion() {
      val WINDOW_PADDING by svMetaData<WindowRoot, Insets>("-fx-window-padding", StyleConverter.getInsetsConverter(), Insets.EMPTY, WindowRoot::windowPadding)
   }
}