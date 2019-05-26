package sp.it.pl.gui.nodeinfo

import javafx.scene.control.Labeled
import javafx.scene.control.TableView
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.onChange
import sp.it.util.reactive.plus
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.text.pluralUnit
import kotlin.properties.Delegates.observable

/**
 * Provides information about table items and table item selection.
 *
 * @param <E> type of table element
 */
class TableInfo<E>: NodeInfo<TableView<E>> {

    private var updateText = {}
    private var listMonitorDisposer: Subscription? = null
    /** The graphical text element */
    val node: Labeled
    /**
     * Provides text to the node. The first parameters specifies whether selection
     * is empty, the other is the list of table items if selection is empty or
     * selected items if nonempty.
     */
    var textFactory by observable<(Boolean, List<E>) -> String>(DEFAULT_TEXT_FACTORY) { _, _, _ -> updateText() }

    constructor(node: Labeled) {
        this.node = node
    }

    /**
     * Creates infoTable for the labeled and invokes [bind] with the table
     */
    constructor(node: Labeled, t: TableView<E>): this(node) {
        bind(t)
    }

    override fun setVisible(v: Boolean) {
        node.isVisible = v
    }

    override fun bind(bindable: TableView<E>) {
        unbind()
        listMonitorDisposer = bindable.itemsProperty().syncNonNullWhile {
            val listAll = it
            val listSelected = bindable.selectionModel.selectedItems
            updateText = { updateText(bindable.items, listSelected) }

            updateText()
            val s1 = listAll.onChange(updateText)
            val s2 = listSelected.onChange(updateText)
            s1+s2
        }
    }

    override fun unbind() {
        listMonitorDisposer?.unsubscribe()
        updateText = {}
    }

    /**
     * Updates the text of the node using the text factory.
     *
     * @param allItems all items of the table
     * @param selectedItems selected items of the table
     */
    fun updateText(allItems: List<E>, selectedItems: List<E>) {
        val isAll = selectedItems.isEmpty()
        val items = if (isAll) allItems else selectedItems
        node.text = textFactory(isAll, items)
    }

    companion object {

        /** Default text factory. Provides texts like 'All: 1 item' or 'Selected: 89 items'. */
        val DEFAULT_TEXT_FACTORY: (Boolean, List<*>) -> String = { isSelectionEmpty, list ->
            val prefix = if (isSelectionEmpty) "All: " else "Selected: "
            val size = list.size
            "$prefix "+"item".pluralUnit(size)
        }

    }
}