package sp.it.pl.gui.objects.grid;

import javafx.scene.control.IndexedCell;
import javafx.scene.control.Skin;
import sp.it.pl.util.access.V;

/**
 * A cell of {@link GridView}. It contains single item in the {@link sp.it.pl.gui.objects.grid.GridView#getItemsShown()} list.
 *
 * @see GridView
 */
public class GridCell<T, F> extends IndexedCell<T> {

	/**
	 * {@link sp.it.pl.gui.objects.grid.GridView} this cell belongs to.
	 */
	public final V<GridView<T,F>> gridView = new V<>(null);

	public GridCell() {
		getStyleClass().add("grid-cell");
	}

	@Override
	public void updateIndex(int i) {
		if (getIndex()==i) return;
		super.updateIndex(i);
	}

	void update(T item, boolean isSelected) {
		updateItem(item, item==null);
		updateSelected(isSelected);
	}

	@Override
	protected Skin<?> createDefaultSkin() {
		return new GridCellSkin<>(this);
	}

}