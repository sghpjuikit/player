

package utilities;

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

    /**
    * Prepares a (stopped) timer with the given delay and action.
    */
    public static FxTimer create(Duration delay, Runnable action) {
        return new FxTimer(delay, action, 1);
    }

    /**
    * Equivalent to {@code create(delay, action).restart()}.
    */
    public static FxTimer run(Duration delay, Runnable action) {
        FxTimer timer = create(delay, action);
        timer.restart();
        return timer;
    }

    /**
    * Prepares a (stopped) timer that executes the given action periodically
    * with the given interval.
    */
    public static FxTimer createPeriodic(Duration interval, Runnable action) {
        return new FxTimer(interval, action, Animation.INDEFINITE);
    }

    /**
    * Equivalent to {@code createPeriodic(interval, action).restart()}.
    */
    public static FxTimer runPeriodic(Duration interval, Runnable action) {
        FxTimer timer = createPeriodic(interval, action);
        timer.restart();
        return timer;
    }

    
    private final Timeline timeline;
    private final Runnable action;
    
    private Duration timeout;
    private long seq = 0;

    private FxTimer(Duration timeout, Runnable action, int cycles) {
        this.timeout = Duration.millis(timeout.toMillis());
        this.timeline = new Timeline();
        this.action = action;

        timeline.setCycleCount(cycles);
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

    @Override
    public void stop() {
        timeline.stop();
        seq++;
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
}