/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import utilities.Parser.ParserImpl.Parser;
import utilities.Util;
import utilities.filtering.NumberPredicates;
import utilities.filtering.Predicates;
import utilities.filtering.StringPredicates;

/**
 *
 * @author Plutonium_
 */
public class Filter extends HBox {
    
    ComboBox<Predicates> filterType = new ComboBox(FXCollections.observableArrayList());
    TextField valueField = new TextField();
    
    Consumer<Predicate<? super Object>> onFilterChange;
    private Class type;
    
    public Filter() {
        // initialize values
        setClass(String.class);
        // initialize gui
        getChildren().addAll(filterType, valueField);
        // generate predicates on change
        filterType.valueProperty().addListener((o,ov,nv) -> generatePredicate(nv, valueField.getText()));
        valueField.textProperty().addListener((o,ob,nv) -> generatePredicate(filterType.getValue(), nv));
    }
    
    public void setOnFilterChange(Consumer<Predicate<Object>> filter_acceptor) {
        onFilterChange = filter_acceptor;
    }
    
    /**
     * Entry point. After class is provided, list of available predicates will
     * be assigned to the combo box.
     * 
     * @param c 
     */
    public final void setClass(Class c) {
        c = Util.unPrimitivize(c);
        
        type = c;
        
        if (String.class.equals(type)) {
            filterType.getItems().setAll(StringPredicates.values());
            filterType.setValue(StringPredicates.CONTAINS_NOCASE);
        } else if(Number.class.isAssignableFrom(type)) {
            filterType.getItems().setAll(NumberPredicates.values());
            filterType.setValue(NumberPredicates.SAME);
        }
    }
    
    private void generatePredicate(Predicates nv, String txt_val) {
        if(onFilterChange!=null) {
            BiPredicate filterP = nv.predicate(type);
            try {
                Object value = Parser.fromS(type, txt_val);
                if(value != null) onFilterChange.accept(x -> filterP.test(x,value));
            } catch(Exception e) {
                // ignore
            }
        }
    }
    
}