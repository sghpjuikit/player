/**
 * Copyright (c) 2013, 2015, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package gui.objects.grid;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Skin;

/**
 * A GridCell is created to represent items in the {@link GridView} {@link gui.objects.grid.GridView#getItemsShown()}
 * items list.
 *
 * @see GridView
 */
public class GridCell<T,F> extends IndexedCell<T> {

	public GridCell() {
		getStyleClass().add("grid-cell");
	}

    @Override
    public void updateIndex(int i) {
        if(getIndex()!=i && i>=0)
            forceUpdateIndex(i);
    }

    public void forceUpdateIndex(int i) {
        super.updateIndex(i);

        GridView<T,F> grid = getGridView();
        T item = grid.getItemsShown().get(i);
        updateItem(item, item == null);
        updateSelected(i == grid.implGetSkin().selectedCI);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new GridCellSkin<>(this);
    }

	/**
     * The {@link GridView} that this GridCell exists within.
     */
    public SimpleObjectProperty<GridView<T,F>> gridViewProperty() {
        return gridView;
    }

    private final SimpleObjectProperty<GridView<T,F>> gridView = new SimpleObjectProperty<>(this, "gridView");

    /**
     * Sets the {@link GridView} that this GridCell exists within.
     */
    public final void updateGridView(GridView<T,F> gridView) {
        this.gridView.set(gridView);
    }

    /**
     * Returns the {@link GridView} that this GridCell exists within.
     */
    public GridView<T,F> getGridView() {
        return gridView.get();
    }
}