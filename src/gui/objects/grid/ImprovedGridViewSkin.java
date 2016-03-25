
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

import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import com.sun.javafx.scene.control.VirtualScrollBar;

import util.graphics.Util;

import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static util.Util.*;
import static util.graphics.Util.bgr;
import static util.graphics.Util.layStack;

public class ImprovedGridViewSkin<T> implements Skin<ImprovedGridView> {

    private class SkinDelegate extends CustomVirtualContainerBase<ImprovedGridView<T>,ImprovedGridRow<T>> {
        public SkinDelegate(ImprovedGridView<T> control) {
            super(control);
            registerChangeListener(control.cellHeightProperty(), e -> flowRecreateCells());
            registerChangeListener(control.cellWidthProperty(), e -> { updateRowCount(); flowRecreateCells(); });
            registerChangeListener(control.horizontalCellSpacingProperty(), e -> { updateRowCount(); flowRecreateCells(); });
            registerChangeListener(control.verticalCellSpacingProperty(), e -> flowRecreateCells());
            registerChangeListener(control.widthProperty(), e -> updateRowCount());
            registerChangeListener(control.heightProperty(), e -> updateRowCount());
            registerChangeListener(control.itemsProperty(), e -> updateGridViewItems());
            registerChangeListener(control.cellFactoryProperty(), e -> flowRecreateCells());
            registerChangeListener(control.parentProperty(), e -> {
                if (getSkinnable().getParent() != null && getSkinnable().isVisible())
                    getSkinnable().requestLayout();
            });
        }

        @Override
        int getItemCount() {
            return ImprovedGridViewSkin.this.getItemCount();
        }

        @Override
        void updateRowCount() {
            ImprovedGridViewSkin.this.updateRowCount();
        }
    }

    private final SkinDelegate skin;
    private final VBox root;
    private final WeakListChangeListener<T> weakGridViewItemsListener = new WeakListChangeListener<>(change -> {
        updateRowCount();
        getSkinnable().requestLayout();
    });
    private final VirtualFlow<ImprovedGridRow<T>> f; // accesses superclass' flow field, dont name "flow"!

    @SuppressWarnings("unchecked")
    public ImprovedGridViewSkin(ImprovedGridView<T> control) {
        skin = new SkinDelegate(control);
        f = getFieldValue(skin,VirtualFlow.class,"flow"); // make flow field accessible
        f.setId("virtual-flow"); //$NON-NLS-1$
        f.setPannable(false);
        f.setVertical(true);
        f.setFocusTraversable(getSkinnable().isFocusTraversable());
        f.setCellFactory(f -> ImprovedGridViewSkin.this.createCell());

        updateGridViewItems();

        Pane l = new StackPane();
             l.setMinHeight(25);
        root = Util.layHeaderTop(10, Pos.TOP_RIGHT, l, f);
        root.getChildren().remove(0);

        updateRowCount();

        // selection
        f.addEventHandler(KEY_PRESSED, e -> {
            KeyCode c = e.getCode();
            if(c.isNavigationKey()) {
                if(c==KeyCode.UP || c==KeyCode.KP_UP) selectUp();
                if(c==KeyCode.DOWN || c==KeyCode.KP_DOWN) selectDown();
                if(c==KeyCode.LEFT || c==KeyCode.KP_LEFT) selectLeft();
                if(c==KeyCode.RIGHT || c==KeyCode.KP_RIGHT) selectRight();
                if(c==KeyCode.PAGE_DOWN) selectDown();
                if(c==KeyCode.PAGE_UP) selectUp();
                if(c==KeyCode.HOME) selectFirst();
                if(c==KeyCode.END) selectLast();
                e.consume();
            }
        });
    }

    public VirtualFlow<ImprovedGridRow<T>> getFlow() {
        return f;
    }

    @Override
    public ImprovedGridView<T> getSkinnable() {
        return skin.getSkinnable();
    }

    @Override
    public Node getNode() {
        return root;
    }

    @Override
    public void dispose() {
        skin.dispose();
    }

    public void updateGridViewItems() {
        if (getSkinnable().getItems() != null) {
            getSkinnable().getItems().removeListener(weakGridViewItemsListener);
            getSkinnable().getItems().addListener(weakGridViewItemsListener);
        }

        updateRowCount();
        flowRecreateCells();
        getSkinnable().requestLayout();
    }

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

