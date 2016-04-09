/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package gui.objects.grid;

import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ScrollToEvent;
import javafx.scene.control.SkinBase;
import javafx.scene.control.skin.VirtualFlow;

import static util.type.Util.invokeMethodP1;

/**
 * Parent class to control skins whose contents are virtualized and scrollable.
 * This class handles the interaction with the VirtualFlow class, which is the
 * main class handling the virtualization of the contents of this container.
 */
public abstract class CustomVirtualContainerBase<C extends Control, I extends IndexedCell> extends SkinBase<C> {

    /** The virtualized container which handles the layout and scrolling of all the cells. */
    final VirtualFlow<I> flow;
    boolean rowCountDirty;

    /**
     *
     * @param control
     */
    public CustomVirtualContainerBase(final C control) {
        super(control);
        flow = createVirtualFlow();

        control.addEventHandler(ScrollToEvent.scrollToTopIndex(), event -> {
            // Fix for RT-24630: The row count in VirtualFlow was incorrect
            // (normally zero), so the scrollTo call was misbehaving.
            if (rowCountDirty) {
                // update row count before we do a scroll
                updateRowCount();
                rowCountDirty = false;
            }
            flow.scrollToTop(event.getScrollTarget());
        });
    }

    /**
     * Returns the total number of items in this container, including those
     * that are currently hidden because they are out of view.
     */
    abstract int getItemCount();

    abstract void updateRowCount();

    /** {@inheritDoc} */
    @Override protected void layoutChildren(double x, double y, double w, double h) {
        checkState();
    }

    /**
     * Enables skin subclasses to provide a custom VirtualFlow implementation,
     * rather than have VirtualContainerBase instantiate the default instance.
     */
    protected VirtualFlow<I> createVirtualFlow() {
        return new VirtualFlow<>();
    }

    double getMaxCellWidth(int rowsToCount) {
        return snappedLeftInset()
                + ((double)invokeMethodP1(VirtualFlow.class,flow,"getMaxCellWidth",int.class,rowsToCount))
                + snappedRightInset();
//        return snappedLeftInset() + flow.getMaxCellWidth(rowsToCount) + snappedRightInset();
    }

    double getVirtualFlowPreferredHeight(int rows) {
        double height = 1.0;

        for (int i = 0; i < rows && i < getItemCount(); i++) {
            height += ((double)invokeMethodP1(VirtualFlow.class,flow,"getCellLength",int.class,i));
        }

        return height + snappedTopInset() + snappedBottomInset();
    }

    void checkState() {
        if (rowCountDirty) {
            updateRowCount();
            rowCountDirty = false;
        }
    }
}