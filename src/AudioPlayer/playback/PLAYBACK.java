
package AudioPlayer.playback;

import Action.IsAction;
import Action.IsActionable;
import AudioPlayer.Player;
import AudioPlayer.playback.MediaSupport.GeneralPlayer;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.ItemSelection.PlayingItemSelector;
import AudioPlayer.playlist.ItemSelection.PlayingItemSelector.LoopMode;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.MetadataWriter;
import Configuration.Configurable;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import static java.lang.Double.max;
import static java.lang.Double.min;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import static javafx.scene.media.MediaPlayer.Status.PAUSED;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import javafx.util.Duration;
import util.File.Enviroment;

/**
 * Provides methods for player.
 */
@IsActionable
@IsConfigurable("Playback")
public final class PLAYBACK implements Configurable {
    @IsConfig(name="Remember playback state", info = "Continue last remembered playback when application starts.")
    public static boolean continuePlaybackOnStart = true;
    @IsConfig(name="Pause playback on start", info = "Continue last remembered playback paused on application start.")
    public static boolean continuePlaybackPaused = false;
    @IsConfig(name="Seek relative to length", info = "Seeks forward.backward by fraction of song's length instead of fixed time unit.")
    public static boolean seekPercent = true;    
    @IsConfig(name="Seek time unit", info = "Fixed time unit in milliseconds to jump, when seeking forward/backward.")
    public static long seekUnitT = 4000;
    @IsConfig(name="Seek fraction", info = "Relative time in fracton of song's length to seek forward/backward by.", min=0, max=1)
    public static double seekUnitP = 0.05;
    
    public static final PlaybackState state = Player.state.playback;
    static GeneralPlayer player = new GeneralPlayer();
    
    /** Initializes the Playback. */
    public static void initialize() {
        player.realTime.initialize();
        
        // add end of player behavior
        addOnPlaybackEnd(() -> {
            switch (state.loopMode.get()) {
                case OFF:       stop();
                                break;
                case PLAYLIST:  PlaylistManager.playNextItem();
                                break;
                case SONG:      seek(Duration.ZERO);
                                break;
                default:
            }
        });
    }
    
    /** Initialize state from last session */
    public static void loadLastState() {
        if(!continuePlaybackOnStart) return;
        if (PlaylistManager.getPlayingItem()==null) return;
        
        if (continuePlaybackPaused)
            state.status.set(Status.PAUSED);
        
        // create player
        player.createPlayback(PlaylistManager.getPlayingItem().getURI().toString(), state);
        if (state.status.get()== PAUSED || state.status.get()==PLAYING) {
            seek(state.currentTime.get());
        }
    }
    
    public static void suspend() {
        state.realTime.set(getRealTime());
        Player.state.serialize();
        player.dispose();
    }
    public static void activate() {
//        Player.state.
    }
/******************************************************************************/
    
    /**
     * Starts player of item.
     * It is safe to assume that application will have updated currently played
     * item after this method is invoked. The same is not guaranteed for cached
     * metadata of this item.
     * immediately after method is invoked, real time and current time are 0 and
     * all current song related information are updated and can be assumed to be
     * correctly initialized.
     * Invokation of this method fires playbackStart event.
     * @param item to play
     */
    public static void play(PlaylistItem item) {
        player.play(item);
    }
    
    /** Resumes player, if file is being played. Otherwise does nothing. */
    @IsAction(name = "Resume", description = "Resumes playback, if file is being played.", shortcut = "", global = true)
    public static void resume() {
        player.resume();
    }
    
    /** Pauses player, if already paused, does nothing. */
    @IsAction(name = "Pause", description = "Pauses playback, if file is being played.", shortcut = "", global = true)
    public static void pause() {
        player.pause();
    }
    
    /** Pauses/resumes player, if file is being played. Otherwise does nothing. */
    @IsAction(name = "Pause/resume", description = "Pauses/resumes playback, if file is being played.", shortcut = "ALT+S", global = true)
    public static void pause_resume() {
        player.pause_resume();
    }
    
    /** Stops player. */
    @IsAction(name = "Stop", description = "Stops playback.", shortcut = "ALT+F", global = true)
    public static void stop() {
        player.stop();
    }
    
    /** Seeks player to position specified by duration parameter. */
    public static void seek(Duration duration) {
        player.seek(duration);
    }

    /** Seeks player to position specified by percent value 0-1. */
    public static void seek(double at) {
        if(at<0 ||at>1) throw new IllegalArgumentException("Seek value must be 0-1");
        seek(getTotalTime().multiply(at));
        if(state.status.get()==PAUSED) player.pause_resume();
    }
    
    /** Seek forward by specified duration */
    @IsAction(name = "Seek to beginning", description = "Seek playback to beginning.", shortcut = "ALT+R", global = true)
    public static void seekZero() {
        seek(0);
    }
    
