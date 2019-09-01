package sp.it.pl.gui.objects.grid

import javafx.scene.control.IndexedCell
import sp.it.util.access.V

/**
 * A cell of [GridView].
 * It contains single item in the [sp.it.pl.gui.objects.grid.GridView.getItemsShown] list.
 */
open class GridCell<T, F>: IndexedCell<T>() {

   /** [GridView] this cell belongs to. */
   val gridView = V<GridView<T, F>?>(null)

   init {
      styleClass += "grid-cell"
   }

   override fun updateIndex(i: Int) {
      if (index==i) return
      super.updateIndex(i)
   }

   fun update(i: Int, item: T?, isSelected: Boolean) {
      updateIndex(i)
      updateItem(item, item==null)
      updateSelected(isSelected)
   }

   override fun createDefaultSkin() = GridCellSkin(this)

   /** Dispose of this cell with the intention of never being used again. Called automatically in when grid skin disposes.  */
   open fun dispose() {}

}