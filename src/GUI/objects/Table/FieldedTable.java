/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.Table;

import com.sun.javafx.scene.control.skin.TableHeaderRow;
import com.sun.javafx.scene.control.skin.TableViewSkinBase;
import gui.objects.ContextMenu.SelectionMenuItem;
import gui.objects.Table.TableColumnInfo.ColumnInfo;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javafx.application.Platform.runLater;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.geometry.Pos;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.geometry.Side.BOTTOM;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.SortType;
import static javafx.scene.control.TableColumn.SortType.ASCENDING;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import static util.Util.getEnumConstants;
import util.access.FieldValue.FieldEnum;
import util.access.FieldValue.FieldEnum.ColumnField;
import util.access.FieldValue.FieldedValue;
import util.dev.TODO;
import static util.dev.TODO.Purpose.FUNCTIONALITY;
import static util.functional.Util.*;
import util.functional.functor.FunctionC;
import util.parsing.Parser;

/**
 * Table for {@link FieldedValue}. This facilitates column creation, sorting and
 * potentially additional features (e.g. filtering - {@link FilteredTable}.
 * <p>
 * Supports column state serialization and deserialization - it is possible to
 * restore previous state. This includes column order, column names, sort order, 
 * and column widths.
 * <p>
 * Nested columns are not supported.
 * <p>
 * The columns use userData property to store the field F for identification.
 * Use {@link #setUserData(java.lang.Object)}
 * to obtain the exact F field the column represents. Every column will return
 * a value except for index column, which returns null. Never use 
 * {@link #getUserData()} on the columns or column lookup will break.
 * <p>
 * Only visible columns are built.
 * <p>
 * Has redesigned menu for column visibility. The table header button opening it is
 * hidden by default and the menu can also be shown by right click on the table
 * header.
 * <p>

 * @param <T> type of element in the table, must be aware of its fields
 * @param <F> field of the T to access the fields

 * @author Plutonium_
 */
public class FieldedTable <T extends FieldedValue<T,F>, F extends FieldEnum<T>> extends ImprovedTable<T> {
    
    private Function<F,ColumnInfo> colStateFact;
    private FunctionC<F,TableColumn<T,?>> colFact;
    private UnaryOperator<String> keyNameColMapper = name -> name;
    
    private TableColumnInfo columnState;
    private final Class<F> type;
    public ContextMenu columnVisibleMenu;
    
    
    public FieldedTable(Class<F> type) {
        super();
        if(!type.isEnum()) throw new IllegalArgumentException("Fields must be an enum");
        this.type = type;
        
        // install comparator updating part I
        getSortOrder().addListener((Change<?> c) -> updateComparator(c));
        
        // show the column control menu on right click ( + hide if shown )
        addEventHandler(MOUSE_CLICKED, e -> {
            if (e.getButton()==SECONDARY && e.getY()<getTableHeaderHeight())
                columnVisibleMenu.show(this, e.getScreenX(), e.getScreenY());
            
            else if(columnVisibleMenu.isShowing()) columnVisibleMenu.hide();
        });

        // column control menu button not needed now (but still optionally usable)
        setTableMenuButtonVisible(false);
    }
    
    /** 
     * Returns all fields of this table. The fields are all values of the
     * field enum that are string representable.
     */
    public List<F> getFields() {
        return filter(getEnumConstants(type), f->f.isTypeStringRepresentable());
    }
    
    /**
     * Similar to {@link #getFields()}, but includes fields that are not derived
     * from the value. Such as index column.
     */
    public List<FieldEnum<? super F>> getFieldsC() {
        List<FieldEnum<? super F>> l = filter(getEnumConstants(type), f->f.isTypeStringRepresentable());
        l.addAll(filter(getEnumConstants(ColumnField.class), f->f.isTypeStringRepresentable()));
        return l;
    }
    
    public void setColumnFactory(FunctionC<F,TableColumn<T,?>> columnFactory) {
        colFact = f -> {
            TableColumn<T,?> c = f==null ? columnIndex : columnFactory.call(f);
            c.setUserData(f==null ? ColumnField.INDEX : f);
            c.setSortable(f!=null);
            return c;
        };
    }
    
    public Callback<F,TableColumn<T,?>> getColumnFactory() {
        return colFact;
    }
    
