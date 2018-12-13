package sp.it.pl.gui.itemnode.textfield

import javafx.scene.control.TextField
import sp.it.pl.gui.objects.textfield.DecoratedTextField
import sp.it.pl.util.access.AccessibleValue
import sp.it.pl.util.functional.invoke
import sp.it.pl.util.functional.setTo
import java.util.function.BiConsumer

/**
 * Customized [TextField] that displays a nullable object value. Normally a non-editable text
 * field that brings up a popup picker for its item type. Useful as an editor with value selection feature.
 *
 * In addition there is a dialog button calling implementation dependant item
 * chooser expected in form of a pop-up.
 *
 * @param <T> type of the value
 */
abstract class TextFieldItemNode<T>: DecoratedTextField, AccessibleValue<T> {

    constructor(textValueConverter: (T) -> String): super() {
        this.textValueConverter = textValueConverter

        isEditable = false
        styleClass setTo textFieldStyleClass()
        text = nullText
        promptText = nullText

        right.value = ArrowDialogButton().apply {
            setOnMouseClicked { onDialogAction() }
        }
    }

    /** Value. */
    protected var vl: T? = null
    /** Behavior executing when value changes. */
    var onValueChange: BiConsumer<T?, T?> = BiConsumer { _,_ -> }
    /** Value to string converter. */
    private val textValueConverter: (T) -> String
    /** No value text. */
    private val nullText = "<none>"

    override fun setValue(value: T?) {
        if (vl==value) return

        val valueOld = vl
        vl = value
        text = value?.let { textValueConverter(it) } ?: nullText
        promptText = text
        onValueChange(valueOld, value)
    }

    /** @return currently displayed value */
    override fun getValue(): T? = vl

    /** Behavior to be executed on dialog button click. Should invoke of an [.setValue]. */
    protected abstract fun onDialogAction()

    companion object {

        /** @return style class as [TextField] */
        @JvmStatic fun textFieldStyleClass() = TextField().styleClass.toList()

    }

}