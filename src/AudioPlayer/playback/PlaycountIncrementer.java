
package AudioPlayer.playback;

import Action.IsAction;
import Action.IsActionable;
import AudioPlayer.Player;
import AudioPlayer.services.Service;
import AudioPlayer.services.Tray.TrayService;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataWriter;
import AudioPlayer.tagging.Playcount;
import Configuration.AppliesConfig;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import static java.awt.TrayIcon.MessageType.INFO;
import javafx.util.Duration;
import main.App;
import util.dev.Log; 

/**
 *
 * @author uranium
 */
@IsConfigurable
@IsActionable
public class PlaycountIncrementer implements Service {
    
    @IsConfig(name="Incrementing strategy", info = "Playcount strategy for incrementing playback.")
    public static Playcount.IncrStrategy increment_playcount = Playcount.IncrStrategy.NEVER;
    @IsConfig(name="Increment at percent", info = "Percent at which playcount is incremented.")
    public static double increment_playcount_at_percent = 0.5;
    @IsConfig(name="Increment at time", info = "Time at which playcount is incremented.")
    public static double increment_playcount_at_time = Duration.seconds(5).toMillis();
    @IsConfig(name="Increment at min percent", info = "Minimum percent at which playcount is incremented.")
    public static double increment_playcount_min_percent = 0.0;
    @IsConfig(name="Increment at min time", info = "Minimum time at which playcount is incremented.")
    public static double increment_playcount_min_time = Duration.seconds(0).toMillis();
    
    @IsAction(name = "Increment playcount", description = "Rises the number of times the song has been played by one.")
    private static void incrementPlayback() {
        Metadata m = Player.playingtem.get();
        if (!m.isEmpty() && m.isFileBased() ) {
            MetadataWriter.useToIncrPlaycount(m);
            Log.info("Incrementing playount of played item.");
            App.use(TrayService.class, t -> t.showNotification("Tagger", "Playcount incrememted", INFO));
        }
    };
    
    private static PercentTimeEventHandler percIncrementer;
    private static TimeEventHandler timeIncrementer;
    private static Runnable pIncr = PlaycountIncrementer::incrementPlayback;
    
    
    
    @Override
    public void start() {
        // initialize percent incrementer
        percIncrementer = new PercentTimeEventHandler(increment_playcount_at_percent, pIncr, "Playcount percent event handler");
        percIncrementer.setPercMin(increment_playcount_min_percent);
        percIncrementer.setTimeMin(Duration.millis(increment_playcount_min_time));
        // initialize time incrementer
        timeIncrementer = new TimeEventHandler(Duration.millis(increment_playcount_at_time), pIncr, "Playcount time event handler");
        timeIncrementer.setPercMin(increment_playcount_min_percent);
        timeIncrementer.setTimeMin(Duration.millis(increment_playcount_min_time));
        
        configureIncrementation();
        rereadSettings();
    }

    @Override
    public boolean isRunning() {
        return pIncr != null;
    }
    
    @Override
    public void stop() {
        removeOld();
        pIncr=null;
    }

    @Override
    public boolean isDependency() {
        return false;
    }

    @Override
    public boolean isSupported() {
        return true;
    }
    
    
    @AppliesConfig("increment_playcount")
    @AppliesConfig("increment_playcount_at_percent")
    @AppliesConfig("increment_playcount_at_time")
    @AppliesConfig("increment_playcount_min_percent")
    @AppliesConfig("increment_playcount_min_time")
    private static void configureIncrementation() {
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
            PLAYBACK.addOnPlaybackStart(pIncr);
        } else if (increment_playcount == Playcount.IncrStrategy.ON_END) {
            removeOld();
            rereadSettings();
            PLAYBACK.addOnPlaybackEnd(pIncr);
        } else if (increment_playcount == Playcount.IncrStrategy.NEVER) {
            removeOld();
        }
    }
    
    private static void removeOld() {
        PLAYBACK.realTimeProperty().removeOnTimeAt(percIncrementer);
        PLAYBACK.realTimeProperty().removeOnTimeAt(timeIncrementer);
        PLAYBACK.removeOnPlaybackStart(pIncr);
        PLAYBACK.removeOnPlaybackEnd(pIncr);
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
