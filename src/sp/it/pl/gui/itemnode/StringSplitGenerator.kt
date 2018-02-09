package sp.it.pl.gui.itemnode

import javafx.scene.control.TextField
import sp.it.pl.util.reactive.attach

class StringSplitGenerator: ValueNode<StringSplitParser>(StringSplitParser.singular()) {
    private val node = TextField()

    init {
        node.promptText = "expression"
        node.text = value.expression
        node.textProperty() attach { generateValue(it) }
    }

    override fun getNode() = node

    private fun generateValue(s: String) {
        StringSplitParser.fromString(s).ifOk(this::changeValue)
    }

}