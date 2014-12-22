/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Table;

import java.util.List;
import java.util.function.UnaryOperator;
import static java.util.stream.Collectors.joining;
import java.util.stream.Stream;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import util.collections.KeyMap;
import static util.functional.FunctUtil.toIndexedStream;

/**
 *
 * @author Plutonium_
 */
public final class TableColumnInfo {
    
    public final KeyMap<String,ColumnInfo> columns;
    public UnaryOperator<String> nameKeyMapper = name -> name;
    
    private String nameKeyMap(String name) {
        return nameKeyMapper.apply(name);
    }
    
    public TableColumnInfo() {
        columns = new KeyMap<>(c->nameKeyMap(c.name));
//        columns = new KeyMap<>(c->nameKeyMapper.apply(c.name));
    }
    
    public TableColumnInfo(List<String> all_columns) {
        this();
        // add columns as visible
        toIndexedStream(all_columns)
                .map(p->new ColumnInfo(p._2,p._1,true,50))
                .forEach(columns::addE);
    }
    
    public TableColumnInfo(TableView<?> table) {
        this();
        // add visible columns
        toIndexedStream(table.getColumns())
                .map(p->new ColumnInfo(p._1,p._2))
                .forEach(columns::addE);
    }
    
    public TableColumnInfo(TableView<?> table, List<String> all_columns) {
        this(all_columns);
        update(table);
    }
    
    public void update(TableView<?> table) {
        KeyMap<String,ColumnInfo> old = new KeyMap<>(c->nameKeyMap(c.name));
        columns.clear();
        // add visible columns
        toIndexedStream(table.getColumns())
                .map(p->new ColumnInfo(p._1,p._2))
                .peek(old::removeE)
                .forEach(columns::addE);
        // add invisible columns
        int i = columns.size();
        old.stream()
                .map(p->new ColumnInfo(p.name, i+p.position, false, p.width))
                .forEach(columns::addE);
    }

    @Override
    public String toString() {
        return columns.stream().map(Object::toString).collect(joining(";"));
    }
    
    public static TableColumnInfo fromString(String s) {
        if("".equals(s)) return new TableColumnInfo();
        TableColumnInfo tci = new TableColumnInfo();
        Stream.of(s.split(";", 0)).map(ColumnInfo::fromString).forEach(tci.columns::addE);
        return tci;
    }
    
    
    
    public static class ColumnInfo implements Comparable<ColumnInfo>{
        public final String name;
        public final int position;
        public final boolean visible;
        public final double width;
        
        public ColumnInfo(String name, int position, boolean visible, double width) {
            this.name = name;
            this.position = position;
            this.visible = visible;
            this.width = width;
        }
        public ColumnInfo(int position, TableColumn c) {
//            this(c.getText(), position, c.isVisible(), c.getWidth());
            this(c.getText(), position, true, c.getWidth());
        }

        @Override
        public String toString() {
            return name + "," + position + "," + visible + "," + width;
        }
        
        public static ColumnInfo fromString(String str) {
            String[] s = str.split(",", 0);
            return new ColumnInfo(s[0], Integer.parseInt(s[1]), 
                    Boolean.valueOf(s[2]), Double.parseDouble(s[3]));
        }

        @Override
        public int compareTo(ColumnInfo o) {
            return Integer.compare(position, o.position);
        }
    }
}
///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package GUI.objects.Table;
//
//import java.util.ArrayList;
//import java.util.List;
//import static java.util.stream.Collectors.joining;
//import java.util.stream.Stream;
//import javafx.scene.control.TableColumn;
//import javafx.scene.control.TableView;
//import util.functional.FunctUtil;
//
///**
// *
// * @author Plutonium_
// */
//public class TableColumnInfo {
//    
//    public final List<ColumnInfo> columns;
//    
//    public TableColumnInfo() {
//        columns = new ArrayList();
//    }
//    
//    public TableColumnInfo(List<String> all_columns) {
//        this();
//        // add columns as visible
//        all_columns.stream().map(c->new ColumnInfo(c, true, 50))
//                            .forEach(columns::add);
//    }
//    
//    public TableColumnInfo(TableView<?> table) {
//        this();
//        // add visible columns
//        columns.addAll(FunctUtil.mapToList(table.getColumns(), ColumnInfo::new));
//    }
//    
//    public TableColumnInfo(TableView<?> table, List<String> all_columns) {
//        this(table);
//        // add invisible columns
//        all_columns.stream().filter(c->columns.stream().noneMatch(cc->cc.name.equals(c)))
//                            .map(c->new ColumnInfo(c, false, 50))
//                            .forEach(columns::add);
//    }
//    
//    public void update(TableView<?> table) {
//        List<ColumnInfo> old = new ArrayList(columns);
//        columns.clear();
//        // add visible columns
//        columns.addAll(FunctUtil.mapToList(table.getColumns(), ColumnInfo::new));
//        // remove all visible from old
//        table.getColumns().forEach(c1->old.removeIf(c->c.name.equals(c1.getText())));
//        // add invisible columns
//        columns.addAll(FunctUtil.mapToList(old, c->new ColumnInfo(c.name, false, c.width)));
//    }
//
//    @Override
//    public String toString() {
//        return columns.stream().map(Object::toString).collect(joining(";"));
//    }
//    
//    public static TableColumnInfo fromString(String s) {
//        if("".equals(s)) return new TableColumnInfo();
//        TableColumnInfo tci = new TableColumnInfo();
//        Stream.of(s.split(";", 0)).map(ColumnInfo::fromString).forEach(tci.columns::add);
//        return tci;
//    }
//    
//    
//    
//    public static class ColumnInfo {
//        public final String name;
//        public final boolean visible;
//        public final double width;
//        
//        public ColumnInfo(String name, boolean visible, double width) {
//            this.name = name;
//            this.visible = visible;
//            this.width = width;
//        }
//        public ColumnInfo(TableColumn c) {
//            this(c.getText(), c.isVisible(), c.getWidth());
//        }
//
//        @Override
//        public String toString() {
//            return name + "," + visible + "," + width;
//        }
//        
//        public static ColumnInfo fromString(String s) {
//            String[] is = s.split(",", 0);
//            return new ColumnInfo(is[0], Boolean.valueOf(is[1]), Double.parseDouble(is[2]));
//        }
//    }
//}
