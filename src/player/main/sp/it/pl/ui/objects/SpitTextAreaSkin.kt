package sp.it.pl.ui.objects

import javafx.scene.control.TextArea
import javafx.scene.control.skin.TextAreaSkin
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import sp.it.pl.ui.showContextMenu
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp

/**
 * [TextAreaSkin] with:
 * - improved right click context menu ([showContextMenu])
 */
class SpitTextAreaSkin(control: TextArea): TextAreaSkin(control) {
   private val disposer = Disposer()

   fun initCustomContextMenu() {
      val control = skinnable
      control.onEventUp(ContextMenuEvent.ANY) { it.consume() } on disposer
      control.onEventDown(MOUSE_CLICKED, SECONDARY) { showContextMenu(control, it, control::getText, null) } on disposer
   }

   override fun install() {
      super.install()
      initCustomContextMenu()
   }

   override fun dispose() {
      disposer()
      super.dispose()
   }

}