    public void setColumnStateFacory(Function<F,ColumnInfo> columnStateFactory) {
        colStateFact = columnStateFactory;
    }
    
    public Function<F,ColumnInfo> getColumnStateFactory() {
        return colStateFact;
    }
    
    public void setkeyNameColMapper(UnaryOperator<String> columnNametoKeyMapper) {
        keyNameColMapper = columnNametoKeyMapper;
    }
    
    public boolean isColumnVisible(FieldEnum<? super T> f) {
        return getColumn(f).isPresent();
    }
    
    public void setColumnVisible(FieldEnum<? super T> f, boolean v) {
        TableColumn<T,?> c = getColumn(f).orElse(null);
        if(v) {
            if(c==null) {
                c = f == ColumnField.INDEX ? columnIndex : colFact.call((F)f);
                c.setPrefWidth(columnState.columns.get(f.name()).width);
                c.setVisible(v);
                getColumns().add(c);
            } else {
                c.setVisible(v);
            }
        } else if(!v && c!=null) {
            getColumns().remove(c);
        }
    }

    public void setColumnState(TableColumnInfo state) {
        requireNonNull(state);
        
        List<TableColumn<T,?>> visibleColumns = new ArrayList();
        state.columns.stream().sorted().filter(c->c.visible).forEach(c->{
            // get or build column
            TableColumn tc = getColumn(nameToCF(c.name)).orElse(colFact.call(nameToF(c.name)));
            // set width
            tc.setPrefWidth(c.width);
            // set visibility
            tc.setVisible(c.visible);
            // set position (automatically, because we sorted the input)
            visibleColumns.add(tc);
        });
        // restore all at once => 1 update
        getColumns().setAll(visibleColumns);System.out.println("DESERIALIZING");
        // restore sort order
        state.sortOrder.toTable(this);
    }
    
    public TableColumnInfo getColumnState() {
        columnState.update(this);
        return columnState;
    }
    
    private TableColumnInfo defColInfo;
    @TODO(purpose = FUNCTIONALITY, note = "menu needs to be checked menu. However"
            + "rater than building it from scratch, get rid of the reflection and"
            + "make a skin that does this natively. Not sure what is better option here")
    public TableColumnInfo getDefaultColumnInfo() {
        if(defColInfo==null) {
            // generate column states
            List<ColumnInfo> colinfos = map(getFields(),colStateFact);
            defColInfo = new TableColumnInfo();
            defColInfo.nameKeyMapper = keyNameColMapper;
            defColInfo.columns.addAll(colinfos);
            // insert index column state
            defColInfo.columns.forEach(t->t.position++);
            defColInfo.columns.add(new ColumnInfo("#", 0, true, USE_COMPUTED_SIZE));
            // leave sort order empty
            
            columnState = defColInfo;
            
            
            // build new table column menu
            columnVisibleMenu = new ContextMenu();
            defColInfo.columns.streamV()
                    .sorted(by(c->c.name))
                    .map(c-> {
                        FieldEnum<? super T> f =  nameToCF(c.name);
                        SelectionMenuItem m = new SelectionMenuItem(c.name,c.visible,v -> setColumnVisible(f, v));
                        String d = f.description();
                        if(!d.isEmpty()) Tooltip.install(m.getGraphic(), new Tooltip(d));
                        return m;
                    })
                    .forEach(columnVisibleMenu.getItems()::add);
            // update column menu check icons every time we show it
            // the menu is rarely shown + no need to update it any other time
            columnVisibleMenu.setOnShowing(e -> columnVisibleMenu.getItems()
                            .filtered(i -> i instanceof SelectionMenuItem)
                            .forEach(i -> ((SelectionMenuItem)i).selected.set(isColumnVisible(nameToCF(i.getText())))));
            
            // link table column button to our menu instead of an old one
            // we need to delay this because the graphics is not yet ready
            runLater(()->{
                TableHeaderRow h = ((TableViewSkinBase)getSkin()).getTableHeaderRow();
                try {
                    // cornerRegion is the context menu button, use reflection
                    Field f = TableHeaderRow.class.getDeclaredField("cornerRegion");
                    // they just wont let us...
                    f.setAccessible(true);
                    // link to our custom menu
                    Pane columnB = (Pane) f.get(h);
                         columnB.setOnMousePressed(e -> columnVisibleMenu.show(columnB, BOTTOM, 0, 0));
                    f.setAccessible(false);
                } catch (Exception ex) {
                    Logger.getLogger(FieldedTable.class.getName()).log(Level.SEVERE, null, ex);
                }

                // install comparator updating part II
                // we need this because sort order list changes dont reflect
                // every sort change (when only ASCENDING-DESCENDING is changed
                // theres no list change event. 
                h.setOnMouseReleased(this::updateComparator);
                h.setOnMouseClicked(this::updateComparator);
            });
            
        }
        return defColInfo;
    }
    
