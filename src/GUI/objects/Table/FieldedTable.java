/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Table;

import GUI.objects.Table.TableColumnInfo.ColumnInfo;
import java.util.List;
import java.util.function.Supplier;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import util.access.FieldValue.FieldEnum;
import util.access.FieldValue.FieldedValue;
import static util.functional.FunctUtil.forEachIndexed;

/**
 *
 * @author Plutonium_
 */
public class FieldedTable <T extends FieldedValue<T,F>, F extends FieldEnum<T>> extends ImprovedTable<T> {
    
    
    private Supplier<TableColumnInfo> defaultColumnStateFacory;
    private Callback<String,TableColumn<T,?>> columnFactory;
    
    
    public FieldedTable() {
        super();
        
        
    }
    
    public void setColumnFactory(Callback<String,TableColumn<T,?>> columnFactory) {
        this.columnFactory = name -> "#".equals(name) ? buildIndexColumn() : columnFactory.call(name);
    }
    
    public void setDefaultColumnStateFacory(Supplier<List<ColumnInfo>> defaultColumnStateFacory) {
        this.defaultColumnStateFacory = () -> {
            TableColumnInfo ci = new TableColumnInfo();
            ci.columns.addE(new ColumnInfo("#", 0, true, USE_PREF_SIZE));
            forEachIndexed(defaultColumnStateFacory.get(), (j,c) -> 
                    ci.columns.addE(new ColumnInfo(c.name, 1+c.position, c.visible, c.width)));
            return ci;
        };
    }
    
/******************************************************************************/
    
    private void buildColumns() {
        
    }
    
}
