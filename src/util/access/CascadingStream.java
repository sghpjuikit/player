/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access;

import static java.util.Collections.EMPTY_LIST;
import java.util.HashMap;
import static java.util.Objects.requireNonNull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.reactfx.EventStream;
import org.reactfx.Subscription;

/**
 *
 * @author Plutonium_
 */
public class CascadingStream<E> {
    
    public static final Supplier<EventStream<?>> DEFAULT_EVENT_STREAM_FACTORY = () -> new AccessibleStream(EMPTY_LIST);
//    public static final Supplier<EventStream<?>> DEFAULT_EVENT_STREAM_FACTORY = () -> new EventSource();
    
    HashMap<Integer,AccessibleStream<E>> map = new HashMap();
    public Supplier<AccessibleStream<E>> eventStreamFactory = (Supplier)DEFAULT_EVENT_STREAM_FACTORY;
    
    public CascadingStream() {
        link(1);
    }
    
    public void push(int i, E event) {
        link(i);
        AccessibleStream<E> s = map.get(i);
        s.push(event);
    }
    
    public Subscription subscribe(int i, BiConsumer<Integer,? super E> subscriber) {
        link(i);
        AccessibleStream<E> s = map.get(i);
        Consumer<? super E> mediatorSubscriber = e -> subscriber.accept(i, e);
        return s.subscribe(mediatorSubscriber);
    }
    
    private void link(int i) {
        if(i<1) throw new IllegalArgumentException();
        AccessibleStream<E> s = map.get(i);
        if(s==null) {
            map.put(i, eventStreamFactory.get());
            if(i>1) link(i-1);
        }
    }
    private void unlink(int i) {
        AccessibleStream<E> s = map.get(i);
//        if(s!=null) {
//            s.
//            if(i>1) link(i-1);
//        }
    }
    
    public E getValue(int i) {
        link(i);
        AccessibleStream<E> s = map.get(i);
        requireNonNull(s);
        return s.getValue();
    }
    
    /** @return highest active level */
    public int getLastLvl() {
        return map.size()-1;
    }
}