    public Optional<TableColumn<T,?>> getColumn(Predicate<TableColumn<T,?>> filter) {
        for(TableColumn t : getColumns())
            if(filter.test(t)) {
                return Optional.of(t);
            }
        return Optional.empty();
    }
    
    public Optional<TableColumn<T,?>> getColumn(FieldEnum<? super T> f) {
        return getColumn(c -> c.getUserData()==f);
    }


    public void refreshColumn(FieldEnum<? super T> f) {
        getColumn(f).ifPresent(this::refreshColumn);
    }
    public void refreshCoumns() {
        if(!getColumns().isEmpty()) refreshColumn(getColumns().get(0));
    }
    
/************************************* SORT ***********************************/
    
    /**
     * Comparator for ordering items reflecting this table's sort order.
     * Read only, changing value will have no effect.
     */
    public final ObjectProperty<Comparator<T>> itemsComparator = new SimpleObjectProperty<>((a,b)->0);
    
    /**
     * Sorts the items by the field. Sorting does not operate on table's sort
     * order and is applied to items backing the table. Any sort order of 
     * the table will be removed.
     * <p>
     * This is not a programmatic equivalent of sorting the table manually by
     * clicking on their header (which operates through sort order).
     * <p>
     * Works even when field's respective column is invisible.
     * <p>
     * Note, that the field must support sorting - return Comparable type.
     * 
     * @param field 
     */
    public void sortBy(F field) {
        getSortOrder().clear();
        getItems().sort(by(p -> (Comparable) p.getField(field)));
    }
    
    /**
     * Sorts the items by the column.
     * Same as {@link #sortBy(javafx.scene.control.TableColumn, 
     * javafx.scene.control.TableColumn.SortType)}, but uses 
     * {@link #getColumn(util.access.FieldValue.FieldEnum)} for column lookup.
     */
    public void sortBy(F field, SortType type) {
        getColumn(field).ifPresent(c -> sortBy(c, type));
        updateComparator(null);
    }
    
/******************************** CELL FACTORY ********************************/
    
    /** 
     * Use as cell factory for columns created in column factory.
     * <p>
     * This cell factory
     * <ul>
     * <li> sets text using {@link FieldEnum#toS(java.lang.Object, java.lang.String)}
     * <li> sets alignment to CENTER_LEFT for Strings and CENTER_RIGHT otherwise
     * </ul>
     */
    public TableCell<T,?> buildDefaultCell(F f) {
        Pos a = f.getType().equals(String.class) ? CENTER_LEFT : CENTER_RIGHT;
        TableCell<T,Object> cell = new TableCell<T,Object>(){
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : f.toS(item,""));
            }
        };
        cell.setAlignment(a);
        return cell;
    } 
    
/*********************************** PRIVATE **********************************/
    
    // sort order -> comparator, never null
    private void updateComparator(Object ignored) {
        Comparator<T> c = getSortOrder().stream().map(column -> {
                F f = (F) column.getUserData();
                int type = column.getSortType()==ASCENDING ? 1 : -1;
                return (Comparator<T>)(m1,m2) -> type*((Comparable)m1.getField(f)).compareTo((m2.getField(f)));
            })
            .reduce(Comparator::thenComparing).orElse((a,b)->0);
        itemsComparator.setValue(c);
    }
    
    private F nameToF(String name) {
        return ColumnField.INDEX.name().equals(name) ? null : Parser.fromS(type, keyNameColMapper.apply(name));
    }
    private FieldEnum<? super T> nameToCF(String name) {
        return ColumnField.INDEX.name().equals(name) ? ColumnField.INDEX : nameToF(name);
    }
}