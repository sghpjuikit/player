/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.grid;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.IndexedCell;

class GridRow<T,F> extends IndexedCell<T>{

    /** {@link GridView} that this GridRow exists within */
    private final SimpleObjectProperty<GridView<T,F>> gridView = new SimpleObjectProperty<>(this, "gridView");

    public GridRow() {
        super();
        getStyleClass().add("grid-row");
    }

    @Override
    public void updateIndex(int i) {
        if(getIndex()!=i) forceUpdateIndex(i);
    }

    public void forceUpdateIndex(int i) {
        super.updateIndex(i);
        updateItem(null, i<0);
        updateSelected(i==getGridView().implGetSkin().selectedRI);
    }


    @Override
    protected GridRowSkin<T,F> createDefaultSkin() {
        return new GridRowSkin<>(this);
    }

    @SuppressWarnings("unchecked")
    public GridRowSkin<T,F> getSkinn() {
        return (GridRowSkin<T,F>)getSkin();
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