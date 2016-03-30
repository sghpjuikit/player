package util.graphics;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import javafx.geometry.Point2D;

import org.reactfx.Subscription;

import com.sun.glass.ui.Robot;

import util.access.V;
import util.async.executor.FxTimer;
import util.dev.TODO;

import static util.dev.TODO.Purpose.UNTESTED;

/**
 * Provides access to mouse position and mouse speed.
 * By default is lazy, i.e., consumes resources only if observed.
 */
@TODO(purpose = UNTESTED, note = "Make sure the class is thread safe")
public class MouseCapture {
    private Robot robot;
    private final Set<Consumer<Point2D>> positionSubscribers = new HashSet<>();
    private final Set<DoubleConsumer> velocitySubscribers = new HashSet<>();
    private FxTimer pulse;
    private Point2D lastPos = null;
    private boolean calcSpeed = false;
    private Subscription lazy;
    /**
     * Denotes lazyness. Performance optimization.
     * Use false when it is expected that mouse will not be observed and a
     * lot of mouse position queries will be invoked.
     * True by default.
     * <p/>
     * If true, the resources used will be initialized and destroyed on
     * each mouse position query, unless mouse (speed or position) is observed.
     * If false, the resources will will live even if the mouse is not observed and at least until value changes to
     * true, possibly longer, depending on whether mouse is observed.
     */
    public V<Boolean> isLazy = new V<>(true, is -> {
        if(is) lazy.unsubscribe();
        else lazy = observeMousePosition(pos -> {});
    });

    public Point2D getMousePosition() {
        boolean dispose = robot==null;
        Robot r = robot==null ? com.sun.glass.ui.Application.GetApplication().createRobot() : robot;
        Point2D p = new Point2D(r.getMouseX(), r.getMouseY());
        if(dispose) r.destroy();
        return p;
    }

    public Subscription observeMousePosition(Consumer<Point2D> action) {
        if(positionSubscribers.isEmpty() && velocitySubscribers.isEmpty())
            startPulse();
        positionSubscribers.add(action);
        return () -> unsubscribe(action);
    }

    /** Measured in px/second. */
    public Subscription observeMouseVelocity(DoubleConsumer action) {
        if(positionSubscribers.isEmpty() && velocitySubscribers.isEmpty())
            startPulse();
        velocitySubscribers.add(action);
        calcSpeed = true;
        return () -> unsubscribe(action);
    }

    private void startPulse() {
        if(pulse==null) pulse = new FxTimer(100, -1, () -> {
            Point2D p = getMousePosition();
            positionSubscribers.forEach(s -> s.accept(p));
            if(calcSpeed && lastPos!=null) {
                double speed = p.distance(lastPos)*10;
                velocitySubscribers.forEach(s -> s.accept(speed));
            }
            lastPos = p;
        });
        if(!pulse.isRunning()) pulse.start();
    }

    private void stopPulse() {
        if(pulse!=null) {
            pulse.stop();
            pulse = null;
        }
    }

    private void unsubscribe(Object s) {
        positionSubscribers.remove(s);
        velocitySubscribers.remove(s);
        calcSpeed = !velocitySubscribers.isEmpty();
        if(positionSubscribers.isEmpty() && velocitySubscribers.isEmpty() && pulse!=null)
            stopPulse();
    }

}