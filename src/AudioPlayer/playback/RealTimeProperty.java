
package AudioPlayer.playback;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Duration;
import static javafx.util.Duration.ZERO;
import utilities.FxTimer;

/**
 *
 * @author uranium
 */
public final class RealTimeProperty {
    
    final ObjectProperty<Duration> totalTime;
    final ObjectProperty<Duration> currentTime;
    final ObjectProperty<Duration> realTime = new SimpleObjectProperty(ZERO);
    int count = 0;
    
    // temp
    public Duration curr_sek = ZERO;
    public Duration real_seek = ZERO;
    boolean eventT_fired = false; // prevention so event fires only once
    boolean eventP_fired = false; // prevention so event fires only once
    
    public RealTimeProperty(ObjectProperty<Duration> _totalTime, ObjectProperty<Duration> _currentTime) {
        totalTime = _totalTime;
        currentTime = _currentTime;
    }
    
    void initialize() {
        // bind realTime to playback
        currentTime.addListener(o -> {
            Duration d = real_seek.add(currentTime.get().subtract(curr_sek));
            realTime.set(d);
        });
        
        // update count
        setOnTimeAt(new PercentTimeEventHandler(1, () -> count++, "RealTimeProperty count"));

        eventDistributorPulse.restart();
        // make sure time ZERO and TOTAL execute (outside of pulse)
        PLAYBACK.addOnPlaybackEnd(eventExecutor);
        PLAYBACK.addOnPlaybackStart(eventExecutor);
    }
    
    void synchroRealTime_onPlayed() {
        real_seek = ZERO;
        curr_sek = ZERO;
        count = 0;
        eventExecutor.run();
        eventDistributorPulse.restart();
    }
    
    void synchroRealTime_onStopped() {
        real_seek = Duration.ZERO;
        curr_sek = Duration.ZERO;
        count = 0;
        eventDistributorPulse.stop();
        eventExecutor.run();
    }
    
    void synchroRealTime_onPreSeeked() {
        real_seek = realTime.get();
    }
    
    void synchroRealTime_onPostSeeked(Duration duration) {
        curr_sek = duration;
        eventExecutor.run();
    }
    
/******************************************************************************/
    
    /**
     * @return value of this property
     */
    public Duration get() {
        return realTime.get();
    }
    
    public ObjectProperty<Duration> realTimeProperty() {
        return realTime;
    }
    
    public void addListener(InvalidationListener listener) {
        realTime.addListener(listener);
    }
    
    public void removeListener(InvalidationListener listener) {
        realTime.removeListener(listener);
    }
    
    /**
     * Registers specified behavior to behave when real played time of the
     * played item reaches certain portion of total time. For example
     * doSometing(); when item has been playing for exactly 50% of its total
     * time.
     * @param h
     */
    public void setOnTimeAt(PercentTimeEventHandler h) {
        if (handlers.isEmpty())
            realTime.addListener(eventInvalidator);
        
        handlers.add(h);
    }
    
    /**
     * Removes the handler if it has been registered before.
     * @param h
     */
    public void removeOnTimeAt(PercentTimeEventHandler h) {
        if (h != null)
            handlers.remove(h);
        
        if (handlers.isEmpty())
            realTimeProperty().removeListener(eventInvalidator);
    }
    
    /**
     * Registers specified behavior to behave when real played time of the
     * played item reaches certain point. For example doSometing(); when item
     * has been playing for exactly 10 seconds
     * @param h
     */
    public void setOnTimeAt(TimeEventHandler h) {
        if (handlers.isEmpty())
            realTime.addListener(eventInvalidator);
        
        handlers.add(h);
    }
    
    /**
     * Removes the handler if it has been registered before.
     * @param h
     */
    public void removeOnTimeAt(TimeEventHandler h) {
        if (h != null)
            handlers.remove(h);
            
        if (handlers.isEmpty())
            realTimeProperty().removeListener(eventInvalidator);
    }
//*****************************************************************************/
    
    private final List<DurationHandler> handlers = new ArrayList();
    
    private boolean invalidated = false;
    private final InvalidationListener eventInvalidator = o -> invalidated = true;
    private final Runnable eventExecutor = () -> handlers.forEach(DurationHandler::handle);
    private final FxTimer eventDistributorPulse = FxTimer.createPeriodic(Duration.millis(500), () -> {
        if (invalidated) eventExecutor.run();
        invalidated = false;
    });
    

}
