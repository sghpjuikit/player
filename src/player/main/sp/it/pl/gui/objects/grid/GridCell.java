package sp.it.pl.gui.objects.grid;

import javafx.scene.control.IndexedCell;
import javafx.scene.control.Skin;
import sp.it.util.access.V;

/**
 * A cell of {@link GridView}.
 * It contains single item in the {@link sp.it.pl.gui.objects.grid.GridView#getItemsShown()} list.
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

	void update(int i, T item, boolean isSelected) {
		updateIndex(i);
		updateItem(item, item==null);
		updateSelected(isSelected);
	}

	@Override
	protected Skin<?> createDefaultSkin() {
		return new GridCellSkin<>(this);
	}

	/** Dispose of this cell with the intention of never being used again. Called automatically in when grid skin disposes. */
	protected void dispose() {}

}