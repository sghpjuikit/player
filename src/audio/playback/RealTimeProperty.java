
package audio.playback;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Duration;
import static javafx.util.Duration.ZERO;

/**
 *
 * @author uranium
 */
public final class RealTimeProperty {
    
    final ObjectProperty<Duration> totalTime;
    final ObjectProperty<Duration> currentTime;
    final ObjectProperty<Duration> realTime = new SimpleObjectProperty(ZERO);
    
    // temp
    public Duration curr_sek = ZERO;
    public Duration real_seek = ZERO;
    
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
    }
    
    public void synchroRealTime_onPlayed() {
        real_seek = ZERO;
        curr_sek = ZERO;
    }
    
    public void synchroRealTime_onStopped() {
        real_seek = Duration.ZERO;
        curr_sek = Duration.ZERO;
    }
    
    public void synchroRealTime_onPreSeeked() {
        real_seek = realTime.get();
    }
    
    public void synchroRealTime_onPostSeeked(Duration duration) {
        curr_sek = duration;
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

}
