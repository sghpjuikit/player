package util.graphics;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import javafx.geometry.Point2D;

import org.reactfx.Subscription;

import util.async.executor.FxTimer;

/**
 * Provides access to mouse position and mouse speed.
 * By default is lazy, i.e., consumes resources only if observed.
 */
public class MouseCapture {
    private final Set<Consumer<Point2D>> positionSubscribers = new HashSet<>();
    private final Set<DoubleConsumer> velocitySubscribers = new HashSet<>();
    private FxTimer pulse;
	private final int pulseFrequency = 10; // Hz
    private Point2D lastPos = null;
    private boolean calcSpeed = false;

    public Point2D getMousePosition() {
	    return Util.getMousePosition();
    }

    public Subscription observeMousePosition(Consumer<Point2D> action) {
        if (positionSubscribers.isEmpty() && velocitySubscribers.isEmpty())
            startPulse();
        positionSubscribers.add(action);
        return () -> unsubscribe(action);
    }

    /** Measured in px/second. */
    public Subscription observeMouseVelocity(DoubleConsumer action) {
        if (positionSubscribers.isEmpty() && velocitySubscribers.isEmpty())
            startPulse();
        velocitySubscribers.add(action);
        calcSpeed = true;
        return () -> unsubscribe(action);
    }

    private void startPulse() {
        if (pulse==null) pulse = new FxTimer(1000/pulseFrequency, -1, () -> {
            Point2D p = getMousePosition();
            positionSubscribers.forEach(s -> s.accept(p));
            if (calcSpeed && lastPos!=null) {
                double speed = p.distance(lastPos)*pulseFrequency;
                velocitySubscribers.forEach(s -> s.accept(speed));
            }
            lastPos = p;
        });
        if (!pulse.isRunning()) pulse.start();
    }

    private void stopPulse() {
        if (pulse!=null) {
            pulse.stop();
            pulse = null;
        }
    }

    private void unsubscribe(Object s) {
        positionSubscribers.remove(s);
        velocitySubscribers.remove(s);
        calcSpeed = !velocitySubscribers.isEmpty();
        if (positionSubscribers.isEmpty() && velocitySubscribers.isEmpty() && pulse!=null)
            stopPulse();
    }

}