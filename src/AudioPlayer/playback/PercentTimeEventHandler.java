/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback;

import utilities.functional.functor.Procedure;
import javafx.util.Duration;
import utilities.Log;

/**
 * 
 * @author uranium
 */
public class PercentTimeEventHandler {
        RealTimeProperty parent;
        private double percent;
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
        
        PercentTimeEventHandler(double p, Procedure b) {
            parent = PLAYBACK.realTimeProperty();
            percent = p;
            percMin = 0;
            percMax = 0;
            timeMin = Duration.ZERO;
            timeMax = Duration.ZERO;
            behavior = b;
        }
        
        void handle() {
            // precalculate values
            double total = parent.totalTime.get().toMillis();
            double real = parent.realTime.get().toMillis();
            double min = timeMin.toMillis();
            double max = (timeMax.greaterThan(Duration.ZERO)) ? timeMax.toMillis() : total+500;
            double pMax = (percMax==0) ? 2 : percMax; // disable if zero
            real -= parent.count*total; // get sub-total part of realTime
            double p = real/total;
            
            // fire event - do action
            if(p>=percMin && p<=pMax && real>=min && real<=max && p>=percent) {
                if (!fired) {
                    behavior.run();
                    fired = true;
                    Log.deb("PercentTimeEvent fired:");
                    Log.deb("   realTime "+parent.realTime.get().toSeconds());
                    Log.deb("   totalTime "+parent.totalTime.get().toSeconds());
                    Log.deb("   percent "+percent);
                    Log.deb("   count "+ parent.count);
                    Log.deb("   min "+percMin);
                    Log.deb("   max "+percMax);
                    Log.deb("   min "+timeMin.toSeconds());
                    Log.deb("   max "+timeMax.toSeconds());
                    }
            } else {
                if (fired)  { 
                    fired = false; }
            }
        }

    /**
     * Will always return number from interval <0;1>
     * @return Percent at which event is fired.
     */
    public double getPercent() {
        return percent;
    }

    /**
     * Percent at which event is fired.
     * Should be from interval <0;1>. If not it will be cut to nearest value
     * from that interval.
     * @param percent the percent to set
     */
    public void setPercent(double percent) {
        double p = percent;
        if (p>1) { p = 1; }
        if (p<0) { p = 0; }
        this.percent = p;
    }

    /**
     * Lower percent limit for event firing.
     * @return Lower percent limit for event firing.
     */
    public double getPercMin() {
        return percMin;
    }

    /**
     * Set to 0 to disable lower limit.
     * Set to 1 to prevent event from ever firing.
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
     * Upper percent limit for event firing.
     * Set to 1 to disable max value.
     * Set to 0 to prevent event from ever firing.
     * Should be from interval <0;1>. If not it will be cut to nearest value
     * from that interval.
     * @param percMax the percMax to set
     */
    public void setPercMax(double percMax) {
        double p = percMax;
        if (p>1) { p = 1; }
        if (p<0) { p = 0; }
        this.percMax = p;
    }

    /**
     * @return Lower time limit for event firing.
     */
    public Duration getTimeMin() {
        return timeMin;
    }

    /**
     * Lower time limit for event firing.
     * Set to Duration.ZERO to disable max time.
     * Min time should be lower than total time. If its set too high the event might
     * never fire for short audio items.
     * Min time should be smaller than max time. Otherwise the event never fires.
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
     * Upper time limit for event firing.
     * Set to Duration.ZERO to disable max time.
     * Min time should be smaller than max time. Otherwise the event never fires.
     * @param timeMax the timeMax to set
     */
    public void setTimeMax(Duration timeMax) {
        this.timeMax = timeMax;
    }
    
}
