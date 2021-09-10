package sp.it.pl.ui.objects.complexfield

import javafx.collections.FXCollections.observableSet
import javafx.collections.ObservableSet
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode.BACK_SPACE
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.FlowPane
import sp.it.pl.main.IconFA
import sp.it.pl.main.Ui.ICON_CLOSE
import sp.it.pl.main.showFloating
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.autocomplete.AutoCompletion.Companion.autoComplete
import sp.it.pl.ui.objects.complexfield.TagTextField.EditableBy.PLUS_NODE
import sp.it.pl.ui.objects.complexfield.TagTextField.EditableBy.TEXT_FIELD
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.window.NodeShow.DOWN_CENTER
import sp.it.util.access.StyleableCompanion
import sp.it.util.access.enumConverter
import sp.it.util.access.focused
import sp.it.util.access.sv
import sp.it.util.access.svMetaData
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.parsing.ConverterFromString
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachTrue
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onItemSyncWhile
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.reactive.zip
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.pseudoClassToggle
import sp.it.util.ui.textField

/** [TextField], which can be decorated with nodes inside on the left and right. */
open class TagTextField<T>(converterString: ConverterFromString<T>): FlowPane() {
   /** Displayed tag items. */
   val items: ObservableSet<T> = observableSet(LinkedHashSet())
   /** Whether user can add to or remove from [items]. */
   val isEditable = v(true)
   /** The way user can add to [items]. */
   val editableBy by sv(EDITABLE_BY)
   /** Text field for new [items]. [TextField.isEditable] if [isEditable] and [EditableBy.TEXT_FIELD]. The last child in this [TagTextField], not a child if not editable. */
   val textField = TextField("")
   /** [TagNode] for new [items]. if [isEditable] and [EditableBy.PLUS_NODE]. The last child in this [TagTextField], not a child if not editable. */
   val plusNode = label {
      styleClass += TagNode.STYLECLASS
      graphic = Icon(IconFA.PLUS).onClickDo {
         showFloating("Add item", DOWN_CENTER(this@TagTextField)) { popup ->
            textField {
               onEventDown(KEY_PRESSED, ENTER) {
                  if (this@TagTextField.isEditable.value)
                     converterString.ofS(text ?: "").ifOk {
                        itemAdd(it)
                        popup.hide()
                     }
               }
               autocompleteSuggestionProvider.value.ifNotNull {
                  autoComplete(this, it)
               }
            }
         }
      }
   }
   /** Autocomplete suggestions or null if none. Default null. */
   val autocompleteSuggestionProvider = vn<(String) -> List<T>>(null)
   /** Custom item adder overriding the implicit `[items] += item` adder or null if none. Default null. */
   val itemAdder = vn<(T) -> Unit>(null)
   /** Custom item remover overriding the implicit `[items] -= item` remover or null if none. Default null. */
   val itemRemover = vn<(T) -> Unit>(null)

   init {
      styleClass += STYLECLASS

      // The FlowPane pretends to be the TextField
      onEventDown(MOUSE_CLICKED) { textField.requestFocus() }
      focused attachTrue { textField.requestFocus() }
      textField.focused attach { pseudoClassChanged("has-focus", it) }

      // ENTER commits tag, BACKSPACE removes last tag
      textField.onEventDown(KEY_PRESSED, ENTER, false) {
         if (isEditable.value) {
            converterString.ofS(textField.text ?: "").ifOk { item ->
               textField.text = ""
               itemAdd(item)
            }
            it.consume()
         }
      }
      textField.onEventDown(KEY_PRESSED, BACK_SPACE, false) {
         if (isEditable.value) {
            if (textField.text.isEmpty() && items.isNotEmpty()) {
               itemRemove(items.last())
               it.consume()
            }
         }
      }

      // autocomplete
      autocompleteSuggestionProvider syncNonNullWhile  {
         autoComplete(textField, it)
      }

      // Read-only mode disables
      isEditable zip editableBy sync { (editable, mode) ->
         pseudoClassToggle("readonly", !editable)
         textField.isEditable = editable

         lay -= textField
         lay -= plusNode
         if (editable && mode==TEXT_FIELD) lay += textField
         if (editable && mode==PLUS_NODE) lay += plusNode
         tagNodes().forEach { it.updateEditable() }
      }

      items.onItemSyncWhile {
         val tagNode = TagNode(it).updateEditable()
         if (isEditable.value) lay -= textField
         if (isEditable.value) lay -= plusNode
         lay += tagNode
         if (isEditable.value && editableBy.value==TEXT_FIELD) lay += textField
         if (isEditable.value && editableBy.value==PLUS_NODE) lay += plusNode
         Subscription { lay -= tagNode }
      } on onNodeDispose

   }

   override fun getCssMetaData() = classCssMetaData

   private fun itemAdd(item: T) = itemAdder.value.ifNull { items += item }.ifNotNull { it(item) }

   private fun itemRemove(item: T) = itemRemover.value.ifNull { items -= item }.ifNotNull { it(item) }

   private fun tagNodes() = children.filterIsInstance<TagNode<T>>()

   private fun TagNode<T>.closeTagIcon() = Icon(ICON_CLOSE).onClickDo { itemRemove(tagItem) }

   private fun TagNode<T>.updateEditable() = apply {
      pseudoClassToggle("readonly", !isEditable.value)
      graphic = if (isEditable.value) closeTagIcon() else null
   }

   enum class EditableBy { PLUS_NODE, TEXT_FIELD }

   class TagNode<T>(val tagItem: T): Label(tagItem.toUi()) {
      init {
         styleClass += STYLECLASS
      }

      companion object {
         const val STYLECLASS = "tag-text-field-tag"
      }
   }

   companion object: StyleableCompanion() {
      const val STYLECLASS = "tag-text-field"

      val EDITABLE_BY by svMetaData<TagTextField<*>, EditableBy>("-fx-editable-by", enumConverter(), TEXT_FIELD, TagTextField<*>::editableBy)
   }

}