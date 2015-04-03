/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.async;

import AudioPlayer.tagging.SuccessTask;
import static java.util.Objects.requireNonNull;
import java.util.concurrent.Executor;
import java.util.function.*;
import static javafx.animation.Animation.INDEFINITE;
import javafx.application.Platform;
import static javafx.application.Platform.runLater;
import javafx.concurrent.Task;
import javafx.util.Duration;

/**
 *
 * @author Plutonium_
 */
public final class Async {
    
    public static Consumer<Runnable> FX = Async::executeFX;
    public static Consumer<Runnable> FXLATER = Async::executeLater;
    public static Consumer<Runnable> BGR = Async::executeBgr;
    public static Consumer<Runnable> CURR = Async::executeCurr;
    
    public static Executor eFX = Async::executeFX;
    public static Executor eFXLATER = Async::executeLater;
    public static Executor eBGR = Async::executeBgr;
    public static Executor eCURR = Async::executeCurr;
    
    
    public static void executeCurr(Runnable r) {
        r.run();
    }
    
    public static void executeBgr(Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.start();
    }
    
    public static void executeFX(Runnable r) {
        if(Platform.isFxApplicationThread()) r.run(); else runLater(r);
    }
    
    public static void executeLater(Runnable r) {
        Platform.runLater(r);
    }
    
    
    /**
     * Executes the action immediately on new thread as new 
     * {@link SuccessTask} and returns it. Use
     * @param <Void>
     * @param action
     * @return task
     */
    public static Task<Void> runAsTask(Runnable action) {
        return Async.runBgr(new SuccessTask<Void,SuccessTask>() {
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
        return runBgr(new SuccessTask(name, onEnd) {
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
    public static<R extends Runnable> R runBgr(R action) {
        Thread thread = new Thread(action);
        thread.setDaemon(true);
        thread.start();
        return action;
    }
    
    /** 
     * Prepares value V on new bgr thread started immediately and consumes it
     * on FX thread.
     * <p>
     * Use to run blocking call (e.g. I/O) on bgr and use result back on FX thread
     * 
     * @throws NullPointerException if any parameter null
     */
    public static <V> void runBgr(Supplier<V> bgraction, Consumer<V> fxaction) {
        requireNonNull(bgraction);
        requireNonNull(fxaction);
        
        Thread thread = new Thread(() -> {
            V t = bgraction.get();
            runLater(() -> fxaction.accept(t));
        });
        thread.setDaemon(true);
        thread.start();
    }
    /** Convenience method. {@link Void} version of {@link #runBgr(java.util.function.Supplier, java.util.function.Consumer)} */
    public static <V> void runBgr(Runnable bgraction, Runnable fxaction) {
        runBgr(() -> { bgraction.run(); return null; }, nothing->fxaction.run());
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
    
    

    
}
