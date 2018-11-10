package sp.it.pl.gui.infonode

import javafx.scene.control.Labeled
import javafx.scene.control.TableView
import org.reactfx.Subscription
import sp.it.pl.util.reactive.onChange
import sp.it.pl.util.text.plural

/**
 * Provides information about table items and table item selection.
 *
 * @param <E> type of table element
 */
class InfoTable<E>: InfoNode<TableView<E>> {

    /** The graphical text element */
    @JvmField var node: Labeled
    /**
     * Provides text to the node. The first parameters specifies whether selection
     * is empty, the other is the list of table items if selection is empty or
     * selected items if nonempty.
     */
    @JvmField var textFactory: (Boolean, List<*>) -> String
    private var s: Subscription? = null

    constructor(node: Labeled) {
        this.node = node
        this.textFactory = DEFAULT_TEXT_FACTORY
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
        val handleChange = { updateText(listAll, listSelected) }

        handleChange()
        s = Subscription.multi(
                listAll.onChange(handleChange),
                listSelected.onChange(handleChange)
        )
    }

    override fun unbind() {
        s?.unsubscribe()
        s = null
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
        @JvmField val DEFAULT_TEXT_FACTORY: (Boolean, List<*>) -> String = { isSelectionEmpty, list ->
            val prefix = if (isSelectionEmpty) "All: " else "Selected: "
            val size = list.size
            "$prefix$size ${"item".plural(size)}"
        }
    }
}