
/**
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

import javafx.scene.Node;
import javafx.scene.control.skin.CellSkinBase;

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
            int maxCellsInRow = ((GridViewSkin<?,?>)gridView.getSkin()).computeMaxCellsInRow();
            int totalCellsInGrid = gridView.getItemsShown().size();
            int startCellIndex = rowIndex * maxCellsInRow;
            int endCellIndex = startCellIndex + maxCellsInRow - 1;
            int cacheIndex = 0;

            for (int cellIndex = startCellIndex; cellIndex <= endCellIndex; cellIndex++, cacheIndex++) {
                if (cellIndex < totalCellsInGrid) {
                    // Check if we can re-use a cell at this index or create a new one
                    GridCell<T,F> cell = getCellAtIndex(cacheIndex);
                    if(cell == null) {
                        cell = createCell();
                        getChildren().add(cell);
                    }
                    cell.updateIndex(-1);
                    cell.updateIndex(cellIndex);
                }
                // we are going out of bounds -> exist the loop
                else break;
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
//        double currentWidth = getSkinnable().getWidth();
        double cellWidth = getSkinnable().gridViewProperty().get().getCellWidth();
        double cellHeight = getSkinnable().gridViewProperty().get().getCellHeight();
        double horizontalCellSpacing = getSkinnable().gridViewProperty().get().getHorizontalCellSpacing();
        double verticalCellSpacing = getSkinnable().gridViewProperty().get().getVerticalCellSpacing();

        double xPos = 0;
        double yPos = 0;

        // here we alter the horizontal spacing to get rid of the gap on the right
        int columns = ((GridViewSkin)getSkinnable().getGridView().getSkin()).computeMaxCellsInRow();
        // TODO: use hgap as min gap or something
        horizontalCellSpacing = (w-columns*cellWidth)/(columns+1);

        for (Node child : getChildren()) {
            child.relocate(xPos + horizontalCellSpacing, yPos + verticalCellSpacing);
            child.resize(cellWidth, cellHeight);
            xPos = xPos + horizontalCellSpacing + cellWidth; // + horizontalCellSpacing
        }
    }


    private GridCell<T,F> createCell() {
        GridView<T,F> gridView = getSkinnable().gridViewProperty().get();
        GridCell<T,F> cell = gridView.getCellFactory()!=null
                ? gridView.getCellFactory().call(gridView)
                : createDefaultCellImpl();
        cell.updateGridView(gridView);
        cell.addEventHandler(MOUSE_CLICKED, e -> getSkinnable().getGridView().getSkinn().select(cell));
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