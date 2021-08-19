package sp.it.pl.ui.objects.icon

import javafx.scene.Node
import javafx.scene.input.KeyEvent.KEY_RELEASED
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.text.Text
import javafx.scene.text.TextBoundsType
import sp.it.util.functional.asIs
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp

/** Setting [TextBoundsType.VISUAL] has an effect of center aligning the icon. See more at [TextBoundsType]. */
var Icon.boundsType: TextBoundsType
   get() = lookup("Text").asIs<Text>().boundsType
   set(value) {
      lookup("Text").asIs<Text>().boundsType = value
      requestLayout()
   }

fun Icon.onClickDelegateMouseTo(node: Node) = node.onEventUp(MOUSE_CLICKED) { onMouseClicked?.handle(it) }

fun Icon.onClickDelegateKeyTo(node: Node) = node.onEventDown(KEY_RELEASED) { onKeyReleased?.handle(it) }
