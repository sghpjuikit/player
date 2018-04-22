package sp.it.pl.gui.itemnode.textfield

import javafx.scene.control.TextField
import sp.it.pl.gui.objects.textfield.DecoratedTextField
import sp.it.pl.util.access.AccessibleValue
import sp.it.pl.util.functional.clearSet
import sp.it.pl.util.functional.invoke
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
        styleClass clearSet textFieldStyleClass()
        text = nullText
        promptText = nullText

        right.value = ArrowDialogButton().apply {
            setOnMouseClicked { onDialogAction() }
        }
    }

    /** Value. */
    protected var v: T? = null
    /** Behavior executing when value changes. */
    var onValueChange: BiConsumer<T?, T?> = BiConsumer { _,_ -> }
    /** Value to string converter. */
    private val textValueConverter: (T) -> String
    /** No value text. */
    private val nullText = "<none>"

    override fun setValue(value: T?) {
        if (v==value) return

        val valueOld = v
        v = value
        val valueText = value?.let { textValueConverter(it) } ?: nullText
        text = valueText
        promptText = valueText
        onValueChange(valueOld, value)
    }

    /** @return currently displayed value */
    override fun getValue(): T? = v

    /** Behavior to be executed on dialog button click. Should invoke of an [.setValue]. */
    protected abstract fun onDialogAction()

    companion object {

        /** @return style class as [TextField] */
        @JvmStatic fun textFieldStyleClass() = TextField().styleClass.toList()

    }

}