/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback;

import java.util.function.Function;
import javafx.util.Duration;
import util.async.executor.FxTimer;

/**
 *
 * @author Plutonium_
 */
public class PlayTimeHandler {
    private final FxTimer timer;
    private final Function<Duration,Duration> dcal;

    public PlayTimeHandler(Function<Duration,Duration> dur_cal, Runnable action) {
        timer = new FxTimer(Duration.INDEFINITE, 1, action);
        dcal = dur_cal;
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
        timer.restart(dcal.apply(total_time));
    }
    
    public static PlayTimeHandler at(Duration at, Runnable action) {
        return new PlayTimeHandler(total -> at, action);
    }
    
    public static PlayTimeHandler at(double perc, Runnable action) {
        return new PlayTimeHandler(total -> total.multiply(perc), action);
    }
    
}
