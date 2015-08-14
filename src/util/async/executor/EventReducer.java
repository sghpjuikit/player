/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.async.executor;

import java.util.function.Consumer;

import util.functional.Functors.F2;

/**
 * Event frequency reducer. Consumes events and reduces close temporal successions into (exactly) 
 * single event.
 * <p>
 * The reducing can work in two ways:
 * <ul>
 * <li>Firing the first event will
 * be instantaneous (as soon as it arrives) and will in effect ignore all future events of the
 * succession.
 * <p>
 * <li>Firing the final event will cause all previous events to be accumulated into one event using 
 * a reduction function (by default it simply ignores the events until the last one). It is then 
 * fired, when the succession ends. Note the dalay between last consumed event of the succession and
 * the succession ending. It only ends when the timer runs out and future events will start a new
 * succession. Even if the succession has only 1 event, there will still be delay between consuming
 * it and firing it as a reduced event.
 * </ul>
 * For example, one may wish to run an action when first even arrives and then avoid running it
 * again when new events are captured quickly one after another. For this chose {@link #FIRST}.
 * If one needs the most up to date information (perhaps the events update UI), then 
 * {@link #LAST} is recommended.
 * @author Plutonium_
 */
public abstract class EventReducer<E> {
    protected Consumer<E> action;
    protected double time;
    protected final F2<E,E,E> r;
    protected E e;
    
    private EventReducer(double inter_period, F2<E,E,E> reduction, Consumer<E> handler) {
        time = inter_period;
        action = handler;
        r = reduction;
    }
    
    public void push(E event) {
        e = r==null || e==null ? event : r.apply(e, event);
        handle();
    }
    
    protected abstract void handle();


    
    public static <E> EventReducer<E> toFirst(double inter_period, Consumer<E> handler) {
        return new HandlerFirst<>(inter_period, handler);
    }
    
    public static <E> EventReducer<E> toFirst(double inter_period, Runnable handler) {
        return new HandlerFirst<>(inter_period, e -> handler.run());
    }
    
    public static <E> EventReducer<E> toFirstDelayed(double inter_period, Consumer<E> handler) {
        return new HandlerFirstDel<>(inter_period, handler);
    }
    
    public static <E> EventReducer<E> toFirstDelayed(double inter_period, Runnable handler) {
        return new HandlerFirstDel<>(inter_period, e -> handler.run());
    }
    
    public static <E> EventReducer<E> toLast(double inter_period, Consumer<E> handler) {
        return new HandlerLast<>(inter_period, null, handler);
    }
    
    public static <E> EventReducer<E> toLast(double inter_period, Runnable handler) {
        return new HandlerLast<>(inter_period, null, e -> handler.run());
    }
    
    public static <E> EventReducer<E> toLast(double inter_period, F2<E,E,E> reduction, Consumer<E> handler) {
        return new HandlerLast<>(inter_period, reduction, handler);
    }
    
    public static <E> EventReducer<E> toLast(double inter_period, F2<E,E,E> reduction, Runnable handler) {
        return new HandlerLast<>(inter_period, reduction, e -> handler.run());
    }
    
    
    
    private static class HandlerLast<E> extends EventReducer<E> {
        
        private final FxTimer t;

        public HandlerLast(double inter_period, F2<E, E, E> reduction, Consumer<E> handler) {
            super(inter_period, reduction, handler);
            t = new FxTimer(time, 1, () -> action.accept(e));
        }
        
        @Override
        public void handle() {
            t.start(time);
        }
        
    }
    private static class HandlerFirst<E> extends EventReducer<E> {
        
        private long last = 0;

        public HandlerFirst(double inter_period, Consumer<E> handler) {
            super(inter_period, null, handler);
        }
        
        @Override
        public void handle() {
            long now = System.currentTimeMillis();
            long diff = now-last;
            last = now;
            if(diff > time) action.accept(e);
        }
        
    }
    private static class HandlerFirstDel<E> extends EventReducer<E> {
        
        private long last = 0;
        private final FxTimer t;

        public HandlerFirstDel(double inter_period, Consumer<E> handler) {
            super(inter_period, null, handler);
            t = new FxTimer(time, 1, () -> action.accept(e));
        }
        
        @Override
        public void handle() {
            long now = System.currentTimeMillis();
            long diff = now-last;
            last = now;
            if(diff > time) if(!t.isRunning()) t.start();
        }
        
    }
}
