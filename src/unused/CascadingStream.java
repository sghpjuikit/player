/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unused;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.reactfx.EventStream;
import org.reactfx.Subscription;

import util.dev.TODO;
import util.reactive.ValueEventSource;

import static util.dev.TODO.Purpose.FUNCTIONALITY;
import static util.dev.TODO.Severity.CRITICAL;
import static util.dev.Util.noØ;
import static util.functional.Util.listRO;

/**
 *
 * @author Martin Polakovic
 */
public class CascadingStream<E> {
    
    public static final Supplier<EventStream<?>> DEFAULT_EVENT_STREAM_FACTORY = () -> new ValueEventSource<>(listRO());
    
    HashMap<Integer,ValueEventSource<E>> map = new HashMap<>();
    public Supplier<ValueEventSource<E>> eventStreamFactory = (Supplier)DEFAULT_EVENT_STREAM_FACTORY;
    
    public CascadingStream() {
        link(1);
    }
    
    public void push(int i, E event) {
        link(i);
        ValueEventSource<E> s = map.get(i);
        s.push(event);
    }
    
    public Subscription subscribe(int i, BiConsumer<Integer,? super E> subscriber) {
        link(i);
        ValueEventSource<E> s = map.get(i);
        Consumer<? super E> mediatorSubscriber = e -> subscriber.accept(i, e);
        return s.subscribe(mediatorSubscriber);
    }
    
    private void link(int i) {
        if (i<1) throw new IllegalArgumentException();
        ValueEventSource<E> s = map.get(i);
        if (s==null) {
            map.put(i, eventStreamFactory.get());
            if (i>1) link(i-1);
        }
    }
    @TODO(severity = CRITICAL, purpose = FUNCTIONALITY)
    private void unlink(int i) {
        ValueEventSource<E> s = map.get(i);
//        if (s!=null) {
//            s.
//            if (i>1) link(i-1);
//        }
    }
    
    public E getValue(int i) {
        link(i);
        ValueEventSource<E> s = map.get(i);
	    noØ(s);
        return s.getValue();
    }
    
    /** @return highest active level */
    public int getLastLvl() {
        return map.size()-1;
    }
}
