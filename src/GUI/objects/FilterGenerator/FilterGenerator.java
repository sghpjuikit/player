/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.FilterGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
    
    ComboBox<Tuple3<String,Class,T>> typeCB = new ComboBox();
    ComboBox<Tuple2<String,BiPredicate>> filterCB = new ComboBox();
    TextField valueF = new TextField();
    
    private Supplier<Tuple3<String,Class,T>> prefTypeSupplier;
    private BiConsumer<Predicate<Object>,T> onFilterChange;
    private Callback<Class,List<Tuple2<String,BiPredicate>>> predicateSupplier;
    private Callback<Class,Tuple2<String,BiPredicate>> prefpredicateSupplier;
    
    private Class type;
    
    public Predicate<Object> predicate;
    public T val;
    
    public FilterGenerator() {
        // initialize gui
        getChildren().addAll(typeCB, filterCB, valueF);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(1));
        
        typeCB.setVisibleRowCount(25);
        filterCB.setVisibleRowCount(25);
        valueF.setPromptText("Filter criteria");
        
        // generate predicates on change
        valueF.textProperty().addListener((o,ob,nv) -> generatePredicate(filterCB.getValue()==null ? null : filterCB.getValue()._2, nv, typeCB.getValue()==null ? null : typeCB.getValue()._3));
        filterCB.valueProperty().addListener((o,ov,nv) -> generatePredicate(nv==null ? null : nv._2, valueF.getText(), typeCB.getValue()==null ? null : typeCB.getValue()._3));

        // use dynamic toString() when populating combobox values
        typeCB.setCellFactory( view -> {
            return new ListCell<Tuple3<String,Class,T>>(){
                @Override
                protected void updateItem(Tuple3<String,Class,T> item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item._1);
                }
            };
        });
        typeCB.setButtonCell(typeCB.getCellFactory().call(null));
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
        typeCB.valueProperty().addListener((o,ov,nv) -> setClass(nv._2));
    }
    
    public void setPrefTypeSupplier(Supplier<Tuple3<String,Class,T>> supplier) {
        prefTypeSupplier = supplier;
    }
    
    public void setPredicateSupplier(Callback<Class,List<Tuple2<String,BiPredicate>>> supplier) {
        predicateSupplier = supplier;
    }
    
    public void setPrefPredicateSupplier(Callback<Class,Tuple2<String,BiPredicate>> supplier) {
        prefpredicateSupplier = supplier;
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
    
    /**
     * Sets chosable data specifying what filter can be generated in form of list
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
        List<Tuple3<String,Class,T>> cs = new ArrayList(classes);
        cs.removeIf(e->predicateSupplier.call(Util.unPrimitivize(e._2)).isEmpty()); // remove unsupported
        typeCB.getItems().setAll(cs);
        
        Tuple3<String,Class,T> v = prefTypeSupplier == null ? null : prefTypeSupplier.get();
        if (v==null) v = cs.isEmpty() ? null : cs.get(0);
        typeCB.setValue(v);
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
        
        Tuple2<String,BiPredicate> v = prefpredicateSupplier == null ? null : prefpredicateSupplier.call(type);
        if (v==null) v = filterCB.getItems().isEmpty() ? null : filterCB.getItems().get(0);
        filterCB.setValue(v);
    }
    
    private void generatePredicate(BiPredicate p, String txt_val, T o) {
        if(onFilterChange!=null && p!=null) {
            try {
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