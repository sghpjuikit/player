/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Table;

import GUI.objects.FilterGenerator.TableFilterGenerator;
import java.util.Collection;
import java.util.Comparator;
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
import util.TODO;
import static util.TODO.Purpose.BUG;
import static util.TODO.Severity.MEDIUM;
import static util.Util.zeroPad;
import util.access.FieldValue.FieldEnum;
import util.access.FieldValue.FieldedValue;
import static util.functional.FunctUtil.cmpareBy;

/**
 * 
 * Table with a search filter header that supports filtering with provided gui.
 *
 * @author Plutonium_
 */
public class FilteredTable<T extends FieldedValue<T,F>, F extends FieldEnum<T>> extends FieldedTable<T,F> {
    
    private ObservableList<T> allitems = FXCollections.observableArrayList();
    private FilteredList<T> filtereditems = new FilteredList(allitems);
    private SortedList<T> sortedItems = new SortedList(filtereditems);
    public final TableFilterGenerator<T,F> searchBox;
    final VBox root = new VBox(this);
    
    private boolean show_original_index;
    
    
    public FilteredTable(F initialVal) {
        super((Class<F>)initialVal.getClass());
        
        searchBox = new TableFilterGenerator(filtereditems, initialVal);
        
        setItems(sortedItems);
        sortedItems.comparatorProperty().bind(comparatorProperty());
        VBox.setVgrow(this, ALWAYS);
        
        searchBox.setVisible(false);
        
        // close search box on ESCAPE when focused
        searchBox.addEventFilter(KEY_PRESSED, e -> {
            if(e.getCode()==ESCAPE) {
                // use this to hide seach box on single ESC stroke
                // searchBox.clear();
                // setFilterVisible(false);
                // or clear filter on 1st, hide on 2nd
                if(searchBox.isEmpty()) setFilterVisible(false);
                else searchBox.clear();
                e.consume();
            }
        });
        
        // using EventFilter would cause ignoring first key stroke when setting
        // search box visible
        addEventHandler(KEY_PRESSED, e -> {
            if(e.isAltDown() || e.isControlDown() || e.isShiftDown()) return;
            KeyCode k = e.getCode();
            // close search box on ESCAPE when not focused
            if(k==ESCAPE) {
                if(searchBox.isEmpty()) setFilterVisible(false);
                else searchBox.clear();
                e.consume();
            // open search box when typing (and NOT consume the key stroke)
            } else if (k.isDigitKey() || k.isLetterKey()){
                if(searchBox.isEmpty()) setFilterVisible(true);
                // must not consume event
            }
        });
    }
    
    /** @return the table filter */
    public TableFilterGenerator<T,F> getSearchBox() {
        return searchBox;
    }
    
    /** The root is a container for this table and the filter. Use the root instead
     * of this table when attaching it to the scene graph.
     * @return the root of this table */
    public VBox getRoot() {
        return root;
    }
    
    /** Return the items assigned to this this table. Includes the filtered out items. 
     * <p>
     * This list can be modified, but it is recommended to use {@link #setItemsRaw(java.util.Collection)}
     * to change the items in the table.
     */
    public final ObservableList<T> getItemsRaw() {
        return allitems;
    }
    
    /** Return the items assigned to this this table as sorted list. The sorted
     *  list wraps the raw list. Use to apply sorting operations on the table. 
     * <p>
     * Do not modify the contents of this list.
     */
    public final SortedList<T> getItemsSorted() {
        return sortedItems;
    }
    
    /** Return the items assigned to this this table as filtered list. The filtered
     *  list wraps the sorted list. Use to apply filtering operations on the table.
     * <p>
     * Do not modify the contents of this list. 
     */
    public final FilteredList<T> getItemsFiltered() {
        return filtereditems;
    }
    
    /**
     * Sets items to the table. If any filter is in effect, it will be applied.
     * <p>
     * Do not use {@link #setItems(javafx.collections.ObservableList)} or 
     * {@code getItems().setAll(new_items)} . It will cause the filters to stop
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
    public final void setShowOriginalIndex(boolean val) {
        show_original_index = val;
        refreshColumn(columnIndex);
    }
    
    public final boolean isShowOriginalIndex() {
        return show_original_index;
    }
    
    /** 
     * Indexes range from 1 to n, where n can differ when filter is applied.
     * Equivalent to: {@code isShowOriginalIndex ? getItemsRaw().size() : getItems().size(); }
     * @return last index */
    public final int getLastIndex() {
        return show_original_index ? allitems.size() : getItems().size();
    }
    
    @Override
    @TODO(purpose = BUG, severity = MEDIUM, note = "IndexOutOfBounds during filter & item operations")
    protected Callback<TableColumn<T, Void>, TableCell<T, Void>> buildIndexColumnCellFactory() {
        return ( column -> new TableCell<T,Void>() {
            { 
                setAlignment(Pos.CENTER_RIGHT);
            }
            @Override 
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty)
                    setText(null);
                else {
                    int j = getIndex();
                    String txt;
                    if(zero_pad) {
                        int i = show_original_index ? filtereditems.getSourceIndex(j) : j;      // BUG HERE
                        txt = zeroPad(i+1, getLastIndex(), '0');
                    } else
                        txt = String.valueOf(j+1);
                    
                    setText( txt + ".");
                }
            }
        });
    }
    
/************************************* SORT ***********************************/
    
    /** {@inheritDoc} */
    @Override
    public void sortBy(F field) {
        getSortOrder().clear();
        allitems.sort(cmpareBy(p -> (Comparable) p.getField(field)));
    }
    
    /***
     * Sorts items using provided comparator. Any sort order is cleared.
     * @param comparator 
     */
    public void sort(Comparator<T> comparator) {
        getSortOrder().clear();
        allitems.sorted(comparator);
    }
}