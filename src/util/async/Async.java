/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.async;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.util.Duration;

import util.async.executor.FxTimer;

import static javafx.animation.Animation.INDEFINITE;
import static util.dev.Util.no;

/**
 *
 * @author Plutonium_
 */
public final class Async {

    public static Consumer<Runnable> FX = Async::runFX;
    public static Consumer<Runnable> FXLATER = Async::runLater;
    public static Consumer<Runnable> NEW = Async::runNew;
    public static Consumer<Runnable> CURR = Async::run;
    public static Consumer<Runnable> FXAFTER(double delay) {
        return r -> runFX(delay, r);
    }
    public static Consumer<Runnable> FXAFTER(Duration delay) {
        return r -> runFX(delay, r);
    }


    public static Executor eFX = Async.FX::accept;
    public static Executor eFXLATER = Async.FXLATER::accept;
    public static Executor eBGR = Async.NEW::accept;
    public static Executor eCURR = Async.CURR::accept;

/****************************************** RUNNABLE **********************************************/

    /** Sleeps currently executing thread for specified duration. When interrupted, returns. */
    public static void sleep(Duration d) {
        try {
            Thread.sleep((long)d.toMillis());
        } catch (InterruptedException ex) {}
    }

    /** Sleeps currently executing thread for specified number of milliseconds. When interrupted, returns. */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {}
    }

    /** Runnable that invokes {@link #sleep(javafx.util.Duration)}. */
    public static Runnable sleeping(Duration d) {
        return () -> sleep(d);
    }

/****************************************** EXECUTORS *********************************************/

    /**
     * Executes the runnable immediately on current thread.
     * Equivalent to
     * <pre>{@code
     *   r.run();
     * </pre>
     */
    public static void run(Runnable r) {
        r.run();
    }

    /**
     * Executes the action on current thread after specified delay from now.
     * Equivalent to {@code new FxTimer(delay, action, 1).restart();}.
     * @param delay delay
     */
    public static void run(Duration delay, Runnable action) {
        new FxTimer(delay, 1, action).start();
    }

    /**
     * Executes the action on current thread after specified delay from now.
     * Equivalent to {@code new FxTimer(delay, action, 1).restart();}.
     * @param delay delay in milliseconds
     */
    public static void run(double delay, Runnable action) {
        new FxTimer(delay, 1, action).start();
    }

    /**
     * Executes the action on current thread repeatedly with given time period.
     * Equivalent to {@code new FxTimer(delay, action, INDEFINITE).restart();}.
     * @param delay delay
     * @param action action. Takes the timer as a parameter. Use it to stop the
     * periodic execution. Otherwise it will never stop !
     */
    public static void runPeriodic(Duration period, Consumer<FxTimer> action) {
        new FxTimer(period, INDEFINITE, action).start();
    }

    /**
     * Executes the runnable immediately on a new daemon thread.
     * Equivalent to
     * <pre>{@code
     *   Thread thread = new Thread(action);
     *   thread.setDaemon(true);
     *   thread.start();
     * }</pre>
     */
    public static void runNew(Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.start();
    }

    public static void runAfter(Duration delay, Consumer<Runnable> executor, Runnable r) {
        if(delay.lessThanOrEqualTo(Duration.ZERO)) {
            executor.accept(r);
        } else {
            executor.accept(() -> {
                if(Platform.isFxApplicationThread()) {
                    new FxTimer(delay, 1, r).start();
                } else {
                    try {
                        Thread.sleep((long)delay.toMillis());
                        r.run();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Async.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }
    public static void runNewAfter(Duration delay, Runnable r) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep((long)delay.toMillis());
                r.run();
            } catch (InterruptedException ex) {
                Logger.getLogger(Async.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Executes runnable on fx thread, immediately id called on fx thread, or
     * using Platform.runLater() otherwise.
     * <p>
     * Use to execute the action on fx as soon as possible.
     * <p>
     * Equivalent to
     * <pre>{@code
     *   if(Platform.isFxApplicationThread())
     *       r.run();
     *   else
     *       Platform.runLater(r);
     * }</pre>
     */
    public static void runFX(Runnable r) {
        if(Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }

    public static void runNotFX(Runnable r) {
        if(Platform.isFxApplicationThread()) runNew(r);
        else r.run();
    }

    /**
     * Executes the action on fx thread after specified delay from now.
     * @param delay delay in milliseconds
     */
    public static void runFX(double delay, Runnable r) {
        no(delay<0);
        if(delay==0) runFX(r);
        else new FxTimer(delay, 1, () -> runFX(r)).start();
    }

    public static void runFX(double delay1, Runnable r1, double delay2, Runnable r2) {
        no(delay1<0);
        runFX(delay1, () -> {
            r1.run();
            runFX(delay2,r2);
        });
    }

    /**
     * Executes the action on fx thread after specified delay from now.
     * @param delay delay
     */
    public static void runFX(Duration delay, Runnable r) {
        new FxTimer(delay, 1, () -> Async.runFX(r)).start();
    }

    /**
     * Executes the runnable on fx thread at unspecified time in the future.
     * <p>
     * Use to execute the action on fx thread, but not immediately. In practice
     * the delay is very small.
     * <p>
     * Equivalent to
     * <pre>{@code
     *   Platform.runLater(r);
     * }</pre>
     */
    public static void runLater(Runnable r) {
        Platform.runLater(r);
    }

    public static ExecutorService newSingleDaemonThreadExecutor() {
        return Executors.newSingleThreadExecutor(threadFactory(true));
    }

    public static ThreadFactory threadFactory(boolean daemon) {
        return r -> {
            Thread t = new Thread(r);
            t.setDaemon(daemon);
            return t;
        };
    }

    public static ThreadFactory threadFactory(String name, boolean daemon) {
        return r -> {
            Thread t = new Thread(r);
            t.setName(name);
            t.setDaemon(daemon);
            return t;
        };
    }

}
