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

import org.reactfx.Subscription;

import static org.reactfx.EventStreams.valuesOf;
import static util.dev.Util.noØ;

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

    public static<O,V> Subscription maintain(ObservableValue<O> o, Function<? super O, ? extends V> m, WritableValue<? super V> w) {
        w.setValue(m.apply(o.getValue()));
        return valuesOf(o).map(m).subscribe(w::setValue);
    }

    public static<O> Subscription maintain(ObservableValue<? extends O> o, WritableValue<O> w) {
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

    /**
     * Runs action immediately consuming the property's value if non null or sets a
     * listener which will run the action when the value changed to non null and remove itself.
     * <p>
     * Used to execute some kind of initialization routine, which requires nonnull value (which is
     * not guaranteed to be the case).
     */
    public static <T> void executeWhenNonNull(ObservableValue<T> property, Consumer<T> action) {
        if(property.getValue()!=null)
            action.accept(property.getValue());
        else {
            property.addListener(new ChangeListener<T>() {
                @Override
                public void changed(ObservableValue<? extends T> observable, T ov, T nv) {
                    if(nv!=null) {
                        action.accept(nv);
                        property.removeListener(this);
                    }
                }
            });
        }
    }

    /** Creates list change listener which calls the respective listeners (only) on add or remove events respectively. */
    public static <T> ListChangeListener<T> listChangeListener(ListChangeListener<T> onAdded, ListChangeListener<T> onRemoved) {
        noØ(onAdded);
        noØ(onRemoved);
        return change -> {
            while(change.next()) {
                if (change.wasPermutated()) {
                    for (int i = change.getFrom(); i < change.getTo(); ++i) {
                        //permutate
                    }
                } else if (change.wasUpdated()) {
                    //update item
                } else {
                    if(change.wasAdded()) onAdded.onChanged(change);
                    if(change.wasAdded()) onRemoved.onChanged(change);
                }
            }
        };
    }

    /** Creates list change listener which calls an action for every added or removed item. */
    public static <T> ListChangeListener<T> listChangeHandler(Consumer<T> addedHandler, Consumer<T> removedHandler) {
        noØ(addedHandler);
        noØ(removedHandler);
        return change -> {
            while(change.next()) {
                if (change.wasPermutated()) {
                    for (int i = change.getFrom(); i < change.getTo(); ++i) {
                        //permutate
                    }
                } else if (change.wasUpdated()) {
                    //update item
                } else {
                    for (T o : change.getRemoved())
                        removedHandler.accept(o);

                    for (T o : change.getAddedSubList())
                        addedHandler.accept(o);
                }
            }
        };
    }
}