/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.async.executor;

import java.util.function.Consumer;

/**
 * Event frequency reducer. Consumes events and reduces events in close temporal
 * succession and fires the last event.
 *
 * @author Plutonium_
 */
public class EventThrottler<E> {
    
    private final FxTimer t;
    private E e;
    
    public EventThrottler(double period, Consumer<E> forward_action) {
        t = new FxTimer(period, 1, () -> forward_action.accept(e));
    }
    
    public void push(E event) {
        e = event;
        t.restart();
    }
}
