/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access;

import java.util.Objects;

import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;

/**
 *
 * @author Martin Polakovic
 */
public class Vo<T> implements ObservableValue<T>, WritableValue<T> {
    public final BooleanProperty override = new SimpleBooleanProperty(true);
    public final ObjectProperty<T> real = new SimpleObjectProperty<T>();
    public final Property<T> parent;
    public final ObjectProperty<T> current;
    
    public Vo(Property<T> parent ) {
        this(parent.getValue(), false, parent);
    }
    
    public Vo(boolean override, Property<T> parent ) {
        this(parent.getValue(), override, parent);
    }
    
    public Vo(T val, boolean override, Property<T> parent ) {
        this.override.set(override);
        this.real.set(val);
        this.parent = parent;
        current = new SimpleObjectProperty(override ? val : parent.getValue());
        
        ChangeListener<Object> l = (o,ov,nv) -> change();
        this.override.addListener(l);
        this.real.addListener(l);
        this.parent.addListener(l);
    }

    
    @Override
    public void addListener(ChangeListener<? super T> listener) {
        current.addListener(listener);
    }

    @Override
    public void removeListener(ChangeListener<? super T> listener) {
        current.removeListener(listener);
    }

    @Override
    public T getValue() {
        return current.getValue();
    }

    @Override
    public void setValue(T val) {
        real.setValue(val);
    }

    @Override
    public void addListener(InvalidationListener listener) {
        current.addListener(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        current.removeListener(listener);
    }
    
    private void change() {
        T t = override.get() ? real.getValue(): parent.getValue();
        if (!Objects.equals(t, current.get()))
            current.setValue(t);
    }
}