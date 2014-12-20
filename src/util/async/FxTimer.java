

package util.async;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import org.reactfx.util.Timer;

/**
* Provides factory methods for timers that are manipulated from and execute
* their action on the JavaFX application thread.
* <p>
* @author Tomas Mikula
*/
public class FxTimer implements Timer {
    
    private final Timeline timeline;
    private final Runnable action;
    
    private Duration timeout;
    private long seq = 0;
    
    /**
    * Creates a (stopped) timer that executes the given action specified number
    * of times with a delay period.
    * @param delay Time to wait before each execution. The first execution is 
    * already delayed.
    * @param action action to execute
    * @param cycles denotes number of executions. Use 1 for single execution, n
    * for n executions and {@link Animation.INDEFINITE} (-1) for infinite amount.
    */
    public FxTimer(Duration delay, int cycles, Runnable action) {
        this.timeout = Duration.millis(delay.toMillis());
        this.timeline = new Timeline();
        this.action = action;

        timeline.setCycleCount(cycles);
    }
    /**
    * Equivalent to {@code new FxTimer(Duration.millis(delay), action, cycles);}
    */
    public FxTimer(double delay, int cycles, Runnable action) {
        this(Duration.millis(delay), cycles, action);
    }

    @Override
    public void restart() {
        restart(timeout);
    }
    
    /**
     * Equivalent to calling {@link #setTimeout()} and {@link #restart()}
     * subsequently.
     * @param timeout 
     */
    public void restart(Duration timeout) {
        stop();
        long expected = seq;
        this.timeout = timeout;
        timeline.getKeyFrames().setAll(new KeyFrame(timeout, ae -> {
            if(seq == expected) {
                action.run();
            }
        }));
        timeline.play();
    }

    public void restart(double timeoutInMillis) {
        restart(Duration.millis(timeoutInMillis));
    }
    
    @Override
    public void stop() {
        timeline.stop();
        seq++;
    }
    
    public boolean isRunning() {
        return timeline.getCurrentRate()!=0;
    }
    
    /**
     * Sets the delay for the task. Takes effect only if set before the task
     * execution is planned. It will not affect currently running cycle. It will
     * affect every subsequent cycle. Therefore, it is pointless to run this
     * method if this timer is non-periodic.
     * @param timeout 
     */
    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
    
    public void setTimeout(double timeautInMillis) {
        this.timeout = Duration.millis(timeautInMillis);
    }
    
    /**
     * If timer running, executes {@link #restart(javafx.util.Duration)}, else
     * executes {@link #setTimeout(javafx.util.Duration)}
     * <p>
     * Basically same as {@link #restart(javafx.util.Duration)}, but restarts
     * only if needed (when running).
     * 
     * @param timeout 
     */
    public void setTimeoutAndRestart(Duration timeout) {
        if (isRunning()) restart(timeout);
        else setTimeout(timeout);
    }
    
    public void setTimeoutAndRestart(double timeautInMillis) {
        if (isRunning()) restart(Duration.millis(timeautInMillis));
        else setTimeout(Duration.millis(timeautInMillis));
    }
}