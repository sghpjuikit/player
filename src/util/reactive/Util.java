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

/**
 * Utility methods for reactive behavior.
 */
public interface Util {

    /**  */
    static <O,V> Subscription maintain(ObservableValue<O> o, Function<O,V> m, Consumer<? super V> u) {
        u.accept(m.apply(o.getValue()));
        return valuesOf(o).map(m).subscribe(u);
    }

    /***/
    static <O> Subscription maintain(ObservableValue<O> o, Consumer<? super O> u) {
        ChangeListener<O> l = (b,ov,nv) -> u.accept(nv);
        u.accept(o.getValue());
        o.addListener(l);
        return () -> o.removeListener(l);
    }

    static <O,V> Subscription maintain(ObservableValue<O> o, Function<? super O, ? extends V> m, WritableValue<? super V> w) {
        w.setValue(m.apply(o.getValue()));
        ChangeListener<O> l = (x,ov,nv) -> w.setValue(m.apply(nv));
        o.addListener(l);
        return () -> o.removeListener(l);
    }

    static <O> Subscription maintain(ObservableValue<? extends O> o, WritableValue<O> w) {
        w.setValue(o.getValue());
        ChangeListener<O> l = (x,ov,nv) -> w.setValue(nv);
        o.addListener(l);
        return () -> o.removeListener(l);
    }

    static <T> Subscription sizeOf(ObservableList<T> list, Consumer<? super Integer> action) {
        ListChangeListener<T> l = change -> action.accept(list.size());
        l.onChanged(null);
        list.addListener(l);
        return () -> list.removeListener(l);
    }


    static <O> Subscription maintain(ValueStream<O> o, Consumer<? super O> u) {
        u.accept(o.getValue());
        return o.subscribe(u);
    }

    static <O> Subscription maintain(ValueStream<O> o, O initial, Consumer<? super O> u) {
        u.accept(initial);
        return o.subscribe(u);
    }

    /**
     * Runs action (consuming the property's value) immediately if value non null or sets a one-time
     * listener which will run the action when the value changes to non null for the 1st time and
     * remove itself.
     * <p/>
     * It is guaranteed:
     * <ul>
     * <li> action executes at most once
     * <li> action never consumes null
     * <li> action executes as soon as the property value is not null - now or in the future
     * </ul>
     * <p/>
     * Used to execute some kind of initialization routine, which requires nonnull value (which is
     * not guaranteed to be the case).
     */
    static <T> void executeWhenNonNull(ObservableValue<T> property, Consumer<T> action) {
        if(property.getValue()!=null)
            action.accept(property.getValue());
        else {
            property.addListener(singletonListener(property, action));
        }
    }

    static <T> ChangeListener<T> singletonListener(ObservableValue<T> property, Consumer<T> action) {
        return new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends T> observable, T ov, T nv) {
                if(nv!=null) {
                    action.accept(nv);
                    property.removeListener(this);
                }
            }
        };
    }

    static <T> void installSingletonListener(ObservableValue<T> property, Consumer<T> action) {
        property.addListener(singletonListener(property, action));
    }

    /** Creates list change listener which calls the respective listeners (only) on add or remove events respectively. */
    static <T> ListChangeListener<T> listChangeListener(ListChangeListener<T> onAdded, ListChangeListener<T> onRemoved) {
        noØ(onAdded, onRemoved);
        return change -> {
            while(change.next()) {
                if (!change.wasPermutated() && !change.wasUpdated()) {
                    if(change.wasAdded()) onAdded.onChanged(change);
                    if(change.wasAdded()) onRemoved.onChanged(change);
                }
            }
        };
    }

    /** Creates list change listener which calls an action for every added or removed item. */
    static <T> ListChangeListener<T> listChangeHandler(Consumer<T> addedHandler, Consumer<T> removedHandler) {
        noØ(addedHandler, removedHandler);
        return change -> {
            while(change.next()) {
                if (!change.wasPermutated() && !change.wasUpdated()) {
                    if(change.wasAdded()) change.getRemoved().forEach(removedHandler);
                    if(change.wasAdded()) change.getAddedSubList().forEach(addedHandler);
                }
            }
        };
    }
}