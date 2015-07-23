/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.reactive;

import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import static org.reactfx.EventStreams.valuesOf;
import org.reactfx.Subscription;

/***/
public class Util {
    
    /**  */
    public static<O,V> Subscription maintain(ObservableValue<O> o, Function<O,V> m, Consumer<? super V> u) {
        u.accept(m.apply(o.getValue()));
        return valuesOf(o).map(m).subscribe(u);
    }
    
    /***/
    public static<O> Subscription maintain(ObservableValue<O> o, Consumer<? super O> u) {
        ChangeListener<O> l = (b,ov,nv) -> u.accept(nv);
        u.accept(o.getValue());
        o.addListener(l);
        return () -> o.removeListener(l);
    }
    
    public static<O,V> Subscription maintain(ObservableValue<O> o, Function<? super O, ? extends V> m, WritableValue<V> w) {
        w.setValue(m.apply(o.getValue()));
        return valuesOf(o).map(m).subscribe(w::setValue);
    }
    
    public static<O> Subscription maintain(ObservableValue<O> o, WritableValue<O> w) {
        w.setValue(o.getValue());
        ChangeListener<O> l = (x,ov,nv) -> w.setValue(nv);
        o.addListener(l);
        return () -> o.removeListener(l);
    }
    
    
    public static<T> Subscription sizeOf(ObservableList<T> list, Consumer<? super Integer> action) {
        ListChangeListener<T> l = change -> action.accept(list.size());
        l.onChanged(null);
        list.addListener(l);
        return () -> list.removeListener(l);
    }
    
    
    public static<O> Subscription maintain(ValueStream<O> o, Consumer<? super O> u) {
        u.accept(o.getValue());
        return o.subscribe(u);
    }
    public static<O> Subscription maintain(ValueStream<O> o, O initial, Consumer<? super O> u) {
        u.accept(initial);
        return o.subscribe(u);
    }
}
