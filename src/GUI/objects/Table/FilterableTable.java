/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Table;

import GUI.objects.FilterGenerator.TableFilterGenerator;
import java.util.Collection;
import java.util.function.Predicate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.layout.Priority.ALWAYS;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import util.Util;
import util.access.FieldValue.FieldEnum;
import util.access.FieldValue.FieldedValue;

/**
 * 
 * Table with a search filter header that supports filtering with provided gui.
 *
 * @author Plutonium_
 */
public class FilterableTable<T extends FieldedValue<T,F>, F extends FieldEnum<T>> extends ImprovedTable<T> {
    
    private final ObservableList<T> allitems = FXCollections.observableArrayList();
    private final FilteredList<T> filtereditems = new FilteredList(allitems);
    private final SortedList<T> sortedItems = new SortedList(filtereditems);
    public final TableFilterGenerator<T,F> searchBox;
    final VBox root = new VBox(this);
    
    private boolean show_original_index;
    
    public FilterableTable(F initialVal) {
        searchBox = new TableFilterGenerator(filtereditems, initialVal);
        
        setItems(sortedItems);
        sortedItems.comparatorProperty().bind(comparatorProperty());
        VBox.setVgrow(this, ALWAYS);
        
        searchBox.setVisible(false);
        
        searchBox.addEventFilter(KEY_PRESSED, e -> {
            if(e.getCode()==ESCAPE) {
                searchBox.clear();
                setFilterVisible(false);
                e.consume();
            }
        });
        
        addEventFilter(KEY_PRESSED, e -> {
            KeyCode k = e.getCode();
            if(k==ESCAPE) {
                if(searchBox.isEmpty()) setFilterVisible(false);
                else searchBox.clear();
                e.consume();
            } else if (k.isDigitKey() || k.isLetterKey()){
                if(searchBox.isEmpty()) setFilterVisible(true);
                // e.consume(); // must not consume
            }
        });
    }
    
    /**
     * 
     * @return 
     */
    public TableFilterGenerator<T,F> getSearchBox() {
        return searchBox;
    }
    
    /**
     * 
     * @return 
     */
    public VBox getRoot() {
        return root;
    }
    
    public final ObservableList<T> getItemsRaw() {
        return allitems;
    }
    
    public final SortedList<T> getItemsSorted() {
        return sortedItems;
    }
    
    public final FilteredList<T> getItemsFiltered() {
        return filtereditems;
    }
    
    /**
     * Sets items to the table. If any filter is in effect, it will be aplied.
     * <p>
     * Do not use {@link #setItems(javafx.collections.ObservableList)} or 
     * {@code getItems().setAll(new_items)} . It will cause the filters stop
     * working. The first replaces the table item list (instance of {@link FilteredList}
     * which must not happen. The second would throw an exception as FilteredList
     * is not directly modifiable.
     * 
     * @param items 
     */
    public void setItemsRaw(Collection<T> items) {
        allitems.setAll(items);
    }
    
    /**
     * Equivalent to {@code (Predicate<T>) filtereditems.getPredicate();}
     * @return 
     */
    public Predicate<T> getFilterPredicate() {
        return (Predicate<T>) filtereditems.getPredicate();
    }
    
    public final void setFilterVisible(boolean v) {
        if(searchBox.isVisible()==v) return;
        
        if(!v) searchBox.clear();
        
        if(v) root.getChildren().setAll(searchBox,this);
        else root.getChildren().setAll(this);
        searchBox.setVisible(v);
        VBox.setVgrow(this, ALWAYS);
        
        // after gui changes, focus on filter so we type the search criteria
        if(v) searchBox.focus();
    }
    
/******************************************************************************/
    
    /** 
     * @param true shows item's index in the observable list - source of its
     * data. False will display index within filtered list. In other words false
     * will cause items to always be indexed from 1 to items.size. This has only
     * effect when filtering the table. 
     */
    public void setShowOriginalIndex(boolean val) {
        show_original_index = val;
        refreshColumn(columnIndex);
    }
    
    public boolean isShowOriginalIndex() {
        return show_original_index;
    }
    
    @Override
    protected Callback<TableColumn<T, Void>, TableCell<T, Void>> buildIndexColumnCellFactory() {
        return ( column -> new TableCell<T,Void>() {
            { 
                setAlignment(Pos.CENTER_RIGHT);
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText("");
                } else {
                    int index = show_original_index ? filtereditems.getSourceIndex(getIndex()) : getIndex();
                    setText((zero_pad ? index+1 : Util.zeroPad(index+1, getItems().size(),'0')) + ".");
                }
            }
        });
    }
    
    
}