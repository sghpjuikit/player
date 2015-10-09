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
import javafx.scene.control.Skin;

import org.controlsfx.control.GridView;

class ImprovedGridRow<T> extends IndexedCell<T>{


    /**************************************************************************
     *
     * Constructors
     *
     **************************************************************************/

    /**
     *
     */
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

    /**
     * {@inheritDoc}
     */
    @Override protected Skin<?> createDefaultSkin() {
        return new ImprovedGridRowSkin<>(this);
    }



    /**************************************************************************
     *
     * Properties
     *
     **************************************************************************/

    /**
     * The {@link GridView} that this GridRow exists within.
     */
    public SimpleObjectProperty<GridView<T>> gridViewProperty() {
        return gridView;
    }
    private final SimpleObjectProperty<GridView<T>> gridView =
            new SimpleObjectProperty<>(this, "gridView"); //$NON-NLS-1$

    /**
     * Sets the {@link GridView} that this GridRow exists within.
     */
    public final void updateGridView(GridView<T> gridView) {
        this.gridView.set(gridView);
    }

    /**
     * Returns the {@link GridView} that this GridRow exists within.
     */
    public GridView<T> getGridView() {
        return gridView.get();
    }
}