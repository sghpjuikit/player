package sp.it.pl.util.async.executor;

import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * Provides factory methods for timers that are manipulated from and execute
 * their action on the JavaFX application thread.
 * <p/>
 *
 * @author Tomas Mikula
 */
public class FxTimer {

	private final Timeline timeline;
	private final Consumer<FxTimer> action;
	private Duration period;
	private long seq = 0;

	/**
	 * Creates a (stopped) timer that executes the given action specified number
	 * of times with a delay period.
	 *
	 * @param delay Time to wait before each execution. The first execution is already delayed.
	 * @param action action to execute
	 * @param cycles denotes number of executions. Use 1 for single execution, n for n executions and
	 * {@link javafx.animation.Transition#INDEFINITE} for infinite amount.
	 */
	public FxTimer(Duration delay, int cycles, Runnable action) {
		this.period = Duration.millis(delay.toMillis());
		this.timeline = new Timeline();
		this.action = t -> action.run();

		timeline.setCycleCount(cycles);
	}

	/**
	 * Equivalent to {@code new FxTimer(Duration.millis(delay), action, cycles);}
	 */
	public FxTimer(double delay, int cycles, Runnable action) {
		this(Duration.millis(delay), cycles, action);
	}

	public FxTimer(Duration delay, int cycles, Consumer<FxTimer> action) {
		this.period = Duration.millis(delay.toMillis());
		this.timeline = new Timeline();
		this.action = action;

		timeline.setCycleCount(cycles);
	}

	/**
	 * Equivalent to {@code new FxTimer(Duration.millis(delay), action, cycles);}
	 */
	public FxTimer(double delay, int cycles, Consumer<FxTimer> action) {
		this(Duration.millis(delay), cycles, action);
	}

	public void start() {
		start(period);
	}

	/**
	 * Equivalent to calling {@link #setPeriod(javafx.util.Duration)} and {@link #start()} subsequently.
	 */
	public void start(Duration period) {
		stop();
		long expected = seq;
		this.period = period;

		if (period.toMillis()==0)
			runNow();
		else {
			timeline.getKeyFrames().setAll(new KeyFrame(period, ae -> {
				if (seq==expected) {
					action.accept(this);
				}
			}));
			timeline.play();
		}
	}

	public void start(double periodInMs) {
		start(Duration.millis(periodInMs));
	}

	/** Equivalent to {@link #start()} or {@link #start()} when using true, respectively false. */
	public void setRunning(boolean b) {
		if (b) start();
		else stop();
	}

	public void runNow() {
		if (action!=null) action.accept(this);
	}

	public void pause() {
		timeline.pause();
	}

	public void unpause() {
		timeline.play();
	}

	public void stop() {
		timeline.stop();
		seq++;
	}

	/** Returns true if not stopped or paused. */
	public boolean isRunning() {
		return timeline.getCurrentRate()!=0;
	}

	/**
	 * Sets the delay for the task. Takes effect only if set before the task
	 * execution is planned. It will not affect currently running cycle. It will
	 * affect every subsequent cycle. Therefore, it is pointless to run this
	 * method if this timer is non-periodic.
	 */
	public void setPeriod(Duration period) {
		this.period = period;
	}

	public void setPeriod(double periodInMs) {
		this.period = Duration.millis(periodInMs);
	}

	public Duration getPeriod() {
		return period;
	}

	/**
	 * If timer running, executes {@link #start(javafx.util.Duration)}, else
	 * executes {@link #setPeriod(javafx.util.Duration)}
	 * <p/>
	 * Basically same as {@link #start(javafx.util.Duration)}, but restarts
	 * only if needed (when running).
	 */
	public void setTimeoutAndRestart(Duration timeout) {
		if (isRunning()) FxTimer.this.start(timeout);
		else FxTimer.this.setPeriod(timeout);
	}

	public void setTimeoutAndRestart(double timeoutInMillis) {
		if (isRunning()) FxTimer.this.start(Duration.millis(timeoutInMillis));
		else FxTimer.this.setPeriod(Duration.millis(timeoutInMillis));
	}
}