package sp.it.pl.ui.nodeinfo

import javafx.scene.control.Labeled
import javafx.scene.control.ListView
import javafx.scene.control.TableView
import kotlin.properties.Delegates.observable
import sp.it.pl.ui.objects.grid.GridView
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.onChange
import sp.it.util.reactive.plus
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.text.pluralUnit

/**
 * Provides information list-like view's items and item selection selection.
 *
 * @param <E> type of element in the list
 * @param <VIEW> type of view that has list of elements
 */
abstract class ListLikeViewInfo<E, VIEW>: NodeInfo<VIEW> {

   protected var updateText = {}
   protected var listMonitorDisposer: Subscription? = null

   /** The graphical text element */
   val node: Labeled

   /**
    * Provides text to the node. The first parameters specifies whether selection
    * is empty, the other is the list of table items if selection is empty or
    * selected items if nonempty.
    */
   var textFactory by observable<(Boolean, List<E>) -> String>(DEFAULT_TEXT_FACTORY) { _, _, _ -> updateText() }

   /** Creates the info for the labeled and invokes [bind] with the table */
   constructor(node: Labeled, t: VIEW? = null) {
      this.node = node
      t?.also(::bind)
   }

   override fun setVisible(v: Boolean) {
      node.isVisible = v
   }

   override fun unbind() {
      listMonitorDisposer?.unsubscribe()
      updateText = {}
   }

   /** Updates the text of the node using the current state of the table. */
   fun updateText() = updateText.invoke()

   protected fun updateTextImpl(allItems: List<E>, selectedItems: List<E>) {
      val isAll = selectedItems.isEmpty()
      val items = if (isAll) allItems else selectedItems

      try {
         node.text = textFactory(isAll, items)
      } catch (e: NoSuchElementException) {
         // consume no such elements, works around JavaFX bug of ObservableList.iterator.next failing in certain events
      }
   }

   companion object {

      /** Default text factory. Provides texts like 'All: 1 item' or 'Selected: 89 items'. */
      val DEFAULT_TEXT_FACTORY: (Boolean, List<*>) -> String = { isSelectionEmpty, list ->
         val prefix = if (isSelectionEmpty) "All: " else "Selected: "
         val size = list.size
         "$prefix " + "item".pluralUnit(size)
      }

   }
}

class TableInfo<E> @JvmOverloads constructor(node: Labeled, bindable: TableView<E>? = null): ListLikeViewInfo<E, TableView<E>>(node, bindable) {

   override fun bind(bindable: TableView<E>) {
      unbind()
      listMonitorDisposer = bindable.itemsProperty().syncNonNullWhile {
         val listAll = it
         val listSelected = bindable.selectionModel.selectedItems
         updateText = { updateTextImpl(bindable.items, listSelected) }

         updateText()
         val s1 = listAll.onChange(updateText)
         val s2 = listSelected.onChange(updateText)
         s1 + s2
      }
   }

}

class ListInfo<E> @JvmOverloads constructor(node: Labeled, bindable: ListView<E>? = null): ListLikeViewInfo<E, ListView<E>>(node, bindable) {

   override fun bind(bindable: ListView<E>) {
      unbind()
      listMonitorDisposer = bindable.itemsProperty().syncNonNullWhile {
         val listAll = it
         val listSelected = bindable.selectionModel.selectedItems
         updateText = { updateTextImpl(bindable.items, listSelected) }

         updateText()
         val s1 = listAll.onChange(updateText)
         val s2 = listSelected.onChange(updateText)
         s1 + s2
      }
   }

}

class GridInfo<E: Any, F: Any> @JvmOverloads constructor(node: Labeled, bindable: GridView<E, F>? = null): ListLikeViewInfo<E, GridView<E, F>>(node, bindable) {

   override fun bind(bindable: GridView<E, F>) {
      unbind()
         val listAll = bindable.itemsRaw
         val listSelected = bindable.selectedItem
         updateText = { updateTextImpl(listAll, bindable.selectedItems.toList()) }

         updateText()
         val s1 = listAll.onChange(updateText)
         val s2 = listSelected.onChange(updateText)
      listMonitorDisposer = s1 + s2
   }

}