
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

import java.util.stream.Stream;

import javafx.scene.Node;
import javafx.scene.control.skin.CellSkinBase;

import gui.objects.grid.GridView.SelectionOn;
import gui.objects.search.Search;

import static java.lang.Math.min;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static util.Util.getAt;

public class GridRowSkin<T,F> extends CellSkinBase<GridRow<T,F>> {

    public GridRowSkin(GridRow<T,F> control) {
        super(control);

        // Remove any children before creating cells (by default a LabeledText exist and we don't need it)
        getChildren().clear();
        updateCells();

        registerChangeListener(getSkinnable().indexProperty(), e -> updateCells());
        registerChangeListener(getSkinnable().widthProperty(), e -> updateCells());
        registerChangeListener(getSkinnable().heightProperty(), e -> updateCells());
    }

    /**
     *  Returns a cell element at a desired index
     *  @param index The index of the wanted cell element
     *  @return Cell element if exist else null
     */
    @SuppressWarnings("unchecked")
	public GridCell<T,F> getCellAtIndex(int index) {
        return (GridCell<T,F>) getAt(index, getChildren());
    }

    /**
     *  Update all cells
     *  <p/>Cells are only created when needed and re-used when possible.</p>
     */
    public void updateCells() {
        int rowIndex = getSkinnable().getIndex();
        if (rowIndex >= 0) {
            GridView<T,F> gridView = getSkinnable().getGridView();
            int maxCellsInRow = gridView.implGetSkin().computeMaxCellsInRow();
            int totalCellsInGrid = gridView.getItemsShown().size();
            int startCellIndex = rowIndex * maxCellsInRow;
            int endCellIndex = min(startCellIndex + maxCellsInRow, totalCellsInGrid-1);
            int cacheIndex = 0;

	        for (int i = startCellIndex; i <= endCellIndex; i++, cacheIndex++) {
                // Check if we can re-use a cell at this index or create a new one
                GridCell<T,F> cell = getCellAtIndex(cacheIndex);
                if (cell == null) {
                    cell = createCell();
                    getChildren().add(cacheIndex, cell);
                }
                cell.updateIndex(i);
                cell.pseudoClassStateChanged(Search.SEARCHMATCHPC, false);
                cell.pseudoClassStateChanged(Search.SEARCHMATCHNOTPC, false);
            }

            // In case we are re-using a row that previously had more cells than
            // this one, we need to remove the extra cells that remain
            getChildren().remove(cacheIndex, getChildren().size());
        }
    }

    @Override
    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        GridView<T,F> gv = getSkinnable().gridViewProperty().get();
        return gv.getCellHeight() + gv.getVerticalCellSpacing() * 2;
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        // double currentWidth = getSkinnable().getWidth();
        double cellWidth = getSkinnable().gridViewProperty().get().getCellWidth();
        double cellHeight = getSkinnable().gridViewProperty().get().getCellHeight();
        double verticalCellSpacing = getSkinnable().gridViewProperty().get().getVerticalCellSpacing();
        // here we alter the horizontal spacing to get rid of the gap on the right
        int columns = getSkinnable().getGridView().implGetSkin().computeMaxCellsInRow();
        double horizontalCellSpacing = (w-columns*cellWidth)/(columns+1);
        double xPos = 0;
        double yPos = 0;
        for (Node child : getChildren()) {
            child.resizeRelocate(
	            snapPosition(xPos + horizontalCellSpacing),
	            snapPosition(yPos + verticalCellSpacing),
	            snapSize(cellWidth),
	            snapSize(cellHeight)
            );
            xPos = xPos + cellWidth + horizontalCellSpacing ;
        }
    }

    @SuppressWarnings("unchecked")
    public Stream<GridCell<T,F>> getCells() {
		return getChildren().stream().map(c -> (GridCell<T,F>)c);
    }

    private GridCell<T,F> createCell() {
        GridView<T,F> grid = getSkinnable().gridViewProperty().get();
        GridCell<T,F> cell = grid.getCellFactory()!=null
                ? grid.getCellFactory().call(grid)
                : createDefaultCellImpl();
        cell.updateGridView(grid);
        cell.addEventHandler(MOUSE_CLICKED, e -> {
            if (grid.selectOn.contains(SelectionOn.MOUSE_CLICK)) {
	            getSkinnable().getGridView().implGetSkin().select(cell);
	            e.consume();
            }
        });
        cell.hoverProperty().addListener((o,ov,nv) -> {
            if (nv && grid.selectOn.contains(SelectionOn.MOUSE_HOVER))
                getSkinnable().getGridView().implGetSkin().select(cell);
        });
	    cell.pseudoClassStateChanged(Search.SEARCHMATCHPC, false);
	    cell.pseudoClassStateChanged(Search.SEARCHMATCHNOTPC, false);
        return cell;
    }

    private GridCell<T,F> createDefaultCellImpl() {
        return new GridCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.toString());
            }
        };
    }

}