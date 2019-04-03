package sp.it.pl.gui.itemnode.textfield

import javafx.scene.text.Font
import sp.it.pl.gui.objects.picker.FontSelectorDialog
import sp.it.pl.main.APP

/** Text field for [Font] with a picker. */
class FontTextField: ValueTextField<Font>({ APP.converter.general.toS(it) }) {

    init {
        styleClass += STYLECLASS
    }

    override fun onDialogAction() {
        FontSelectorDialog(value, { value = it }).showInCenterOf(this)
    }

    companion object {
        const val STYLECLASS = "font-text-field"
    }

}