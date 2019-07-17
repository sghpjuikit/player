package sp.it.pl.gui.itemnode.textfield

import javafx.scene.Node
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.gui.objects.autocomplete.AutoCompletion.Companion.autoComplete
import sp.it.pl.gui.objects.picker.MoodPicker
import sp.it.pl.gui.objects.popover.NodePos
import sp.it.pl.gui.objects.popover.PopOver
import sp.it.pl.main.APP
import sp.it.util.access.v

/** Text field for audio mood tagging values with a picker and autocompletion. */
class MoodItemNode: ValueTextField<String>({ APP.converter.general.toS(it) }) {

   /** The position for the picker to show on. */
   val pickerPosition = v(NodePos.RIGHT_CENTER)

   init {
      styleClass += STYLECLASS
      isEditable = true
      autoComplete(this) { text ->
         APP.db.itemUniqueValuesByField[Metadata.Field.MOOD].orEmpty().filter { it.contains(text, true) }
      }
   }

   override fun onDialogAction() {
      val p = PopOver<Node>().apply {
         detachable.set(false)
         arrowSize.value = 0.0
         arrowIndent.value = 0.0
         cornerRadius.value = 0.0
         isAutoHide = true
         isAutoFix = true
         contentNode.value = MoodPicker().apply {
            root.setPrefSize(800.0, 600.0)
            onCancel = { hide() }
            onSelect = {
               value = it
               hide()
            }
            buildContent()
         }.root
      }
      p.show(this, pickerPosition.value)
   }

   companion object {
      const val STYLECLASS = "mood-text-field"
   }
}