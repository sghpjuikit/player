package sp.it.pl.layout.container

import de.jensd.fx.glyphs.GlyphIcons
import javafx.scene.Node
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.TransferMode.ANY
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.TilePane
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.layout.widget.WidgetUi
import sp.it.pl.main.Df
import sp.it.pl.main.set
import sp.it.util.ui.height
import sp.it.util.ui.plus

interface ComponentUiControls

abstract class ComponentUiControlsBase: ComponentUiControls {
   abstract val area: ComponentUiBase<*>

   val icons = TilePane(4.0, 4.0)
   val root: AnchorPane = object: AnchorPane() {
      override fun layoutChildren() {
         super.layoutChildren()

         // Center vertically if not enough vertical space for better aesthetics
         if (height<=icons.layoutBounds.height + (insets + icons.insets).height)
            icons.layoutY = height/2.0 - icons.layoutBounds.height/2.0
      }

      override fun computeMinHeight(width: Double) = 0.0 // Prevent layout interference
   }

   protected fun onDragDetected(e: MouseEvent, root: Node) {
      if (e.button==MouseButton.PRIMARY) {
         if (e.isShortcutDown) {
            area.detach()
            e.consume()
         } else {
            if (area.component.parent !is FreeFormContainer) {
               val db = root.startDragAndDrop(*ANY)
               db[Df.COMPONENT] = area.component
               root.pseudoClassStateChanged(WidgetUi.PSEUDOCLASS_DRAGGED, true)
               e.consume()
            }
         }
      }
   }

   protected fun headerIcon(glyph: GlyphIcons?, tooltip: String, action: (Icon) -> Unit = {}) = Icon(glyph, -1.0, tooltip).styleclass("header-icon").onClickDo(action)

}