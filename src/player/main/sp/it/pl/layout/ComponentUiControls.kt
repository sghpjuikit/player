package sp.it.pl.layout

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Orientation.VERTICAL
import javafx.scene.Node
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.TransferMode.ANY
import javafx.scene.layout.AnchorPane
import sp.it.pl.main.Df
import sp.it.pl.main.emScaled
import sp.it.pl.main.set
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.ui.height
import sp.it.util.ui.plus
import sp.it.util.ui.tilePane

interface ComponentUiControls

abstract class ComponentUiControlsBase: ComponentUiControls {
   abstract val area: ComponentUiBase<*>

   val icons = tilePane(4.0, 4.0) {
      prefColumns = 1
      prefRows = 1
      orientation = VERTICAL
   }
   val root: AnchorPane = object: AnchorPane() {
      override fun layoutChildren() {
         super.layoutChildren()

         // Center vertically if not enough vertical space for better aesthetics
         if (height<=icons.layoutBounds.height + (insets + icons.insets).height)
            icons.layoutY = height/2.0 - icons.layoutBounds.height/2.0
      }

      override fun computeMinWidth(height: Double) = 0.0 // Prevent layout interference
      override fun computeMinHeight(width: Double) = 0.0 // Prevent layout interference
      override fun computeMaxWidth(height: Double) = area.root.width // Prevent layout interference
      override fun computeMaxHeight(width: Double) = area.root.height // Prevent layout interference
   }

   protected fun onDragDetected(e: MouseEvent, root: Node) {
      if (e.button==PRIMARY && !e.isAltDown && !e.isShiftDown) {
         if (e.isShortcutDown) {
            area.detach()
            e.consume()
         } else if (area.component.parent !is ContainerFreeForm || e.y<=20.emScaled) {
            val db = root.startDragAndDrop(*ANY)
            db[Df.COMPONENT] = area.component
            root.pseudoClassStateChanged(WidgetUi.PSEUDOCLASS_DRAGGED, true)
            e.consume()
         }
      }
   }

   protected fun headerIcon(glyph: GlyphIcons?, tooltip: String, action: (Icon) -> Unit = {}) = Icon(glyph, -1.0, tooltip).styleclass("header-icon").onClickDo(action)

}