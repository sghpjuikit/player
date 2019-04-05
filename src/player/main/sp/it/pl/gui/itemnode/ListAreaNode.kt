package sp.it.pl.gui.itemnode

import javafx.collections.FXCollections
import javafx.scene.control.TextArea
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.Priority.ALWAYS
import sp.it.pl.util.collections.getElementType
import sp.it.pl.util.collections.setTo
import sp.it.pl.util.functional.Functors
import sp.it.pl.util.reactive.attach
import sp.it.pl.util.ui.lay
import sp.it.pl.util.ui.vBox
import java.util.function.Consumer

// TODO: manual text edits should be part of transformation chain. It
// is doable like this:
// - all subsequent edits until another function is used are condensed into single 'function'
//   which simply returns the edited text. There could be some trouble with syncing the output
//   and getValue(), but its doable
// - this will lock all preceding transformations as our edit function can not react on changes
//   in previous transformations

/**
 * List editor with transformation ability. Editable area with function editor displaying the list contents.
 *
 * Allows:
 *  *  displaying the elements of the list in the text area
 *  *  manual editing of the text in the text area
 *  *  applying chain of function transformation on the list elements
 *
 * The input list is retained and all (if any) transformations are applied on it every time a change in the
 * transformation chain occurs. Every time the text is updated, all manual changes of the text are lost.
 *
 * The input can be:
 *  * set as string which will be split by lines to list of strings
 *  * set as as homogeneous list of objects
 *  * not set, then transformation chain starts as [Void], for which functions that supply value can be provided
 *
 * The result can be accessed as:
 *  *  concatenated text at any time equal to the visible text of the text area. [getValAsText]
 *  *  list of strings. Each string element represents a single line in the text
 * area. List can be empty, but not null. [getVal]
 */
open class ListAreaNode: ValueNode<List<String>>(listOf()) {

    private val root = vBox()
    @JvmField protected val textArea = TextArea()
    @JvmField val input = FXCollections.observableArrayList<Any?>()!!
    /** The transformation chain. */
    @JvmField val transforms = FChainItemNode({ Functors.pool.getI(it) })
    /** Text of the text area and approximately concatenated [getVal]. Editable by user (ui) and programmatically. */
    @JvmField val outputText = textArea.textProperty()!!
    /**
     * Value of this - a transformation output of the input list after transformation is applied to each
     * element. The text of this area shows string representation of this list.
     *
     * Note that the area is editable, but the changes will (and possibly could) only be reflected in this list only
     * if its type is [String], i.e., if the last transformation transforms into [String]. This is because this object
     * is a [String] transformer.
     *
     * When [outputText] is edited, then if:
     *  * output type is String: it is considered a transformation of that text and it will be
     * reflected in this list, i.e., [getVal] and this will contain equal elements
     *  * output type is not String: it is considered arbitrary user change of the text representation
     * of transformation output (i.e., this list), but not the output itself.
     *
     * When further transforming the elements, the manual edit will always be ignored, i.e., only after-transformation
     * text edit will be considered.
     *
     * When observing this list, changes of the text area will only be reflected in it (and fire
     * list change events) when the output type is String. You may observe the text directly using
     * [outputText]
     */
    @JvmField val output = FXCollections.observableArrayList<Any?>()!!

    init {
        transforms.onItemChange = Consumer { transformation ->
            val result = input.map(transformation)
            if (transforms.typeOut!=String::class.java)
                output setTo result

            textArea.text = result.asSequence().map { "$it" }.joinToString("\n")
        }
        textArea.textProperty() attach { text ->
            val newVal = text.split("\n")
            changeValue(newVal)

            if (transforms.typeOut==String::class.java)
                output setTo newVal
        }
        textArea.addEventHandler<KeyEvent>(KEY_PRESSED) {
            if (it.code==KeyCode.V && it.isControlDown) {
                it.consume()
            }
        }

        root.lay += textArea
        root.lay(ALWAYS) += transforms.getNode()
    }

    /**
     * Sets the input list.
     * The input element type is determined to the best of the ability.
     * Transformation chain is cleared if the type of list has changed.
     * Updates text of the text area.
     */
    open fun setData(data: List<Any>) {
        input setTo data
        transforms.typeIn = data.getElementType()  // fires update
    }

    /** Splits the specified text and [setData] with the result */
    fun setData(text: String) = setData(text.split("\n"))

    /** @return the input that was last set or empty list if none */
    fun getInput(): List<Any> = listOf(input)

    override fun getNode() = root

    /** @return the value as text = [outputText] */
    fun getValAsText(): String = textArea.text

}