    public ImprovedGridRow<T> createCell() {
        ImprovedGridRow<T> row = new ImprovedGridRow<>();
        row.setGridView(getSkinnable());
        return row;
    }

    /**
     *  Returns the number of row needed to display the whole set of cells
     *  @return GridView row count
     */
    public int getItemCount() {
        final ObservableList<?> items = getSkinnable().getItems();
        return items == null ? 0 : (int)Math.ceil((double)items.size() / computeMaxCellsInRow());
    }

    /**
     *  Returns the max number of cell per row
     *  @return Max cell number per row
     */
    public int computeMaxCellsInRow() {
        double gap = getSkinnable().horizontalCellSpacingProperty().doubleValue();
        return Math.max((int) Math.floor((computeRowWidth()+gap) / computeCellWidth()), 1);
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
        return f!=null && f.getFirstVisibleCell()!=null && f.getLastVisibleCell()!=null;
    }

    private void flowRecreateCells() {
        invokeMethodP0(VirtualFlow.class,f,"recreateCells");
    }

    private void flowRebuildCells() {
        invokeMethodP0(VirtualFlow.class,f,"rebuildCells");
    }

    private void flowReconfigureCells() {
        invokeMethodP0(VirtualFlow.class,f,"reconfigureCells");
    }

/********************************************* FILTER *********************************************/



/******************************************** SELECTION *******************************************/

    private static final PseudoClass SELECTED_PC = getPseudoClass("selected");
    private int selectedI = -1;
    private ImprovedGridRow<T> selectedR = null;
    private ImprovedGridCell<T> selectedC = null;

    void selectRight() {
        select(selectedI+1);
    }

    void selectLeft() {
        select(selectedI-1);
    }

    void selectUp() {
        select(selectedI-computeMaxCellsInRow());
    }

    void selectDown() {
        select(selectedI+computeMaxCellsInRow());
    }

    void selectFirst() {
        select(0);
    }

    void selectLast() {
        select(getSkinnable().getItems().size()-1);
    }

    void selectNone() {
        select(-1);
    }

    void select(ImprovedGridCell<T> c) {
        if(c==null || c.getItem()==null) selectNone();
        else select(getSkinnable().getItems().indexOf(c.getItem()));
    }

    void select(int i) {
        if(f==null) return;

        int imin = -1;
        int imax = getSkinnable().getItems().size()-1;
        i = clip(imin,i,imax);
        if(i==selectedI) return;

        // unselect
        if(selectedC!=null) selectedC.pseudoClassStateChanged(SELECTED_PC, false);
        if(selectedR!=null) selectedR.pseudoClassStateChanged(SELECTED_PC, false);
        if(selectedC!=null) selectedC.updateSelected(false);
        if(selectedR!=null) selectedR.updateSelected(false);
        getSkinnable().selectedRow.set(null);
        getSkinnable().selectedItem.set(null);
        selectedC = null;
        selectedR = null;

        if(i==-1) return;

        // find index
        int rows = getItemCount();
        int cols = computeMaxCellsInRow();
        int row = i/cols;
        int col = i%cols;

        if(row<0 || row>rows) return;

        // show row & cell to select
        ImprovedGridRow<T> fsc = f.getFirstVisibleCell();
        ImprovedGridRow<T> lsc = f.getLastVisibleCell();
        if(fsc.getIndex() > row || row > lsc.getIndex())
        f.scrollTo(row);

        // find row & cell to select
        ImprovedGridRow<T> r = f.getCell(row);
        ImprovedGridCell<T> c = r.getSkinn().getCellAtIndex(col);
        if(r==null || c==null) return;

        selectedI = i;
        if(c!=null) c.pseudoClassStateChanged(SELECTED_PC, true);
        if(r!=null) r.pseudoClassStateChanged(SELECTED_PC, true);
        selectedR = r;
        selectedC = c;
        selectedC.requestFocus();
        selectedR.updateSelected(true);
        selectedC.updateSelected(true);
        getSkinnable().selectedRow.set(r.getItem());
        getSkinnable().selectedItem.set(c.getItem());
    }
}