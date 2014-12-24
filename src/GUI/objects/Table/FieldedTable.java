/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Table;

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
import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import util.Parser.ParserImpl.Parser;
import util.TODO;
import static util.TODO.Purpose.FUNCTIONALITY;
import static util.TODO.Purpose.PERFORMANCE_OPTIMIZATION;
import static util.Util.createmenuItem;
import util.access.FieldValue.FieldEnum;
import util.access.FieldValue.FieldedValue;
import static util.functional.FunctUtil.list;
import static util.functional.FunctUtil.listM;

/**
 *
 * @author Plutonium_
 */
public class FieldedTable <T extends FieldedValue<T,F>, F extends FieldEnum<T>> extends ImprovedTable<T> {
    
    
    private Function<F,ColumnInfo> colStateFact;
    private Callback<String,TableColumn<T,?>> colFact;
    private UnaryOperator<String> keyNameColMapper = name -> name;
    
    private TableColumnInfo columnState;
    private final Class<F> type;
    ContextMenu columnMenu;
    
    public FieldedTable(Class<F> type) {
        super();
        this.type = type;
        
        // 
//        createDefaultSkin();
    }
    
    public void setColumnFactory(Callback<F,TableColumn<T,?>> columnFactory) {
        this.colFact = name -> "#".equals(name) ? buildIndexColumn() : columnFactory.call(Parser.fromS(type, keyNameColMapper.apply(name)));
    }
    
    public Callback<String,TableColumn<T,?>> getColumnFactory() {
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
    
    public void setColumnVisible(String name, boolean v) {
        TableColumn<T,?> t = getColumn(name).orElse(null);
        if(v) {
            if(t==null) {
                t = colFact.call(name);
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

    @TODO(purpose = PERFORMANCE_OPTIMIZATION, note = "we always build the "
            + "invisible columns as well. This is wrong. But otherwise, the"
            + "table menu button would not list those columns and there would"
            + "be no way to actually make them visible. Generate own table"
            + "menu context menu to solve this.")
    public void setColumnState(TableColumnInfo state) {
        requireNonNull(state);
        
        List<TableColumn<T,?>> visibleColumns = new ArrayList();
        state.columns.stream().sorted().filter(c->c.visible).forEach(c->{
            // get or build column
            TableColumn tc = getColumn(c.name).orElse(colFact.call(c.name));
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
            F[] fields = type.getEnumConstants();
            List<ColumnInfo> l = list(fields,f->f.isTypeStringRepresentable(),colStateFact);
            defColInfo = new TableColumnInfo();
            defColInfo.nameKeyMapper = keyNameColMapper;
            defColInfo.columns.addAllE(l);
            // insert index column state
            defColInfo.columns.values().forEach(t->t.position++);
            defColInfo.columns.addE(new ColumnInfo("#", 0, true, USE_COMPUTED_SIZE));
            // leave sort order empty
            
            columnState = defColInfo;
            
            
            // build new table column menu
            List<MenuItem> mm = listM(defColInfo.columns.values(),c->createmenuItem(c.name, 
                    a->setColumnVisible(c.name, !isColumnVisible(c.name))));
            columnMenu = new ContextMenu();
            columnMenu.getItems().addAll(mm);
            // link table column menu
            Platform.runLater(()->{
                TableHeaderRow h = ((TableViewSkinBase)getSkin()).getTableHeaderRow();
                try {
                    // cornerRegion is the context menu button
                    Field f = TableHeaderRow.class.getDeclaredField("cornerRegion");
                    // they just wont let us...
                    f.setAccessible(true);
                    // link to our custom menu
                    Pane columnMenuButton = (Pane) f.get(h);
                         columnMenuButton.setOnMousePressed(e -> {
                            columnMenu.show(columnMenuButton, Side.BOTTOM, 0, 0);
                            e.consume();
                         });
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
        return getColumn(c->name.equals(c.getText()));
    }
    
    public Optional<TableColumn<T,?>> getColumn(F f) {
        return getColumn(f.toString());
    }
}