    /** Seek forward by specified duration */
    @IsAction(name = "Seek forward", description = "Seek forward playback.", shortcut = "ALT+D", continuous = true, global = true)
    public static void seekForward() {
        if(seekPercent) {
            double d = getCurrentTime().divide(getTotalTime().toMillis()).toMillis()+seekUnitP;
            seek(min(d, 1));
        } else
            seek(getCurrentTime().add(Duration.millis(seekUnitT)));
    }
    
    /** Seek backward by specified duration */
    @IsAction(name = "Seek backward", description = "Seek backward playback.", shortcut = "ALT+A", continuous = true, global = true)
    public static void seekBackward() {
        if(seekPercent) {
            double d = getCurrentTime().divide(getTotalTime().toMillis()).toMillis()-seekUnitP;
            seek(max(d, 0));
        } else
            seek(getCurrentTime().subtract(Duration.millis(seekUnitT)));
    }
    
    /** Seek forward by specified duration */
    @IsAction(name = "Seek to end", description = "Seek playback to end.", shortcut = "", global = true)
    public static void seekEnd() {
        seek(1);
    }
    
/******************************************************************************/
    
    public static double getVolume() {
        return state.volume.get();
    }
    
    public static double getVolumeMin() {
        return Volume.min();
    }
    
    public static double getVolumeMax() {
        return Volume.max();
    }
    
    public static void setVolume(double value) {
        state.volume.set(value);
    }

    /** Increment volume by elementary unit. */
    @IsAction(name = "Volume up", description = "Increment volume by elementary unit.", shortcut = "CTRL+SHIFT+2", continuous = true, global = true)
    public static void incVolume() {
        setVolume(getVolume()+0.05);
    }

    /** Decrement volume by elementary unit. */
    @IsAction(name = "Volume down", description = "Decrement volume by elementary unit.", shortcut = "CTRL+SHIFT+1", continuous = true, global = true)
    public static void decVolume() {
        setVolume(getVolume()-0.05);
    }
   
    public static DoubleProperty volumeProperty() {
        return state.volume.volumeProperty();
    }
    
    public static double getBalance() {
        return state.balance.get();
    }
    
    public static double getBalanceMin() {
        return Balance.min();
    }
    
    public static double getBalanceMax() {
        return Balance.max();
    }
    
    public static void setBalance(double value) {
        state.balance.set(value);
    }
    
    public static DoubleProperty balanceProperty() {
        return state.balance.balanceProperty();
    }
    
    public static PlayingItemSelector.LoopMode getLoopMode() {
        return state.loopMode.get();
    }
    
    @IsAction(name = "Toggle looping", description = "Switch between playlist looping mode.", shortcut = "ALT+L")
    public static void toggleLoopMode() {
        setLoopMode(getLoopMode().next());
    }
    
    public static void setLoopMode(LoopMode mode) {
        state.loopMode.set(mode);
        PlaylistManager.playingItemSelector.setSelector(mode.selector());
    }
    
    public static ObjectProperty<LoopMode> loopModeProperty() {
        return state.loopMode;
    }
   
    public static MediaPlayer.Status getStatus() {
        return state.status.get();
    }   
    
    public static ObjectProperty<Status> statusProperty() {
        return state.status;
    }    
    
    public static Duration getCurrentTime() {
        return state.currentTime.get();
    }
    
    public static ObjectProperty<Duration> currentTimeProperty() {
        return state.currentTime;
    }
    
    public static Duration getRealTime() {
        return player.realTime.get();
    }
    
    public static RealTimeProperty realTimeProperty() {
        return player.realTime;
    }
    
    public static Duration getRemainingTime() {
        return getTotalTime().subtract(getCurrentTime());
    }
    
    public static Duration getTotalTime() {
        return state.duration.get() ;
    }
    
    public static ObjectProperty<Duration> totalTimeProperty() {
        return state.duration;
    }
    
    public static double getPosition() {
        return getCurrentTime().toMillis()/getTotalTime().toMillis();
    }
    
    public static double getPlayedPercentage() {
        return getRealTime().toMillis()/getTotalTime().toMillis();
    }
    
    public static double getRate() {
        return state.rate.get();
    }

    public static void setRate(double value) {
        state.rate.set(value);
    }
    
    public static DoubleProperty rateProperty() {
        return state.rate;
    }   
    
    public static boolean isMute() {
        return state.mute.get();
    }
    
    public static boolean getMute() {
        return state.mute.get();
    }

    public static void setMute(boolean value) {
        state.mute.set(value);
    }
    
    /** Switches between on/off state for mute property. */
    @IsAction(name = "Toggle mute", description = "Switch mute on/off.", shortcut = "ALT+M")
    public static void toggleMute() {
        if (isMute()) setMute(false);
        else setMute(true);
    }
    
    public static BooleanProperty muteProperty() {
        return state.mute;
    }
    
