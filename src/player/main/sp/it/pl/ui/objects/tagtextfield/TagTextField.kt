package sp.it.pl.ui.objects.tagtextfield

import javafx.collections.FXCollections.observableSet
import javafx.collections.ObservableSet
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode.BACK_SPACE
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.FlowPane
import sp.it.pl.main.Ui.ICON_CLOSE
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.access.focused
import sp.it.util.access.v
import sp.it.util.collections.setTo
import sp.it.util.parsing.ConverterFromString
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachTrue
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onItemSyncWhile
import sp.it.util.reactive.sync
import sp.it.util.ui.lay
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.styleclassToggle

/** [TextField], which can be decorated with nodes inside on the left and right. */
open class TagTextField<T>(converterString: ConverterFromString<T>): FlowPane() {
   /** Displayed tag items. */
   val items: ObservableSet<T> = observableSet(LinkedHashSet())
   /** Whether user can add or remove [items]. */
   val isEditable = v(true)
   /** Text field for new [items]. [TextField.isEditable] if [isEditable]. The last child in this [TagTextField], not a child if not editable. */
   val textField = TextField("")

   init {
      styleClass += STYLECLASS

      // The FlowPane pretends to be the TextField
      onEventDown(MOUSE_CLICKED) { textField.requestFocus() }
      focused attachTrue { textField.requestFocus() }
      textField.focused attach { pseudoClassChanged("has-focus", it) }

      // ENTER commits tag, BACKSPACE removes last tag
      textField.onEventDown(KEY_PRESSED, ENTER) {
         if (isEditable.value)
            converterString.ofS(textField.text ?: "").ifOk {
               textField.text = ""
               items += it
            }
      }
      textField.onEventDown(KEY_PRESSED, BACK_SPACE) {
         if (isEditable.value)
            if (textField.text.isEmpty()) {
               items setTo items.toList().dropLast(1)
            }
      }

      // Read-only mode disables
      isEditable sync { editable ->
         textField.isEditable = editable
         styleclassToggle("readonly", !editable)

         lay -= textField
         if (editable) lay += textField
         tagNodes().forEach { it.updateEditable() }
      }

      items.onItemSyncWhile {
         val tagNode = TagNode(it).updateEditable()
         if (isEditable.value) lay -= textField
         lay += tagNode
         if (isEditable.value) lay += textField
         Subscription { lay -= tagNode }
      } on onNodeDispose

   }

   private fun tagNodes() = children.filterIsInstance<TagNode<T>>()

   private fun TagNode<T>.closeTagIcon() = Icon(ICON_CLOSE).onClickDo { items -= tagItem }

   private fun TagNode<T>.updateEditable() = apply {
      styleclassToggle("readonly", !isEditable.value)
      graphic = if (isEditable.value) closeTagIcon() else null
   }

   class TagNode<T>(val tagItem: T): Label(tagItem.toUi()) {
      init {
         styleClass += STYLECLASS
      }

      companion object {
         const val STYLECLASS = "tag-text-field-tag"
      }
   }

   companion object {
      const val STYLECLASS = "tag-text-field"
   }

}