/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.FilterGenerator;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuple3;
import utilities.Parser.ParserImpl.Parser;
import utilities.Util;

/**
 *
 * @author Plutonium_
 */
public class FilterGenerator<T> extends HBox {
    
    ComboBox<Tuple3<String,Class,T>> classCB = new ComboBox();
    ComboBox<Tuple2<String,BiPredicate>> filterCB = new ComboBox();
    TextField valueF = new TextField();
    
    private BiConsumer<Predicate<Object>,T> onFilterChange;
    private Callback<Class,List<Tuple2<String,BiPredicate>>> predicateSupplier;
    
    private Class type;
    
    public Predicate<Object> predicate;
    public T val;
    
    public FilterGenerator() {
        // initialize gui
        getChildren().addAll(classCB, filterCB, valueF);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(1));
        
        // generate predicates on change
        valueF.textProperty().addListener((o,ob,nv) -> generatePredicate(filterCB.getValue()==null ? null : filterCB.getValue()._2, nv, classCB.getValue()==null ? null : classCB.getValue()._3));
        filterCB.valueProperty().addListener((o,ov,nv) -> generatePredicate(nv==null ? null : nv._2, valueF.getText(), classCB.getValue()==null ? null : classCB.getValue()._3));

        // use dynamic toString() when populating combobox values
        classCB.setCellFactory( view -> {
            return new ListCell<Tuple3<String,Class,T>>(){
                @Override
                protected void updateItem(Tuple3<String,Class,T> item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item._1);
                }
            };
        });
        classCB.setButtonCell(classCB.getCellFactory().call(null));
        filterCB.setCellFactory( view -> {
            return new ListCell<Tuple2<String,BiPredicate>>(){
                @Override
                protected void updateItem(Tuple2<String,BiPredicate> item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item._1);
                }
            };
        });
        filterCB.setButtonCell(filterCB.getCellFactory().call(null));
        
        
        // fire filter changes when value changes
        classCB.valueProperty().addListener((o,ov,nv) -> setClass(nv._2));
    }
    
    /**
     * Sets choosable data specifying what filter can be generated in form of list
     * of tri-tuples : displayed name, class, passed object.
     * <pre>
     * The name is what will be displayed in the combobox to choose from
     * The class specifies the type of object the filter is generated for.
     * The passed object's purpose is to be returned along with the filter, mostly to be used in the generated filter
     * </pre>
     * <p>
     * If there is no object to pass, use null.
     * @param classes 
     */
    public void setData(List<Tuple3<String,Class,T>> classes) {
        classes.removeIf(e->predicateSupplier.call(Util.unPrimitivize(e._2)).isEmpty()); // remove unsupported
        classCB.getItems().setAll(classes);
        classCB.setValue(classes.isEmpty() ? null : classes.get(0));
    }
    
    public void setPredicateSupplier(Callback<Class,List<Tuple2<String,BiPredicate>>> supplier) {
        predicateSupplier = supplier;
    }
    
    /**
     * Returns newly generated filter for the selected data type and the passed
     * object.
     * 
     * @param filter_acceptor 
     */
    public void setOnFilterChange(BiConsumer<Predicate<Object>,T> filter_acceptor) {
        onFilterChange = filter_acceptor;
    }
    
    public void focus() {
        valueF.requestFocus();
        valueF.selectEnd();
    }
    
    public boolean isEmpty() {
        return valueF.getText().isEmpty() || predicate==null;
    }
    
    public void clear() {
        valueF.setText("");
    }
    
    /**
     * Entry point. After class is provided, list of available predicates will
     * be assigned to the combo box.
     * 
     * @param c 
     */
    private void setClass(Class c) {
        type = c;
        if(type==null) {
            filterCB.getItems().clear();
            filterCB.setValue(null);
        }
        type = Util.unPrimitivize(type);
        filterCB.getItems().setAll(predicateSupplier.call(type));
        filterCB.setValue(filterCB.getItems().isEmpty() ? null : filterCB.getItems().get(0));
    }
    
    private void generatePredicate(BiPredicate p, String txt_val, T o) {
        if(onFilterChange!=null && p!=null) {
            try {System.out.println("parsing " + type);
                Object value = Parser.fromS(type, txt_val);
                if(value != null) {
                    predicate = x -> p.test(x,value);
                    val = o;
                    onFilterChange.accept(predicate,o);
                }
            } catch(Exception e) {
                predicate = null;
                val = null;
            }
        }
    }
    
}