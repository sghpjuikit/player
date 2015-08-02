/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.controller.io;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.reactfx.Subscription;

/**
 *
 * @author Plutonium_
 */
public class Put<T> {
    
    final Class<? super T> type;
    final ObjectProperty<T> val = new SimpleObjectProperty();
    protected List<Consumer<? super T>> monitors = new ArrayList();
    
    public Put(Class<? super T> type, T init_val) {
        this.type = type;
        this.val.setValue(init_val);
    }

    public Class<? super T> getType() {
        return type;
    }

    public T getValue() {
        return val.get();
    }
    
    public void setValue(T v) {
        val.setValue(v);
        monitors.forEach(m -> m.accept(v));
    }
    
    public Subscription monitor(Consumer<? super T> action) {
        monitors.add(action);
        // i think this is dangerous because we dont know what the action does...
        // it should run only after this method completes, so.. wrap it up in runLater ?
        if(getValue()!=null) action.accept(getValue());
        return () -> monitors.remove(action);
    }
    
    
    
}
