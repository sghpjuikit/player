/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback;

import java.util.function.Function;
import javafx.util.Duration;
import util.async.executor.FxTimer;

/** Handler for handling events at specific point of song playback. */
public class PlayTimeHandler {
    private final FxTimer timer;
    private final Function<Duration,Duration> cal;

    /***
     * @param when_calculator calculates when the even should fire. The function
     * receives total song length and returns time at which the action executes
     * @param action action to execute
     */
    public PlayTimeHandler(Function<Duration,Duration> when_calculator, Runnable action) {
        timer = new FxTimer(Duration.INDEFINITE, 1, action);
        cal = when_calculator;
    }
    
    public void pause() {
        timer.pause();
    }
    
    public void unpause() {
        timer.unpause();
    }
    
    public void stop() {
        timer.stop();
    }
    
    public void restart(Duration total_time) {
        timer.start(cal.apply(total_time));
    }
    
    
    public static PlayTimeHandler at(Duration at, Runnable action) {
        return new PlayTimeHandler(total -> at, action);
    }
    
    public static PlayTimeHandler at(double perc, Runnable action) {
        return new PlayTimeHandler(total -> total.multiply(perc), action);
    }
    
}
