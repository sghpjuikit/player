/*
 * Copyright (c) 2013, ControlsFX
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

class GridRow<T, F> extends IndexedCell<T> {

	/** {@link GridView} that this GridRow exists within */
	private final SimpleObjectProperty<GridView<T,F>> gridView = new SimpleObjectProperty<>(this, "gridView");

	public GridRow() {
		super();
		getStyleClass().add("grid-row");
	}

	@Override
	public void updateIndex(int i) {
		// Since row can resize horizontally, cells can change index even if row does not. Hence we must still update
		// if (getIndex()==i) return;

		super.updateIndex(i);
		updateItem(null, false);
		updateSelected(i==getGridView().implGetSkin().selectedRI);
	}

	@Override
	protected GridRowSkin<T,F> createDefaultSkin() {
		return new GridRowSkin<>(this);
	}

	@SuppressWarnings("unchecked")
	public GridRowSkin<T,F> getSkinImpl() {
		return (GridRowSkin<T,F>) getSkin();
	}

	/** Sets {@link #gridView}. */
	public SimpleObjectProperty<GridView<T,F>> gridViewProperty() {
		return gridView;
	}

	/** Gets {@link #gridView}. */
	public final void setGridView(GridView<T,F> gridView) {
		this.gridView.set(gridView);
	}

	/** Returns {@link #gridView}. */
	public GridView<T,F> getGridView() {
		return gridView.get();
	}

}