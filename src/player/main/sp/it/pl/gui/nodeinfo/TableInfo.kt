package sp.it.pl.gui.nodeinfo

import javafx.scene.control.Labeled
import javafx.scene.control.TableView
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.text.pluralUnit
import kotlin.properties.Delegates.observable

/**
 * Provides information about table items and table item selection.
 *
 * @param <E> type of table element
 */
class TableInfo<E>: NodeInfo<TableView<E>> {

    private var updateText = {}
    private val listMonitorDisposer = Disposer()
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

        val listAll = bindable.items
        val listSelected = bindable.selectionModel.selectedItems
        updateText = { updateText(listAll, listSelected) }

        updateText()
        listAll.onChange(updateText) on listMonitorDisposer
        listSelected.onChange(updateText) on listMonitorDisposer
        listMonitorDisposer += { updateText = {} }
    }

    override fun unbind() {
        listMonitorDisposer()
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
        @JvmField
        val DEFAULT_TEXT_FACTORY: (Boolean, List<*>) -> String = { isSelectionEmpty, list ->
            val prefix = if (isSelectionEmpty) "All: " else "Selected: "
            val size = list.size
            "$prefix " + "item".pluralUnit(size)
        }

    }
}