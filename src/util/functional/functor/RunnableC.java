/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.functional.functor;

import java.util.function.Consumer;
import static javafx.application.Platform.isFxApplicationThread;
import static javafx.application.Platform.runLater;
import javafx.event.EventHandler;

/** Runnable with additional methods for composition and transformations.
 <p>
 @author Plutonium_
 */
public interface RunnableC extends Runnable {
    
    /** Returns a runnable that when run, runs this runnable on fx application
    thread. It wraps this runnable in runlater or executes directly if it is
    called from fx app thread.*/
    public default Runnable toFX() {
        return () -> {
            if(isFxApplicationThread()) run(); else runLater(this);
        };
    }
    
    /** Wraps this runnable in runLater. Equivalent to: {@code return () -> runLater(this);}*/
    public default Runnable toLater() {
        return () -> runLater(this);
    }
    
    /** Composes this runnable, so the after runnable runs right after this one.*/
    public default Runnable compose(Runnable after) {
        return () -> {
            run();
            after.run();
        };
    }
    /** Composes this runnable, so the after runnable runs right before this one.*/
    public default Runnable composeBefore(Runnable before) {
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
    
    
}
