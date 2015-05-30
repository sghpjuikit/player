
package AudioPlayer.playback;

import Action.IsAction;
import Action.IsActionable;
import AudioPlayer.Player;
import static AudioPlayer.playback.PlaycountIncrStrategy.*;
import AudioPlayer.services.Notifier.Notifier;
import AudioPlayer.services.Service;
import AudioPlayer.services.Tray.TrayService;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataWriter;
import Configuration.AppliesConfig;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import static java.awt.TrayIcon.MessageType.INFO;
import javafx.util.Duration;
import static javafx.util.Duration.millis;
import main.App;
import static util.functional.Util.max;
import static util.functional.Util.min;

/**
 *
 * @author uranium
 */
@IsConfigurable(value = "Playcount Incrementing")
@IsActionable
public class PlaycountIncrementer implements Service {
    
    @IsConfig(name="Incrementing strategy", info = "Playcount strategy for incrementing playback.")
    public static PlaycountIncrStrategy increment_playcount = ON_PERCENT;
    @IsConfig(name="Increment at percent", info = "Percent at which playcount is incremented.")
    public static double increment_playcount_at_percent = 0.4;
    @IsConfig(name="Increment at time", info = "Time at which playcount is incremented.")
    public static double increment_playcount_at_time = Duration.seconds(5).toMillis();
    @IsConfig(name="Show notification", info = "Shows notification when playcount is incremented.")
    public static boolean increment_pcnt_notif = false;
    @IsConfig(name="Show tray bubble", info = "Shows tray bubble notification when playcount is incremented.")
    public static boolean increment_pcnt_bubble = false;
    
    @IsAction(name = "Increment playcount", description = "Rises the number of times the song has been played by one.")
    private static void incrementPlayback() {
        Metadata m = Player.playingtem.get();
        if (!m.isEmpty() && m.isFileBased() ) {
            int p = m.getPlaycount()+1;
            MetadataWriter.use(m, w -> w.setPlaycount(p), ok -> {
                if(ok) {
                    if(increment_pcnt_notif) App.use(Notifier.class, n -> n.showTextNotification("Song playcount incremented to: " + p, "Update"));
                    if(increment_pcnt_bubble) App.use(TrayService.class, t -> t.showNotification("Tagger", "Playcount incrememted to: " + p, INFO));
                }
            });
        }
    };
    
    private static final Runnable incr = PlaycountIncrementer::incrementPlayback;
    private static PlayTimeHandler incrHand;
    
    
    @Override
    public void start() {
        configureIncrementation();
    }

    @Override
    public boolean isRunning() {
        return incrHand != null;
    }
    
    @Override
    public void stop() {
        removeOld();
        incrHand=null;
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
    private static void configureIncrementation() {
        removeOld();
        if (increment_playcount == ON_PERCENT) {
            incrHand = new PlayTimeHandler(total -> total.multiply(increment_playcount_at_percent),incr);
            PLAYBACK.addOnPlaybackAt(incrHand);
        } else if (increment_playcount == ON_TIME) {
            incrHand = new PlayTimeHandler(total -> millis(increment_playcount_at_time), incr);
            PLAYBACK.addOnPlaybackAt(incrHand);
        } else if (increment_playcount == ON_TIME_AND_PERCENT) {
            incrHand = new PlayTimeHandler(total -> min(millis(increment_playcount_at_time),total.multiply(increment_playcount_at_percent)),incr);
            PLAYBACK.addOnPlaybackAt(incrHand);
        } else if (increment_playcount == ON_TIME_OR_PERCENT) {
            incrHand = new PlayTimeHandler(total -> max(millis(increment_playcount_at_time),total.multiply(increment_playcount_at_percent)),incr);
            PLAYBACK.addOnPlaybackAt(incrHand);
        } else if (increment_playcount == ON_START) {
            PLAYBACK.addOnPlaybackStart(incr);
        } else if (increment_playcount == ON_END) {
            PLAYBACK.addOnPlaybackEnd(incr);
        } else if (increment_playcount == NEVER) {}
    }
    
    private static void removeOld() {
        if(incrHand!=null) PLAYBACK.removeOnPlaybackAt(incrHand);
        PLAYBACK.removeOnPlaybackStart(incr);
        PLAYBACK.removeOnPlaybackEnd(incr);
    }
}