/*
 * Based on ControlsFX:
 *
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

import gui.objects.grid.GridView.SelectionOn;
import gui.objects.search.Search;
import java.util.stream.Stream;
import javafx.scene.Node;
import javafx.scene.control.skin.CellSkinBase;
import util.dev.Util;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static util.Util.getAt;
import static util.functional.Util.*;

public class GridRowSkin<T, F> extends CellSkinBase<GridRow<T,F>> {
	private final GridView<T,F> grid;
	private final GridViewSkin<T,F> gridSkin;
	private final GridRow<T,F> gridRow;

	@SuppressWarnings("unchecked")
	public GridRowSkin(GridRow<T,F> control) {
		super(control);

		gridRow = control;
		grid = gridRow.getGridView();
		gridSkin = (GridViewSkin<T,F>)grid.getSkin();

		// Remove any children before creating cells (by default a LabeledText exist and we don't need it)
		getChildren().clear();
		updateCells();

		registerChangeListener(gridRow.indexProperty(), e -> updateCells());
		registerChangeListener(gridRow.widthProperty(), e -> updateCells());
		registerChangeListener(gridRow.heightProperty(), e -> updateCells());
	}

	/**
	 * Returns a cell element at a desired index
	 *
	 * @param index The index of the wanted cell element
	 * @return Cell element if exist else null
	 */
	@SuppressWarnings("unchecked")
	protected GridCell<T,F> getCellAtIndex(int index) {
		return (GridCell<T,F>) getAt(index, getChildren());
	}

	protected void updateCells() {
		int rowIndex = gridRow.getIndex();
		if (rowIndex>=0) {
			int maxCellsInRow = gridSkin.computeMaxCellsInRow();
			int totalCellsInGrid = grid.getItemsShown().size();
			int startCellIndex = rowIndex*maxCellsInRow;
			int endCellIndex = min(startCellIndex + maxCellsInRow-1, max(0,totalCellsInGrid - 1));
			int cellCount = totalCellsInGrid==0 ? 0 : endCellIndex-startCellIndex+1;

			if (cellCount<0) {
				Util.log(GridRowSkin.class).warn("This row with index={} should not exist!", rowIndex);
				return;
			}
			// add more cells if cell count increased
			repeat(cellCount-getChildren().size(), () -> getChildren().add(createCell()));
			// remove surplus cells if cell count decreased
			if (getChildren().size()>cellCount) getChildren().remove(cellCount, getChildren().size());

			if (cellCount==0) return;
			int i = 0;
			for (int cellI = startCellIndex; cellI<=endCellIndex; cellI++, i++) {
				// Check if we can re-use a cell at this index or create a new one
				GridCell<T,F> cell = getCellAtIndex(i);
				cell.updateIndex(cellI);
				cell.pseudoClassStateChanged(Search.PC_SEARCH_MATCH, false);
				cell.pseudoClassStateChanged(Search.PC_SEARCH_MATCH_NOT, false);
			}
		}
	}

	@Override
	protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		return grid.getCellHeight() + grid.getVerticalCellSpacing()*2;
	}

	@Override
	protected void layoutChildren(double x, double y, double w, double h) {
		double cellWidth = grid.getCellWidth();
		double cellHeight = grid.getCellHeight();
		double verticalCellSpacing = grid.getVerticalCellSpacing();
		int columns = gridSkin.computeMaxCellsInRow();
		double horizontalCellSpacing = (w - columns*cellWidth)/(columns + 1);
		double xPos = 0;
		double yPos = 0;
		double cellOffsetX = xPos + cellWidth + horizontalCellSpacing;
		for (Node child : getChildren()) {
			child.resizeRelocate(
				snapPositionX(xPos + horizontalCellSpacing),
				snapPositionY(yPos + verticalCellSpacing),
				snapSizeX(cellWidth),
				snapSizeY(cellHeight)
			);
			xPos += cellOffsetX;
		}
	}

	@SuppressWarnings("unchecked")
	public Stream<GridCell<T,F>> getCells() {
		return getChildren().stream().map(c -> (GridCell<T,F>) c);
	}

	private GridCell<T,F> createCell() {
		GridCell<T,F> cell = grid.getCellFactory().call(grid);
		cell.updateGridView(grid);
		cell.addEventHandler(MOUSE_CLICKED, e -> {
			if (grid.selectOn.contains(SelectionOn.MOUSE_CLICK)) {
				gridSkin.select(cell);
				e.consume();
			}
		});
		cell.hoverProperty().addListener((o, ov, nv) -> {
			if (nv && grid.selectOn.contains(SelectionOn.MOUSE_HOVER))
				gridSkin.select(cell);
		});
		cell.pseudoClassStateChanged(Search.PC_SEARCH_MATCH, false);
		cell.pseudoClassStateChanged(Search.PC_SEARCH_MATCH_NOT, false);
		return cell;
	}

}