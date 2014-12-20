/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Table;

import java.util.ArrayList;
import java.util.List;
import static java.util.stream.Collectors.joining;
import java.util.stream.Stream;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import util.functional.FunctUtil;

/**
 *
 * @author Plutonium_
 */
public class TableColumnInfo {
    
    public final List<ColumnInfo> columns;
    
    public TableColumnInfo() {
        columns = new ArrayList();
    }
    
    public TableColumnInfo(TableView<?> table) {
        this();
        columns.addAll(FunctUtil.mapToList(table.getColumns(), ColumnInfo::new));
    }

    @Override
    public String toString() {
        return columns.stream().map(Object::toString).collect(joining(";"));
    }
    
    public static TableColumnInfo fromString(String s) {
        if("".equals(s)) return new TableColumnInfo();
        TableColumnInfo tci = new TableColumnInfo();
        Stream.of(s.split(";", 0)).map(ColumnInfo::fromString).forEach(tci.columns::add);
        return tci;
    }
    
    
    
    public static class ColumnInfo {
        public final String name;
        public final boolean visible;
        public final double width;
        
        public ColumnInfo(String name, boolean visible, double width) {
            this.name = name;
            this.visible = visible;
            this.width = width;
        }
        public ColumnInfo(TableColumn c) {
            this(c.getText(), c.isVisible(), c.getWidth());
        }

        @Override
        public String toString() {
            return name + "," + visible + "," + width;
        }
        
        public static ColumnInfo fromString(String s) {
            String[] is = s.split(",", 0);
            return new ColumnInfo(is[0], Boolean.valueOf(is[1]), Double.parseDouble(is[2]));
        }
    }
}
