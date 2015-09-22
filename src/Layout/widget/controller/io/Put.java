/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget.controller.io;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.WritableValue;

import org.reactfx.Subscription;

/**
 *
 * @author Plutonium_
 */
public class Put<T> implements WritableValue<T> {
    
    final Class<? super T> type;
    final ObjectProperty<T> val = new SimpleObjectProperty();
    protected Set<Consumer<? super T>> monitors = new HashSet<>();
    
    public Put(Class<? super T> type, T init_val) {
        this.type = type;
        this.val.setValue(init_val);
    }

    public Class<? super T> getType() {
        return type;
    }

    @Override
    public T getValue() {
        return val.get();
    }
    
    @Override
    public void setValue(T v) {
        val.setValue(v);
        monitors.forEach(m -> m.accept(v));
    }
    
    public Subscription monitor(Consumer<? super T> action) {
        monitors.add(action);
        action.accept(getValue());
        return () -> monitors.remove(action);
    }
    
}
