
package AudioPlayer.playback;

import AudioPlayer.Player;
import AudioPlayer.tagging.MetadataWriter;
import AudioPlayer.tagging.Playcount;
import Configuration.AppliesConfig;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import javafx.util.Duration;
import util.Log; 

/**
 *
 * @author uranium
 */
@IsConfigurable
public class PlaycountIncrementer {
    
    @IsConfig(name="Playcount incrementing strategy", info = "Playcount strategy for incrementing playback.")
    public static Playcount.IncrStrategy increment_playcount = Playcount.IncrStrategy.NEVER;
    @IsConfig(name="Playcount incrementing at percent", info = "Percent at which playcount is incremented.")
    public static double increment_playcount_at_percent = 0.5;
    @IsConfig(name="Playcount incrementing at time", info = "Time at which playcount is incremented.")
    public static double increment_playcount_at_time = Duration.seconds(5).toMillis();
    @IsConfig(name="Playcount incrementing min percent", info = "Minimum percent at which playcount is incremented.")
    public static double increment_playcount_min_percent = 0.0;
    @IsConfig(name="Playcount incrementing min time", info = "Minimum time at which playcount is incremented.")
    public static double increment_playcount_min_time = Duration.seconds(0).toMillis();
        
    // handlers
    private static PercentTimeEventHandler percIncrementer;
    private static TimeEventHandler timeIncrementer;
    
    // behavior
    private static final Runnable incrementPlayback = () -> {
        // prevent reading when not initialized
        if (Player.playingtem == null) {
            return;
        }
        // increment
        MetadataWriter.useToIncrPlaycount(Player.playingtem.get());
        Log.info("Incrementing playount of played item.");
    };
    
    public static void initialize() {
        // initialize percent incrementer
        percIncrementer = new PercentTimeEventHandler(increment_playcount_at_percent, incrementPlayback, "Playcount percent event handler");
        percIncrementer.setPercMin(increment_playcount_min_percent);
        percIncrementer.setTimeMin(Duration.millis(increment_playcount_min_time));
        // initialize time incrementer
        timeIncrementer = new TimeEventHandler(Duration.millis(increment_playcount_at_time), incrementPlayback, "Playcount time event handler");
        timeIncrementer.setPercMin(increment_playcount_min_percent);
        timeIncrementer.setTimeMin(Duration.millis(increment_playcount_min_time));
    }
    
    @AppliesConfig( "increment_playcount")
    @AppliesConfig( "increment_playcount_at_percent")
    @AppliesConfig( "increment_playcount_at_time")
    @AppliesConfig( "increment_playcount_min_percent")
    @AppliesConfig( "increment_playcount_min_time")
    public static void configureIncrementation() {
        Log.info("Resetting playcount incrementer settings.");
        if (increment_playcount == Playcount.IncrStrategy.ON_PERCENT) {
            removeOld();
            rereadSettings();
            PLAYBACK.realTimeProperty().setOnTimeAt(percIncrementer);
        } else if (increment_playcount == Playcount.IncrStrategy.ON_TIME) {
            removeOld();
            rereadSettings();
            PLAYBACK.realTimeProperty().setOnTimeAt(timeIncrementer);
        } else if (increment_playcount == Playcount.IncrStrategy.ON_START) {
            removeOld();
            rereadSettings();
            PLAYBACK.addOnPlaybackStart(incrementPlayback);
        } else if (increment_playcount == Playcount.IncrStrategy.ON_END) {
            removeOld();
            rereadSettings();
            PLAYBACK.addOnPlaybackEnd(incrementPlayback);
        } else if (increment_playcount == Playcount.IncrStrategy.NEVER) {
            removeOld();
        }
    }
    
    private static void removeOld() {
        PLAYBACK.realTimeProperty().removeOnTimeAt(percIncrementer);
        PLAYBACK.realTimeProperty().removeOnTimeAt(timeIncrementer);
        PLAYBACK.removeOnPlaybackStart(incrementPlayback);
        PLAYBACK.removeOnPlaybackEnd(incrementPlayback);
    }
    
    private static void rereadSettings() {
        percIncrementer.setPercent(increment_playcount_at_percent);
        percIncrementer.setPercMin(increment_playcount_min_percent);
        percIncrementer.setTimeMin(Duration.millis(increment_playcount_min_time));
        timeIncrementer.setTimeAt(Duration.millis(increment_playcount_at_time));
        timeIncrementer.setPercMin(increment_playcount_min_percent);
        timeIncrementer.setTimeMin(Duration.millis(increment_playcount_min_time));
    }
}
