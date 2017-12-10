package sp.it.pl.gui.itemnode.textfield

import javafx.scene.text.Font
import sp.it.pl.gui.objects.picker.FontSelectorDialog
import sp.it.pl.main.AppUtil.APP

/** Text field for [Font] with a picker. */
class FontItemNode: TextFieldItemNode<Font>({ APP.converter.general.toS(it) }) {

    internal override fun onDialogAction() {
        FontSelectorDialog(value, { value = it }).show(this)
    }

}