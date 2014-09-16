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
import org.reactfx.util.Tuple3;
import utilities.Parser.ParserImpl.Parser;
import utilities.Util;
import utilities.filtering.NumberPredicates;
import utilities.filtering.Predicates;
import utilities.filtering.StringPredicates;

/**
 *
 * @author Plutonium_
 */
public class FilterGenerator<T> extends HBox {
    
    ComboBox<Tuple3<String,Class,T>> classCB = new ComboBox();
    ComboBox<Predicates> filterCB = new ComboBox();
    TextField valueF = new TextField();
    
    BiConsumer<Predicate<Object>,T> onFilterChange;
    private Class type;
    
    public Predicate<Object> predicate;
    public T val;
    
    public FilterGenerator() {
        // initialize gui
        getChildren().addAll(classCB, filterCB, valueF);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(1));
        
        // generate predicates on change
        filterCB.valueProperty().addListener((o,ov,nv) -> generatePredicate(nv, valueF.getText(),classCB.getValue()==null ? null : classCB.getValue()._3));
        valueF.textProperty().addListener((o,ob,nv) -> generatePredicate(filterCB.getValue(), nv,classCB.getValue()==null ? null : classCB.getValue()._3));

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
            return new ListCell<Predicates>(){
                @Override
                protected void updateItem(Predicates item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item.toStringEnum());
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
        classCB.getItems().setAll(classes);
        classCB.setValue(classes.isEmpty() ? null : classes.get(0));
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
        
        if (String.class.equals(type)) {
            filterCB.getItems().setAll(StringPredicates.values());
            filterCB.setValue(StringPredicates.CONTAINS_NOCASE);
        } else if(Number.class.isAssignableFrom(type)) {
            filterCB.getItems().setAll(NumberPredicates.values());
            filterCB.setValue(NumberPredicates.SAME);
        }
    }
    
    private void generatePredicate(Predicates nv, String txt_val, T o) {
        if(onFilterChange!=null && nv!= null && o!= null) {
            BiPredicate filterP = nv.predicate(type);
            try {
                Object value = Parser.fromS(type, txt_val);
                if(value != null) {
                    predicate = x -> filterP.test(x,value);
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