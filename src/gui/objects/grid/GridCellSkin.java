package gui.objects.grid;

import javafx.scene.control.skin.CellSkinBase;

public class GridCellSkin<T, F> extends CellSkinBase<GridCell<T,F>> {

	public GridCellSkin(GridCell<T,F> control) {
		super(control);
	}

}