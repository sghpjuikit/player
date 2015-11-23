
/**
 * Copyright (c) 2013, 2015 ControlsFX
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

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.scene.control.skin.VirtualFlow;

import com.sun.javafx.scene.control.VirtualScrollBar;

import static util.Util.getFieldValue;
import static util.Util.invokeMethodP0;

public class ImprovedGridViewSkin<T> extends CustomVirtualContainerBase<ImprovedGridView<T>,ImprovedGridRow<T>> {

    private final ListChangeListener<T> gridViewItemsListener = new ListChangeListener<T>() {
        @Override public void onChanged(ListChangeListener.Change<? extends T> change) {
            updateRowCount();
            getSkinnable().requestLayout();
        }
    };
    private final WeakListChangeListener<T> weakGridViewItemsListener = new WeakListChangeListener<>(gridViewItemsListener);
    private final VirtualFlow<ImprovedGridRow<T>> f; // accesses superclass' flow field, dont name "flow"!

    private void flowRecreateCells() {
        invokeMethodP0(VirtualFlow.class,f,"recreateCells");
    }
    private void flowRebuildCells() {
        invokeMethodP0(VirtualFlow.class,f,"rebuildCells");
    }
    private void flowReconfigureCells() {
        invokeMethodP0(VirtualFlow.class,f,"reconfigureCells");
    }

    @SuppressWarnings("rawtypes")
    public ImprovedGridViewSkin(ImprovedGridView<T> control) {
        super(control);

        // make flow field accessible
        f = getFieldValue(this,VirtualFlow.class,"flow");
        updateGridViewItems();

        f.setId("virtual-flow"); //$NON-NLS-1$
        f.setPannable(false);
        f.setVertical(true);
        f.setFocusTraversable(getSkinnable().isFocusTraversable());
        f.setCellFactory(f -> ImprovedGridViewSkin.this.createCell());
        getChildren().add(f);

        updateRowCount();

        // Register listeners
        registerChangeListener(control.itemsProperty(), e -> updateGridViewItems());
        registerChangeListener(control.cellFactoryProperty(), e -> flowRecreateCells());
        registerChangeListener(control.parentProperty(), e -> {
            if (getSkinnable().getParent() != null && getSkinnable().isVisible()) {
                getSkinnable().requestLayout();
            }
        });
        registerChangeListener(control.cellHeightProperty(), e -> flowRecreateCells());
        registerChangeListener(control.cellWidthProperty(), e -> { updateRowCount(); flowRecreateCells(); });
        registerChangeListener(control.horizontalCellSpacingProperty(), e -> { updateRowCount(); flowRecreateCells(); });
        registerChangeListener(control.verticalCellSpacingProperty(), e -> flowRecreateCells());
        registerChangeListener(control.widthProperty(), e -> updateRowCount());
        registerChangeListener(control.heightProperty(), e -> updateRowCount());
    }

    public VirtualFlow<ImprovedGridRow<T>> getFlow() {
        return f;
    }

    public void updateGridViewItems() {
        if (getSkinnable().getItems() != null) {
            getSkinnable().getItems().removeListener(weakGridViewItemsListener);
        }

        if (getSkinnable().getItems() != null) {
            getSkinnable().getItems().addListener(weakGridViewItemsListener);
        }

        updateRowCount();
        flowRecreateCells();
        getSkinnable().requestLayout();
    }

    @Override
    void updateRowCount() {
        if (f == null)
            return;

        int oldCount = f.getCellCount();
        int newCount = getItemCount();

        if (newCount != oldCount) {
            f.setCellCount(newCount);
            flowRebuildCells();
        } else {
            flowReconfigureCells();
        }
        updateRows(newCount);
    }

    @Override protected void layoutChildren(double x, double y, double w, double h) {
        double x1 = getSkinnable().getInsets().getLeft();
        double y1 = getSkinnable().getInsets().getTop();
        double w1 = getSkinnable().getWidth() - (getSkinnable().getInsets().getLeft() + getSkinnable().getInsets().getRight());
        double h1 = getSkinnable().getHeight() - (getSkinnable().getInsets().getTop() + getSkinnable().getInsets().getBottom());

        f.resizeRelocate(x1, y1, w1, h1);
    }

    public ImprovedGridRow<T> createCell() {
        ImprovedGridRow<T> row = new ImprovedGridRow<>();
        row.updateGridView(getSkinnable());
        return row;
    }

    /**
     *  Returns the number of row needed to display the whole set of cells
     *  @return GridView row count
     */
    @Override
    public int getItemCount() {
        final ObservableList<?> items = getSkinnable().getItems();
        return items == null ? 0 : (int)Math.ceil((double)items.size() / computeMaxCellsInRow());
    }

    /**
     *  Returns the max number of cell per row
     *  @return Max cell number per row
     */
    public int computeMaxCellsInRow() {
        return Math.max((int) Math.floor(computeRowWidth() / computeCellWidth()), 1);
    }

    /**
     *  Returns the width of a row
     *  (should be GridView.width - GridView.Scrollbar.width)
     *  @return Computed width of a row
     */
    protected double computeRowWidth() {
        return getSkinnable().getWidth() - getVScrollbarWidth();
    }

    /**
     *  Returns the width of a cell
     *  @return Computed width of a cell
     */
    protected double computeCellWidth() {
        return getSkinnable().cellWidthProperty().doubleValue() + (getSkinnable().horizontalCellSpacingProperty().doubleValue() * 2);
    }

    protected double getVScrollbarWidth() {
        if(f!=null) {
            VirtualScrollBar vsb = getFieldValue(f, VirtualScrollBar.class, "vbar");
            return vsb!=null && vsb.isVisible() ? vsb.getWidth() : 0;
        }
        return 0;
    }

    protected void updateRows(int rowCount) {
        for (int i = 0; i < rowCount; i++) {
            ImprovedGridRow<T> row = f.getVisibleCell(i);
            if (row != null) {
                // FIXME hacky - need to better understand what this is about
                row.updateIndex(-1);
                row.updateIndex(i);
            }
        }
    }

    protected boolean areRowsVisible() {
        if (f == null)
            return false;

        if (f.getFirstVisibleCell() == null)
            return false;

        if (f.getLastVisibleCell() == null)
            return false;

        return true;
    }

    @Override protected double computeMinHeight(double height, double topInset, double rightInset, double bottomInset,
            double leftInset) {
        return 0;
    }
}