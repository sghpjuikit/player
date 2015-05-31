/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Table;

import GUI.objects.ContextMenu.CheckMenuItem;
import GUI.objects.Table.TableColumnInfo.ColumnInfo;
import com.sun.javafx.scene.control.skin.TableHeaderRow;
import com.sun.javafx.scene.control.skin.TableViewSkinBase;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javafx.application.Platform.runLater;
import static javafx.geometry.Side.BOTTOM;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.Tooltip;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import static util.Util.getEnumConstants;
import util.access.FieldValue.FieldEnum;
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
    
    
    public void setColumnFactory(FunctionC<F,TableColumn<T,?>> columnFactory) {
        colFact = f -> f==null ? columnIndex : columnFactory.andApply(c -> c.setUserData(f)).call(f);
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
    
    public boolean isColumnVisible(String name) {
        return getColumn(name).isPresent();
    }
    
    private F nameToF(String name) {
        return "#".equals(name) ? null : Parser.fromS(type, keyNameColMapper.apply(name));
    }
    
    public void setColumnVisible(String name, boolean v) {
        TableColumn<T,?> t = getColumn(name).orElse(null);
        F f = nameToF(name);
        if(v) {
            if(t==null) {
                t = colFact.call(f);
                t.setPrefWidth(columnState.columns.get(name).width);
                t.setVisible(v);
                getColumns().add(t);
            } else {
                t.setVisible(v);
            }
        } else if(!v && t!=null) {
            getColumns().remove(t);
        }
    }

    public void setColumnState(TableColumnInfo state) {
        requireNonNull(state);
        
        List<TableColumn<T,?>> visibleColumns = new ArrayList();
        state.columns.stream().sorted().filter(c->c.visible).forEach(c->{
            // get or build column
            TableColumn tc = getColumn(c.name).orElse(colFact.call(nameToF(c.name)));
            // set width
            tc.setPrefWidth(c.width);
            // set visibility
            tc.setVisible(c.visible);
            // set position (automatically, because we sorted the input)
            visibleColumns.add(tc);
        });
        // restore all at once => 1 update
        getColumns().setAll(visibleColumns);
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
                        CheckMenuItem m = new CheckMenuItem(c.name,c.visible,v -> setColumnVisible(c.name, v));
                        F f = nameToF(c.name);
                        String desc = f==null ? "" : f.description();
                        if(!desc.isEmpty()) Tooltip.install(m.getGraphic(), new Tooltip(desc));
                        return m;
                    })
                    .forEach(columnVisibleMenu.getItems()::add);
            // update column menu check icons every time we show it
            // the menu is rarely shown + no need to update it any other time
            columnVisibleMenu.setOnShowing(e -> columnVisibleMenu.getItems()
                            .filtered(i -> i instanceof CheckMenuItem)
                            .forEach(i -> ((CheckMenuItem)i).selected.set(isColumnVisible(i.getText()))));
            // link table column button to our menu instead of an old one
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
    
    public Optional<TableColumn<T,?>> getColumn(String name) {
        return getColumn(c->keyNameColMapper.apply(name).equals(keyNameColMapper.apply(c.getText())));
    }
    
    public Optional<TableColumn<T,?>> getColumn(F f) {
        return getColumn(f.toString());
    }


    public void refreshColumn(F f) {
        getColumn(f).ifPresent(this::refreshColumn);
    }
    public void refreshColumn(String name) {
        getColumn(name).ifPresent(this::refreshColumn);
    }
    public void refreshColumnAny() {
        if(!getColumns().isEmpty()) refreshColumn(getColumns().get(0));
    }
    
/************************************* SORT ***********************************/
    
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
    }
}