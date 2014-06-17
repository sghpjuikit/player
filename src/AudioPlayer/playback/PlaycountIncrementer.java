/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback;

import AudioPlayer.Player;
import AudioPlayer.tagging.MetadataWriter;
import AudioPlayer.tagging.Playcount;
import Configuration.Configuration;
import javafx.util.Duration;
import utilities.Log;
import utilities.functional.functor.Procedure;

/**
 *
 * @author uranium
 */
public class PlaycountIncrementer {

    // playcount incrementing settings
    private double atP = Configuration.increment_playcount_at_percent;
    private double pMin = Configuration.increment_playcount_min_percent;
    private Duration atT = Duration.millis(Configuration.increment_playcount_at_time);
    private Duration tMin = Duration.millis(Configuration.increment_playcount_min_time);
    // handlers
    private final PercentTimeEventHandler percIncrementer;
    private final TimeEventHandler timeIncrementer;
    // behavior
    private final Procedure incrementPlayback = () -> {
        // prevent reading when not initialized
        if (Player.getCurrentMetadata() == null) {
            return;
        }
        // increment
        MetadataWriter.incrementPlaycount(Player.getCurrentMetadata());
        Log.mess("Incrementing playount of played item.");
    };

    PlaycountIncrementer() {
        // initialize percent incrementer
        percIncrementer = new PercentTimeEventHandler(atP, incrementPlayback, "Playcount percent event handler");

        percIncrementer.setPercMin(pMin);
        percIncrementer.setTimeMin(tMin);
        // initialize time incrementer
        timeIncrementer = new TimeEventHandler(atT, incrementPlayback, "Playcount time event handler");

        timeIncrementer.setPercMin(pMin);
        timeIncrementer.setTimeMin(tMin);
    }

    public void configureIncrementation() {
        Log.mess("Resetting playcount incrementer settings.");
        if (Configuration.increment_playcount == Playcount.IncrStrategy.ON_PERCENT) {
            removeOld();
            rereadSettings();
            PLAYBACK.realTimeProperty().setOnTimeAt(percIncrementer);
        } else if (Configuration.increment_playcount == Playcount.IncrStrategy.ON_TIME) {
            removeOld();
            rereadSettings();
            PLAYBACK.realTimeProperty().setOnTimeAt(timeIncrementer);
        } else if (Configuration.increment_playcount == Playcount.IncrStrategy.ON_START) {
            removeOld();
            rereadSettings();
            PLAYBACK.addOnPlaybackStart(incrementPlayback);
        } else if (Configuration.increment_playcount == Playcount.IncrStrategy.ON_END) {
            removeOld();
            rereadSettings();
            PLAYBACK.addOnPlaybackEnd(incrementPlayback);
        } else if (Configuration.increment_playcount == Playcount.IncrStrategy.NEVER) {
            removeOld();
        }
    }

    private void removeOld() {
        PLAYBACK.realTimeProperty().removeOnTimeAt(percIncrementer);
        PLAYBACK.realTimeProperty().removeOnTimeAt(timeIncrementer);
        PLAYBACK.removeOnPlaybackStart(incrementPlayback);
        PLAYBACK.removeOnPlaybackEnd(incrementPlayback);
    }

    private void rereadSettings() {
        atP = Configuration.increment_playcount_at_percent;
        pMin = Configuration.increment_playcount_min_percent;
        atT = Duration.millis(Configuration.increment_playcount_at_time);
        tMin = Duration.millis(Configuration.increment_playcount_min_time);
        percIncrementer.setPercent(atP);
        percIncrementer.setPercMin(pMin);
        percIncrementer.setTimeMin(tMin);
        timeIncrementer.setTimeAt(atT);
        timeIncrementer.setPercMin(pMin);
        timeIncrementer.setTimeMin(tMin);
    }
}
