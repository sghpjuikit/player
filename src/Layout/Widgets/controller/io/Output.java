/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.controller.io;

import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.reactfx.Subscription;
import static util.reactive.Util.maintain;

/**
 *
 * @author Plutonium_
 */
public class Output<T> {
    final String name;
    final Class<T> type;
    final ObjectProperty<T> val = new SimpleObjectProperty();
    
        
    public Output(String name, Class c) {
        this.name = name;
        this.type = c;
    }
    
    
    public String getName() {
        return name;
    }
    public Class<T> getType() {
        return type;
    }
    
    public T getValue() {
        return val.getValue();
    }
    
    public void setValue(T v) {
        val.setValue(v);
    }
    
    public Subscription monitor(Consumer<T> action) {
        return maintain(val, action);
    }
    
    
    
    
    
    private Function<T,String> toS = null;

    public Output<T> setStringConverter(Function<T,String> c) {
        toS = c;
        return this;
    }
    
    public String getValueAsS() {
        T v = val.getValue();
        return v==null ? "null" : toS==null ? v.toString() : toS.apply(v);
    }

}
