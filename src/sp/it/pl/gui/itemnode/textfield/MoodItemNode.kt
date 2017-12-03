package sp.it.pl.gui.itemnode.textfield

import javafx.scene.Node
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.gui.objects.picker.MoodPicker
import sp.it.pl.gui.objects.popover.NodePos
import sp.it.pl.gui.objects.popover.PopOver
import sp.it.pl.gui.objects.textfield.autocomplete.AutoCompletion.autoComplete
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.access.V
import java.util.function.Consumer

/**
 * [sp.it.pl.gui.itemnode.textfield.TextFieldItemNode] for audio mood tagging values.
 * Additional functionalities
 * * Auto-completion from set of moods application is aware of
 * * Mood picker popup. The position of the picker popup can be customized.
 */
class MoodItemNode: TextFieldItemNode<String>({ APP.converter.general.toS(it) }) {

    /** The position for the picker to show on. */
    val pickerPosition = V(NodePos.RIGHT_CENTER)

    init {
        isEditable = true
        autoComplete(this) { p ->
            APP.db.itemUniqueValuesByField[Metadata.Field.MOOD]
                    ?.filter { APP.db.autocompletionFilter(it, p.userText) }
                    .orEmpty()
        }
    }

    internal override fun onDialogAction() {
        val p = PopOver<Node>().apply {
            detachable.set(false)
            arrowSize = 0.0
            arrowIndent = 0.0
            cornerRadius = 0.0
            isAutoHide = true
            isAutoFix = true
            contentNode = MoodPicker().apply {
                node.setPrefSize(800.0, 600.0)
                onCancel = Runnable { hide() }
                onSelect = Consumer {
                    value = it
                    hide()
                }
            }.node
        }
        p.show(this, pickerPosition.get())
    }

}