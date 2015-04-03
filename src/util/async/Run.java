/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.async;

import java.util.function.Consumer;
import java.util.function.Supplier;
import static javafx.application.Platform.isFxApplicationThread;
import static javafx.application.Platform.runLater;
import javafx.event.EventHandler;
import javafx.scene.control.ProgressIndicator;
import static javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS;
import static util.async.Async.FX;
import util.functional.functor.FunctionC;

/** Runnable with additional methods for composition and transformations.
 <p>
 @author Plutonium_
 */
public interface Run extends Runnable {
    
    public default Supplier<Void> toSupplier() {
        return () -> { run(); return null; };
    }
    
    public default Consumer<Void> toConsumer() {
        return nothing -> run();
    }
    
    public default FunctionC<Void,Void> toFunction() {
        return nothing -> { run(); return null; };
    }
    
    /** Returns a runnable that when run, runs this runnable on fx application
    thread. It wraps this runnable in runlater or executes directly if it is
    called from fx app thread.*/
    public default Run toFX() {
        return () -> {
            if(isFxApplicationThread()) run(); else runLater(this);
        };
    }
    
    /** Wraps this runnable in runLater. Equivalent to: {@code return () -> runLater(this);}*/
    public default Run toLater() {
        return () -> runLater(this);
    }
    
    /** Composes this runnable, so the after runnable runs right after this one.*/
    public default Run compose(Runnable after) {
        return () -> {
            run();
            after.run();
        };
    }
    /** Composes this runnable, so the after runnable runs right before this one.*/
    public default Run composeBefore(Runnable before) {
        return () -> {
            before.run();
            run();
        };
    }
    
    /** @return event handler that runs this runnable. */
    public default EventHandler toHandler() {
        return e -> run();
    }
    
    /** @return event handler that runs this runnable and consumes the event. */
    public default EventHandler toHandlerConsumed() {
        return e -> { run(); e.consume(); };
    }
    
    /** Passes this runnable into the provided execuor. */
    public default void runOn(Consumer<Runnable> executor) {
        executor.accept(this);
    }
    
    public default Run on(Consumer<Runnable> executor) {
        return () -> executor.accept(this);
    }
    
    public default Run then(Runnable after) {
        return () -> {
            run();
            after.run();
        };
    }
    
    public default Run thenOn(Consumer<Runnable> executor, Run after) {
        return then(after.on(executor));
    }
    
    
    public default Run thenStartProgress(ProgressIndicator i) {
        return thenOn(FX, () -> i.setProgress(INDETERMINATE_PROGRESS));
    }
    
    public default Run thenStopProgress(ProgressIndicator i) {
        return thenOn(FX, () -> i.setProgress(1));
    }
    
    
    
    public static Run runOn(Consumer<Runnable> executor, Run after) {
        return after.on(executor);
    }
    public static Run r() {
        return ()->{};
    }
    
    
    
    public default <R> Then<R> then(Supplier<R> after) {
        return new Callr(() -> {
            run();
            return after.get();
        });
    }
    
    public default <R> Then<R> thenFromThis(Then<R> then) {
        return after -> () -> {
            Run.this.run();
            then.then(after).run();
        };
        
//        Run rr = this;
//        return after -> () -> {
//            rr.then(then.then(after)).run();
////            then.then(after).run();
//        };
        
//        return then;
    }
    
    
    public static class Callr<R> implements Then<R> {
        Supplier<R> first;

        public Callr(Supplier<R> first) {
            this.first = first;
        }
        
        @Override
        public Run then(Consumer<R> c) {
            return () -> c.accept(first.get());
        }
        
        public Run thenOn(Consumer<Runnable> e, Consumer<R> c) {
            return then(c).on(e);
        }
        
    }
    
    public static abstract class CRun implements Run {
        Run first = () -> {};
        Run then = () -> {};

        @Override
        public Run then(Runnable after) {
            then = then.then(after);
            return this;
        }
        
    }
    
    public static class Bcr<R> implements Then<R> {
        Supplier<R> first;
        Thread t;
        
        public Bcr(Supplier<R> first) {
            this.first = first;
        }
        
        @Override
        public Run then(Consumer<R> after) {
            CRun cr = new CRun() {
                @Override public void run() {System.out.println("t start");
                    t.start();
                }
            };
            t = new Thread(() -> {
                R r = first.get();
                after.accept(r);
                cr.then.run();
            });
            t.setDaemon(true);
            return cr;
        }
    }
    
    public interface Then<R> {
        public Run then(Consumer<R> after);
        
        public static <R> Then<R> of(R r) {
            return after -> () -> after.accept(r);
        }
    }
    
}
