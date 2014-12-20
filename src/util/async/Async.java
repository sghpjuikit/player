/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.async;

import AudioPlayer.tagging.SuccessTask;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import static javafx.animation.Animation.INDEFINITE;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.util.Duration;

/**
 *
 * @author Plutonium_
 */
public final class Async {
    
    /**
     * Executes the action immediately on new thread as new 
     * {@link SuccessTask} and returns it. Use
     * @param <Void>
     * @param action
     * @return task
     */
    public static<Void> Task<Void> runAsTask(Runnable action) {
        return run(new SuccessTask<Void>() {
            @Override
            protected Void call() throws Exception {
                action.run();
                return null;
            }
        });
    }
    
    /**
     * Executes the action immediately on new thread as new
     * {@link SuccessTask} returning the result of the action and returns the task.
     * @param <R>
     * @param name Name of the task
     * @param action Action to execute as task
     * @param onEnd Action processing result of the task
     * @return The task
     */
    public static<R> Task<R> runAsTask(String name, Supplier<R> action, BiConsumer<Boolean,R> onEnd) {
        return run(new SuccessTask(name, onEnd) {
            @Override protected R call() throws Exception {
                updateMessage(name + " ...");
                return action.get();
            }
        });
    }
    
    /**
     * Executes the action immediately on a new Thread.
     * Equivalent to 
     * <pre>{@code 
     *   Thread thread = new Thread(action);
     *   thread.setDaemon(true);
     *   thread.start();}
     * </pre>
     * @param action
     * @param <R> type of action - any Runnable, that includes Tasks and Futures
     * @return the action. If the action is a {@link Task} it is useful to catch it
     * and bind to monitor progress.
     */
    public static<R extends Runnable> R run(R action) {
        Thread thread = new Thread(action);
        thread.setDaemon(true);
        thread.start();
        return action;
    }
    
    /**
     * Executes the action immediately on a main application thread.
     * Equivalent to 
     * <pre>{@code 
     *   Platform.runLater(action);}
     * </pre>
     * @param <R> type of action - any Runnable, that includes Tasks and Futures
     * @return the action. If the action is a {@link Task} it is useful to catch it
     * and bind to monitor progress.
     */
    public static<R extends Runnable> R runOnFX(R action) {
        Platform.runLater(action);
        return action;
    }
    
    /**
     * Executes the action on current thread after specified delay from now.
     * Equivalent to {@code new FxTimer(delay, action, 1).restart();}.
     * @param delay delay
     * @param <R> type of action - any Runnable, that includes Tasks and Futures
     * @return the action. If the action is a {@link Task} it is useful to catch it
     * and bind to monitor progress.
     */
    public static<R extends Runnable> R run(Duration delay, R action) {
        new FxTimer(delay, 1, action).restart();
        return action;
    }
    
    /**
     * Executes the action on current thread after specified delay from now.
     * Equivalent to {@code new FxTimer(delay, action, 1).restart();}.
     * @param delay Delay in milliseconds
     * @param <R> type of action - any Runnable, that includes Tasks and Futures
     * @return the action. If the action is a {@link Task} it is useful to catch it
     * and bind to monitor progress.
     */
    public static<R extends Runnable> R run(double delay, R action) {
        new FxTimer(delay, 1, action).restart();
        return action;
    }
    
    /**
     * Executes the action on current thread repeatedly with given time period.
     * Equivalent to {@code new FxTimer(delay, action, INDEFINITE).restart();}.
     * @param delay delay
     */
    public static FxTimer runPeriodic(Duration period, Runnable action) {
        FxTimer t = new FxTimer(period, INDEFINITE, action);
        t.restart();
        return t;
    }
    
    

    /** Transforms Runnable into Runnable that executes on FxApplication Thread.*/
    public static final UnaryOperator<Runnable> toFxRunnable = r -> () -> runOnFX(r);
    
    /** Function transforming executor into function transforming runnable into 
     * runnable that executes with provided executor */
    public static final Function<Consumer<Runnable>,UnaryOperator<Runnable>> executionWrapper = executor -> r -> () -> executor.accept(r);
    
}
