/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import utilities.functional.functor.Procedure;
import javafx.application.Platform;
import javafx.util.Duration;

/**
 * @author uranium
 * 
 * This object provides simple API for handling synchronous actions being invoked
 * once per time interval.
 */
public final class Timer {
    /**
     * name of the task executing in this object
     */
    private String name = "Timer";
    /**
     * the task executing
     */
    private Procedure task;
    /**
     * Time period, the task will execute once per
     */
    private long period = 1000;
    /**
     * The thread for running the task on.
     */
    private Thread thread;
    /**
     * Set true to ensure the task will always execute on ApplicationFX thread
     * (main thread). This is for convenience to shift the responsibility of
     * wrapping the code of the task in runLater().
     * Setting to false will mean the task executes on bgr thread.
     * It is recommended to use bgr threads when possible and minimize usage of
     * FX thread only to when necessary (such as GUI related tasks).
     * Default value: false
     */
    private boolean useFXThread = false;

    /**
     * Constructs the timer for synchronous task execution.
     * The task executes on bgr thread. It is recommended to use bgr threads 
     * when possible and minimize usage of FX thread only to when necessary 
     * (such as GUI related tasks). Shifting the computation weight to bgr
     * threads can avoid potential application locks during heavy computations.
     * @param name Name of the task executing in this object.
     * @param period Time period, the task will execute once per.
     * @param task The task executing. It is recommended to minimize computation
     * done in the task, particularly if it executes on ApplicationFX thread.
     */
    public Timer(String name, Duration period, Procedure task) {
        setName(name);
        setPeriod(period);
        setTask(task);
    }
    /**
     * Constructs the timer for synchronous task execution.
     * @param name Name of the task executing in this object.
     * @param period Time period, the task will execute once per.
     * @param task The task executing. It is recommended to minimize computation
     * done in the task, particularly if it executes on ApplicationFX thread.
     * @param useFXThread Set true to ensure the task will always execute on 
     * ApplicationFX thread (main thread). This is for convenience to shift the
     * responsibility of wrapping the code of the task in runLater().
     * Setting to false will mean the task executes on bgr thread. It is 
     * recommended to use bgr threads when possible and minimize usage of
     * FX thread only to when necessary (such as GUI related tasks). Shifting
     * the computation weight to bgr threads can avoid potential application
     * locks during heavy computations.
     */
    public Timer(String name, Duration period, Procedure task, boolean useFXThread) {
        setName(name);
        setPeriod(period);
        setTask(task);
        setUseFXThread(useFXThread);
    }

    public void start() {
        if (isRunning()) return;
        if (useFXThread)
            thread = new Thread(() -> {
                loopTaskOnFXThread();
            });
        else
            thread = new Thread(() -> {
                loopTask();
            }); 
        thread.setDaemon(true);
        thread.start();
    }
    
    public void stop() {
        if (!isRunning()) return;
        thread.interrupt();
    }
    
    public boolean isRunning() {
        return (thread != null && thread.isAlive());
    }
    private void loopTask() {
        try {
            Thread.sleep(period);
        } catch (InterruptedException ex) {
            Log.err(name + "thread interrupted");
            return;
        }
        executeTask();
        loopTask();
    }
    private void loopTaskOnFXThread() {
        try {
            Thread.sleep(period);
        } catch (InterruptedException ex) {
            Log.err(name + "thread interrupted");
            return;
        }
        executeTaskOnFXThread();
        loopTaskOnFXThread();
    }
    /**
     * Execute task once on ApplicationFX thread (main thread).
     * Use for tasks involving GUI.
     * This method is called repeatedly.
     */
    public void executeTaskOnFXThread() {
        if (task == null) return;
        Platform.runLater(() -> {
            getTask().run();
        });
    }
    public void executeTask() {
        if (task == null) return;
        getTask().run();
    }
    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    private void setName(String name) {
        this.name = name;
    }

    /**
     * @return the task
     */
    public Procedure getTask() {
        return task;
    }

    /**
     * @param task the task to set
     */
    private void setTask(Procedure task) {
        this.task = task;
    }

    /**
     * @return the period
     */
    public long getPeriod() {
        return period;
    }
    /**
     * @return the period
     */
    public double getPeriodAsDouble() {
        return period;
    }
    /**
     * @return the period
     */
    public Duration getPeriodAsDuration() {
        return Duration.millis(period);
    }
    /**
     * Period will not be set below 30 to avoid potential problems.
     * @param period the period to set
     */
    public void setPeriod(long period) {
        if (period < 30l) this.period = 30;
        this.period = period;
    }
    /**
     * Period will not be set below 30 to avoid potential problems.
     * @param period the period to set
     */
    public void setPeriod(double period) {
        setPeriod((long)period);
    }
    /**
     * Period will not be set below 30 miliseconds to avoid potential problems.
     * @param period the period to set
     */
    public void setPeriod(Duration period) {
        setPeriod((long)period.toMillis());
    }
    /**
     * Returns state of the property useFXThread
     * Property description:
     * Set true to ensure the task will always execute on ApplicationFX thread
     * (main thread). This is for convenience to shift the responsibility of
     * wrapping the code of the task in runLater().
     * Setting to false will mean the task executes on bgr thread.
     * It is recommended to use bgr threads when possible and minimize usage of
     * FX thread only to when necessary (such as GUI related tasks).
     * Default value: false
     * @return state of the property useFXThread
     */
    public boolean isFXThread() {
        return useFXThread;
    }
    /**
     * Returns whether the task will execute on bgr thread. Returns negated state
     * of the property useFXThread.
     * Property description:
     * Set true to ensure the task will always execute on ApplicationFX thread
     * (main thread). This is for convenience to shift the responsibility of
     * wrapping the code of the task in runLater().
     * Setting to false will mean the task executes on bgr thread.
     * It is recommended to use bgr threads when possible and minimize usage of
     * FX thread only to when necessary (such as GUI related tasks).
     * Default value: false
     * @return state of the property useFXThread
     */
    public boolean isBgrThread() {
        return !useFXThread;
    }
    /**
     * Sets useFXThread property to specified value.
     * Property description:
     * Set true to ensure the task will always execute on ApplicationFX thread
     * (main thread). This is for convenience to shift the responsibility of
     * wrapping the code of the task in runLater().
     * Setting to false will mean the task executes on bgr thread.
     * It is recommended to use bgr threads when possible and minimize usage of
     * FX thread only to when necessary (such as GUI related tasks).
     * Default value: false
     * @param val value to set.
     */
    private void setUseFXThread(boolean val) {
        useFXThread = val;
    }
    
}