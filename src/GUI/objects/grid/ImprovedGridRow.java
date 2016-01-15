/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.grid;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.IndexedCell;


class ImprovedGridRow<T> extends IndexedCell<T>{

    private final SimpleObjectProperty<ImprovedGridView<T>> gridView = new SimpleObjectProperty<>(this, "gridView");

    public ImprovedGridRow() {
        super();
        getStyleClass().add("grid-row"); //$NON-NLS-1$

        // we need to do this (or something similar) to allow for mouse wheel
        // scrolling, as the GridRow has to report that it is non-empty (which
        // is the second argument going into updateItem).
        indexProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                updateItem(null, getIndex() == -1);
            }
        });
    }

    @Override
    protected ImprovedGridRowSkin<T> createDefaultSkin() {
        return new ImprovedGridRowSkin<>(this);
    }

    public ImprovedGridRowSkin<T> getSkinn() {
        return (ImprovedGridRowSkin<T>)getSkin();
    }

    /** The {@link GridView} that this GridRow exists within. */
    public SimpleObjectProperty<ImprovedGridView<T>> gridViewProperty() {
        return gridView;
    }

    /**
     * Sets the {@link GridView} that this GridRow exists within.
     */
    public final void setGridView(ImprovedGridView<T> gridView) {
        this.gridView.set(gridView);
    }

    /**
     * Returns the {@link GridView} that this GridRow exists within.
     */
    public ImprovedGridView<T> getGridView() {
        return gridView.get();
    }
}