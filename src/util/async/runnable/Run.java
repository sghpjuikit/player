/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.async.runnable;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.event.EventHandler;
import javafx.scene.control.ProgressIndicator;

import util.functional.functor.FunctionC;

import static javafx.application.Platform.isFxApplicationThread;
import static javafx.application.Platform.runLater;

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
    
    /** 
     * Returns a runnable that when run, runs this runnable on fx application
     * thread. It wraps this runnable in runlater or executes directly if it is
     * called from fx app thread.
     * <p>
     * Equivalent to:
     * <pre>{@code
     *   return () -> {
     *       if(isFxApplicationThread()) 
     *          run();
     *       else
     *          runLater(this);
     *   };
     * }</pre>
     */
    public default Run toOnFx() {
        return () -> {
            if(isFxApplicationThread()) run(); else runLater(this);
        };
    }
    
    /** Wraps this runnable in runLater. Equivalent to: {@code return () -> runLater(this);}*/
    public default Run toOnLater() {
        return () -> runLater(this);
    }
    
    /** Wraps this runnable in executor. Equivalent to: {@code return () -> executor.accept(this);}
     * <pre>
     * The following pseudocodes are equivalent:
     * toOnExecutor(e).run();
     * e.execute(this);
     * </pre>
     */
    public default Run toOnExecutor(Consumer<Runnable> e) {
        return () -> e.accept(this);
    }
    
    /** 
     * Limits number of executions of this runnable. Any subsequent executions
     * will be ignored.
     * <p>
     * Equivalent to: {@code return new LimitedRunnable(times,this); }
     * 
     * @see LimitedRunnable
     */
    public default Run limited(long times) {
        return new LimitedRunnable(times,this);
    }
    
    /** Returns runnable that runs this runnable and then after runnable.*/
    public default Run composeAfter(Runnable after) {
        return () -> {
            run();
            after.run();
        };
    }
    
    /** Returns runnable that runs the before runnable and then this runnable.*/
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
    
    /** Executes this runnable on the provided executor. */
    public default void runOn(Consumer<Runnable> e) {
        e.accept(this);
    }
    

    
    
    
    
    
    public default Run then(Runnable after) {
        return composeAfter(after);
    }
    
    public default Run thenOn(Consumer<Runnable> e, Run after) {
        return then(after.toOnExecutor(e));
    }
    
    public default Run showProgress(ProgressIndicator i) {
        return composeBefore(of(() -> i.setProgress(-1)).toOnFx())
              .composeAfter(of(() -> i.setProgress(1)).toOnFx());
    }
    
    /** Transforms Runnable to Run. */
    public static Run of(Runnable r) {
        return r::run;
    }
    
    
    class LimitedRunnable implements Run {

        private long executed = 0;
        private final long max;
        private final Runnable x;

        /** 
         * @param limit maximum number of times the runnable can execute
         * @param action action that will execute when this runnable executes
         */
        LimitedRunnable(long limit, Runnable action) {
            max = limit;
            x = action;
        }

        @Override
        public void run() {
            if(executed<max) x.run();
            executed++;
        }

    }
}