    /**
     * Rates playing item specified by percentage rating.
     * @param rating <0,1> representing percentage of the rating, 0 being minimum
     * and 1 maximum possible rating for current item.
     */
    public static void rate(double rating) {
        if (PlaylistManager.isItemPlaying())
            MetadataWriter.useToRate(Player.playingtem.get(), rating);
    }
    
    /** Rate playing item 0/5. */
    @IsAction(name = "Rate playing 0/5", description = "Rate currently playing item 0/5.", shortcut = "ALT+BACK_QUOTE", global = true)
    public static void rate0() {
        rate(0);
    }
    /** Rate playing item 1/5. */
    @IsAction(name = "Rate playing 1/5", description = "Rate currently playing item 1/5.", shortcut = "ALT+1", global = true)
    public static void rate1() {
        rate(0.2);
    }
    /** Rate playing item 2/5. */
    @IsAction(name = "Rate playing 2/5", description = "Rate currently playing item 2/5.", shortcut = "ALT+2", global = true)
    public static void rate2() {
        rate(0.4);
    }
    /** Rate playing item 3/5. */
    @IsAction(name = "Rate playing 3/5", description = "Rate currently playing item 3/5.", shortcut = "ALT+3", global = true)
    public static void rate3() {
        rate(0.6);
    }
    /** Rate playing item 4/5. */
    @IsAction(name = "Rate playing 4/5", description = "Rate currently playing item 4/5.", shortcut = "ALT+4", global = true)
    public static void rate4() {
        rate(0.8);
    }
    /** Rate playing item 5/5. */
    @IsAction(name = "Rate playing 5/5", description = "Rate currently playing item 5/5.", shortcut = "ALT+5", global = true)
    public static void rate5() {
        rate(1);
    }
    /** Explore current item directory - opens file browser for its location. */
    @IsAction(name = "Explore current item directory", description = "Explore current item directory.", shortcut = "ALT+V", global = true)
    public static void openPlayedLocation() {
        Item i = PlaylistManager.getPlayingItem();
        Enviroment.browse(i==null ? null : i.getURI());
    }

//********************************** ON START *********************************/

    private static final List<Runnable> onStartHandlers = new ArrayList<>();
    
    public static final Runnable playbackStartDistributor = () -> onStartHandlers.forEach(Runnable::run);
    /**
     * Add behavior to behave every time song starts playing. Seeking to 
     * time 0 doesn't activate this event.
     * It is not safe to assume that application's information on currently played
     * item will be updated before this event. Therefore using cached information
     * like can result in misbehavior due to outdated information.
     * @param b 
     */
    public static void addOnPlaybackStart(Runnable b) {
        onStartHandlers.add(b);
    }
    
    /**
     * Remove behavior that executes when item starts playing.
     * @param b 
     */
    public static void removeOnPlaybackStart(Runnable b) {
        onStartHandlers.remove(b);
    }
    
//********************************** ON END ***********************************/

    private static final List<Runnable> onEndHandlers = new ArrayList<>();
    
    public static final Runnable playbackEndDistributor = () -> onEndHandlers.forEach(Runnable::run);
    /**
     * Add behavior that will execute when item player ends.
     * It is safe to use in-app cache of currently played item inside
     * the behavior parameter. Application's information will still apply during
     * playbackEnd event;
     * @param b 
     */
    public static void addOnPlaybackEnd(Runnable b) {
        onEndHandlers.add(b);
    }
    
    /**
     * Remove behavior that executes when item player ends.
     * @param b 
     */
    public static void removeOnPlaybackEnd(Runnable b) {
        onEndHandlers.remove(b);
    }
    
//******************************* SPECTRUM ************************************/
    
    private static final List<AudioSpectrumListener> spectrumListeners = new ArrayList<>();
    /**
     * Only one spectrum listener is allowed per player (MediaPlayer) object. Here
     * reregistering and ditributing of the event is handled.
     * Playback has main spectrum listener registered only if there is at least one
     * listener registered in the listener list.
     */
    public final static AudioSpectrumListener spectrumListenerDistributor = (double d, double d1, float[] floats, float[] floats1) -> {
        // distribute event to all listeners
        spectrumListeners.forEach(l->l.spectrumDataUpdate(d, d1, floats, floats1));
    };
    
    /**
     * Set audio spectrum listener to listen to spectrum changes.
     * Spectrum listener allows real-time observation of frequency bands of
     * played item.
     * @param l The listener.
     */
    public static void addAudioSpectrumListener(AudioSpectrumListener l) {
        spectrumListeners.add(l);
        if(spectrumListeners.size()==1)
            if(player.playback!=null)
                player.playback.setAudioSpectrumListener(spectrumListenerDistributor);
    }
    
    /**
     * Removes audio spectrum listener. 
     * @param l The listener.
     */
    public static void removeAudioSpectrumListener(AudioSpectrumListener l) {
        spectrumListeners.remove(l);
        if(spectrumListeners.isEmpty())
            if(player.playback!=null)
                player.playback.setAudioSpectrumListener(null);
    }
}
