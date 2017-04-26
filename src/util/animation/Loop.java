package util.animation;

import java.util.function.LongConsumer;
import javafx.animation.AnimationTimer;

/**
 * Timer, that executes behavior in each frame while it is running.
 * <p/>
 * The methods {@link #start()} and {@link #stop()} allow to start and stop the timer.
 */
public class Loop {
	private boolean active;
	private final LongConsumer action;
	private final AnimationTimer timer = new AnimationTimer() {
		@Override
		public void handle(long l) {
			doLoop(l);
		}
	};

	/**
	 * Creates a new loop.
	 *
	 * @param action behavior to execute. Takes 1 parameter - The timestamp of the current frame given in nanoseconds.
	 * This value will be the same for all {@code AnimationTimers} called during one frame.
	 */
	public Loop(LongConsumer action) {
		this.action = action;
	}

	/** Creates a new loop. */
	public Loop(Runnable action) {
		this.action = now -> action.run();
	}

	/** Starts this loop. Once started, the behavior will be called in every frame. */
	public void start() {
		if (!active) {
			active = true;
			timer.start();
		}
	}

	/** Stops this loop. It can be activated again by calling {@link #start()}. */
	public void stop() {
		if (active) {
			timer.stop();
			active = false;
		}
	}

	/**
	 * This method needs to be overridden by extending classes. It is going to be called in every frame while the
	 * AnimationTimer is active.
	 *
	 * @param now The timestamp of the current frame given in nanoseconds. This value will be the same for all
	 * AnimationTimers called during one frame.
	 */
	protected void doLoop(long now) {
		action.accept(now);
	}
}