
package AudioPlayer.playback;

import javafx.util.Duration;
import utilities.Log;
import utilities.functional.functor.Procedure;

/**
 *
 * @author uranium
 */
public class TimeEventHandler {
    
    private final String name;
    Duration atTime;
    Procedure behavior;
    // init to true -> failsafe
    // bad realTime initialization could (would) fire single 'initialization'
    // event (very bad thing) before the realTime is properly initialized
    // outside of the above, init value doesnt matter
    boolean fired = true;
    private double percMin;
    private double percMax;
    private Duration timeMin;
    private Duration timeMax;
    
    public TimeEventHandler(Duration at, Procedure b) {
        this(at, b, "");
    }
    
    public TimeEventHandler(Duration at, Procedure b, String name) {
        atTime = at;
        percMin = 0;
        percMax = 0;
        timeMin = Duration.ZERO;
        timeMax = Duration.ZERO;
        behavior = b;
        this.name = name;
    }
    
    void handle() {
        RealTimeProperty parent = PLAYBACK.realTimeProperty();
        // precalculate values
        double total = parent.totalTime.get().toMillis();
        double real = parent.realTime.get().toMillis();
        double at = atTime.toMillis();
        double min = timeMin.toMillis();
        double max = (timeMax.greaterThan(Duration.ZERO)) ? timeMax.toMillis() : total + 500; // disable if zero
        double pMax = (percMax == 0) ? 2 : percMax; // disable if zero
        real -= parent.count * total; // get sub-total part of realTime
        double p = real / total;
        // fire event - do action
        if (p >= percMin && p <= pMax && real >= min && real <= max && real >= at) {
            if (!fired) {
                behavior.run();
                fired = true;
                Log.deb("TimeEvent fired:");
                Log.deb("   realTime " + parent.realTime.get().toSeconds());
                Log.deb("   totalTime " + parent.totalTime.get().toSeconds());
                Log.deb("   atTime " + atTime.toSeconds());
                Log.deb("   count " + parent.count);
                Log.deb("   min " + percMin);
                Log.deb("   max " + percMax);
                Log.deb("   min " + timeMin.toSeconds());
                Log.deb("   max " + timeMax.toSeconds());
            }
        } else {
            if (fired) {
                fired = false;
            }
        }
    }
    
    /** @return Time at which event is fired. */
    public Duration getTimeAt() {
        return atTime;
    }
    
    /**
     * Shouldnt be too high, as it would cause event not to fire for shorter
     * items.
     * @param timeAt Time at which event is fired.
     */
    public void setTimeAt(Duration timeAt) {
        this.atTime = timeAt;
    }
    /**
     * Lower percent limit for event firing.
     *
     * @return Lower percent limit for event firing.
     */
    public double getPercMin() {
        return percMin;
    }
    
    /**
     * Set to 0 to disable lower limit. Set to 1 to prevent event from ever
     * firing.
     * @param percMin Lower percent limit for event firing.
     */
    public void setPercMin(double percMin) {
        this.percMin = percMin;
    }
    
    /**
     * Will always return number from interval <0;1>
     * @return Upper percent limit for event firing.
     */
    public double getPercMax() {
        return percMax;
    }
    
    /**
     * Upper percent limit for event firing. Set to 1 or 0 to disable max value.
     * Should be from interval <0;1>. If not it will be disabled.
     *
     * @param percMax the percMax to set
     */
    public void setPercMax(double percMax) {
        double p = percMax;
        if (p > 1) {
            p = 1;
        }
        if (p < 0) {
            p = 0;
        }
        this.percMax = p;
    }
    
    /** @return Lower time limit for event firing. */
    public Duration getTimeMin() {
        return timeMin;
    }
    
    /**
     * Lower time limit for event firing. Set to Duration.ZERO to disable max
     * time. Min time should be lower than total time. If its set too high the
     * event might never fire for short audio items. Min time should be smaller
     * than max time. Otherwise the event never fires.
     *
     * @param timeMin the timeMin to set
     */
    public void setTimeMin(Duration timeMin) {
        this.timeMin = timeMin;
    }
    
    /**
     * @return Upper time limit for event firing.
     */
    public Duration getTimeMax() {
        return timeMax;
    }
    
    /**
     * Upper time limit for event firing. Set to Duration.ZERO to disable max
     * time. Min time should be smaller than max time. Otherwise the event never
     * fires.
     *
     * @param timeMax the timeMax to set
     */
    public void setTimeMax(Duration timeMax) {
        this.timeMax = timeMax;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
