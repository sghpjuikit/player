
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

public class ImprovedGridRowSkin<T> extends CellSkinBase<ImprovedGridRow<T>> {

    public ImprovedGridRowSkin(ImprovedGridRow<T> control) {
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
	public ImprovedGridCell<T> getCellAtIndex(int index) {
        if( index < getChildren().size() ) {
            return (ImprovedGridCell<T>)getChildren().get(index);
        }
        return null;
    }

    /**
     *  Update all cells
     *  <p>Cells are only created when needed and re-used when possible.</p>
     */
    public void updateCells() {
        int rowIndex = getSkinnable().getIndex();
        if (rowIndex >= 0) {
            ImprovedGridView<T> gridView = getSkinnable().getGridView();
            int maxCellsInRow = ((ImprovedGridViewSkin<?>)gridView.getSkin()).computeMaxCellsInRow();
            int totalCellsInGrid = gridView.getItems().size();
            int startCellIndex = rowIndex * maxCellsInRow;
            int endCellIndex = startCellIndex + maxCellsInRow - 1;
            int cacheIndex = 0;

            for (int cellIndex = startCellIndex; cellIndex <= endCellIndex; cellIndex++, cacheIndex++) {
                if (cellIndex < totalCellsInGrid) {
                    // Check if we can re-use a cell at this index or create a new one
                    ImprovedGridCell<T> cell = getCellAtIndex(cacheIndex);
                    if( cell == null ) {
                        cell = createCell();
                        getChildren().add(cell);
                    }
                    cell.updateIndex(-1);
                    cell.updateIndex(cellIndex);
                }
                // we are going out of bounds -> exist the loop
                else { break; }
            }

            // In case we are re-using a row that previously had more cells than
            // this one, we need to remove the extra cells that remain
            getChildren().remove(cacheIndex, getChildren().size());
        }
    }

    private ImprovedGridCell<T> createCell() {
        ImprovedGridView<T> gridView = getSkinnable().gridViewProperty().get();
        ImprovedGridCell<T> cell;
        if (gridView.getCellFactory() != null) {
            cell = gridView.getCellFactory().call(gridView);
        } else {
            cell = createDefaultCellImpl();
        }
        cell.updateGridView(gridView);
        return cell;
    }

    private ImprovedGridCell<T> createDefaultCellImpl() {
        return new ImprovedGridCell<T>() {
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if(empty) {
                    setText(""); //$NON-NLS-1$
                } else {
                    setText(item.toString());
                }
            }
        };
    }

    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
    }

    @Override protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return Double.MAX_VALUE;
    }

    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        ImprovedGridView<T> gv = getSkinnable().gridViewProperty().get();
        return gv.getCellHeight() + gv.getVerticalCellSpacing() * 2;
    }

    @Override protected void layoutChildren(double x, double y, double w, double h) {
//        double currentWidth = getSkinnable().getWidth();
        double cellWidth = getSkinnable().gridViewProperty().get().getCellWidth();
        double cellHeight = getSkinnable().gridViewProperty().get().getCellHeight();
        double horizontalCellSpacing = getSkinnable().gridViewProperty().get().getHorizontalCellSpacing();
        double verticalCellSpacing = getSkinnable().gridViewProperty().get().getVerticalCellSpacing();

        double xPos = 0;
        double yPos = 0;

        // here we alter the horizontal spacing to get rid of the gap on the right
        int columns = ((ImprovedGridViewSkin)getSkinnable().getGridView().getSkin()).computeMaxCellsInRow();
        horizontalCellSpacing = (w-columns*cellWidth)/(columns+1);

        for (Node child : getChildren()) {
            child.relocate(xPos + horizontalCellSpacing, yPos + verticalCellSpacing);
            child.resize(cellWidth, cellHeight);
            xPos = xPos + horizontalCellSpacing + cellWidth; // + horizontalCellSpacing
        }
    }
}