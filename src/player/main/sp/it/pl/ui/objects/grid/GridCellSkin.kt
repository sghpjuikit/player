package sp.it.pl.ui.objects.grid

import javafx.scene.control.skin.CellSkinBase

class GridCellSkin<T:Any, F:Any>(control: GridCell<T, F>): CellSkinBase<GridCell<T, F>>(control) {

   // actual text
   // private val label: LabeledText by lazy { sp.it.util.type.Util.getFieldValue(this, "text") }

   override fun updateChildren() {}

   override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {}

}