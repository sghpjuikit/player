
package AudioPlayer.playback;

import Action.IsAction;
import Action.IsActionable;
import AudioPlayer.Player;
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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import static javafx.scene.media.MediaPlayer.Status.PAUSED;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.scene.media.MediaPlayer.Status.STOPPED;
import javafx.util.Duration;
import utilities.Log;
import utilities.Parser.File.Enviroment;
import utilities.functional.functor.Procedure;

/**
 * Provides methods for playback.
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
    
    private static final PlaybackState state = Player.state.playback;
    static MediaPlayer playback;
    private static final PlaybackCore core = new PlaybackCore();
    
    private static final RealTimeProperty realTime = 
            new RealTimeProperty(state.durationProperty(), state.currentTimeProperty());
    
    
    /** Initializes the Playback. */
    public static void initialize() {
        realTime.initialize();
        
        // add end of playback behavior
        core.addOnPlaybackEnd(() -> {
            switch (state.getLoopMode()) {
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
            state.setStatus(Status.PAUSED);
        
        // create playback
        createPlayback(PlaylistManager.getPlayingItem().getURI().toString());
        if (state.getStatus() == PAUSED || state.getStatus() == PLAYING) {
            seek(state.getCurrentTime());
        }
    }
    
    public static void suspend() {
        state.setRealTime(getRealTime());
        Player.state.serialize();
        destroyPlayback();
    }
    public static void activate() {
//        Player.state.
    }
/******************************************************************************/
    
    /**
     * Starts playback of item.
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
        // properly end previous
        destroyPlayback();
        // handle corrupted
        if (item.isCorrupt()) { 
            PlaylistManager.playItem(item); // will handle 'corruptness' within playlist
            return;
        }
        // play
        createPlayback(item.getURI().toString());
        playback.play();
        
        realTime.synchroRealTime_onPlayed();
        // fire playing item change event
        Player.playingtem.itemChanged(item);
        PlaylistManager.setPlayingItem(item);
        core.playbackStartDistributor.run();
    }
    
    /** Resumes playback, if file is being played. Otherwise does nothing. */
    @IsAction(name = "Resume", description = "Resumes playback, if file is being played.", shortcut = "", global = true)
    public static void resume() {
        if (playback.getMedia() == null) return;
        playback.play();
    }
    
    /** Pauses playback, if already paused, does nothing. */
    @IsAction(name = "Pause", description = "Pauses playback, if file is being played.", shortcut = "", global = true)
    public static void pause() {
        if (playback.getMedia() == null) return;
        playback.pause();
    }
    
    /** Pauses/resumes playback, if file is being played. Otherwise does nothing. */
    @IsAction(name = "Pause/resume", description = "Pauses/resumes playback, if file is being played.", shortcut = "ALT+S", global = true)
    public static void pause_resume() {
        if (playback == null || playback.getMedia() == null) return;

        if (playback.getStatus() == PLAYING)
            pause();
        else
            resume();
    }
    
    /** Stops playback. */
    @IsAction(name = "Stop", description = "Stops playback.", shortcut = "ALT+F", global = true)
    public static void stop() {
        if (playback == null) return;
        
        playback.stop();
        realTime.synchroRealTime_onStopped();
    }
    
    /** Seeks playback to position specified by duration parameter. */
    public static void seek(Duration duration) {
        if (playback == null) return;
        
        if (playback.getStatus() == STOPPED) {
            pause();
        }
        
        if (playback.getStatus() == PLAYING || playback.getStatus() == PAUSED) {
            realTime.synchroRealTime_onPreSeeked();
            playback.seek(duration);
            realTime.synchroRealTime_onPostSeeked(duration);
            core.needs_seek = false;
        } else {
            core.needs_seek = true;
            core.seekTo = duration;
        }
    }

    /** Seeks playback to position specified by percent value 0-1. */
    public static void seek(double at) {
        if(at<0 ||at>1) throw new IllegalArgumentException("Seek value must be 0-1");
        seek(getTotalTime().multiply(at));
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
        return state.getVolume();
    }
    
    public static double getVolumeMin() {
        return Volume.min();
    }
    
    public static double getVolumeMax() {
        return Volume.max();
    }
    
    public static void setVolume(double value) {
        state.setVolume(value);
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
        return state.volumeProperty();
    }
    
    public static double getBalance() {
        return state.getBalance();
    }
    
    public static double getBalanceMin() {
        return Balance.min();
    }
    
    public static double getBalanceMax() {
        return Balance.max();
    }
    
    public static void setBalance(double value) {
        state.setBalance(value);
    }
    
    public static DoubleProperty balanceProperty() {
        return state.balanceProperty();
    }
    
    public static PlayingItemSelector.LoopMode getLoopMode() {
        return state.getLoopMode();
    }
    
    @IsAction(name = "Toggle looping", description = "Switch between playlist looping mode.", shortcut = "ALT+L")
    public static void toggleLoopMode() {
        setLoopMode(getLoopMode().next());
    }
    
    public static void setLoopMode(LoopMode mode) {
        state.setLoopMode(mode);
        PlaylistManager.playingItemSelector.setSelector(mode.selector());
    }
    
    public static ObjectProperty<LoopMode> loopModeProperty() {
        return state.loopModeProperty();
    }
   
    public static MediaPlayer.Status getStatus() {
        return state.getStatus();
    }   
    
    public static ObjectProperty<Status> statusProperty() {
        return state.statusProperty();
    }    
    
    public static Duration getCurrentTime() {
        return state.getCurrentTime();
    }
    
    public static ObjectProperty<Duration> currentTimeProperty() {
        return state.currentTimeProperty();
    }
    
    public static Duration getRealTime() {
        return realTime.get();
    }
    
    public static RealTimeProperty realTimeProperty() {
        return realTime;
    }
    
    public static Duration getRemainingTime() {
        return getTotalTime().subtract(getCurrentTime());
    }
    
    public static Duration getTotalTime() {
        return state.getDuration();
    }
    
    public static ObjectProperty<Duration> totalTimeProperty() {
        return state.durationProperty();
    }
    
    public static double getPosition() {
        return getCurrentTime().toMillis()/getTotalTime().toMillis();
    }
    
    public static double getPlayedPercentage() {
        return getRealTime().toMillis()/getTotalTime().toMillis();
    }
    
    public static double getRate() {
        return state.getRate();
    }

    public static void setRate(double value) {
        state.setRate(value);
    }
    
    public static DoubleProperty rateProperty() {
        return state.rateProperty();
    }   
    
    public static boolean isMute() {
        return state.getMute();
    }
    
    public static boolean getMute() {
        return state.getMute();
    }

    public static void setMute(boolean value) {
        state.setMute(value);
    }
    
    /** Switches between on/off state for mute property. */
    @IsAction(name = "Toggle mute", description = "Switch mute on/off.", shortcut = "ALT+M")
    public static void toggleMute() {
        if (isMute()) setMute(false);
        else setMute(true);
    }
    
    public static BooleanProperty muteProperty() {
        return state.muteProperty();
    }
    
    /**
     * Rates playing item specified by percentage rating.
     * @param rating <0,1> representing percentage of the rating, 0 being minimum
     * and 1 maximum possible rating for current item.
     */
    public static void rate(double rating) {
        if (PlaylistManager.isItemPlaying())
            MetadataWriter.rate(Player.playingtem.get(), rating);
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
    
/******************************************************************************/

    /**
     * Add behavior to behave every time song starts playing. Seeking to 
     * time 0 dosnt activate this event.
     * It is not safe to assume that application's information on currently played
     * item will be updated before this event. Therefore using cached information
     * like can result in misbehavior due to outdated information.
     * @param b 
     */
    public static void addOnPlaybackStart(Procedure b)
        { core.addOnPlaybackStart(b); }
    /**
     * Remove behavior that executes when item starts playing.
     * @param b 
     */
    public static void removeOnPlaybackStart(Procedure b)
        { core.removeOnPlaybackStart(b); }
    /**
     * Add behavior that will execute when item playback ends.
     * It is safe to use in-app cache of currently played item inside
     * the behavior parameter. Application's information will still apply during
     * playbackEnd event;
     * @param b 
     */
    public static void addOnPlaybackEnd(Procedure b) 
        { core.addOnPlaybackEnd(b); }
    /**
     * Remove behavior that executes when item playback ends.
     * @param b 
     */
    public static void removeOnPlaybackEnd(Procedure b)
        { core.removeOnPlaybackEnd(b); }
    /**
     * Set audio spectrum listener to listen to spectrum changes.
     * Spectrum listener allows real-time observation of frequency bands of
     * played item.
     * @param l The listener.
     */
    public static void addAudioSpectrumListener(AudioSpectrumListener l) 
        { core.addAudioSpectrumListener(l); }
    /**
     * Removes audio spectrum listener. 
     * @param l The listener.
     */
    public static void removeAudioSpectrumListener(AudioSpectrumListener l)
        { core.removeAudioSpectrumListener(l); }
    
/******************************************************************************/
    
    /**
     * Creates playback of specified file initialized to current
     * playback state.
     * The resource is URI.toString() - string representation of the URI
     * @param resource that will be assigned to player and played.
     */
    private static void createPlayback(String resource){
        Media media;
        try {
            media = new Media(resource);
        } catch (MediaException e) {
            Log.err(e.getLocalizedMessage());
            stop();
            return;
        }
        
        playback = new MediaPlayer(media);
        
        // debug
        // these two have no effect, unless something is wrong
        // if seeking out of sync with current value (WHICH IT IS!!)
        // setStopTime might stop the song prematurely
        // playback.setStartTime(ZERO);
        // playback.setStopTime(playback.getTotalDuration());
        
        
        playback.setAudioSpectrumInterval(0.1);
        playback.setAudioSpectrumNumBands(64);
//        playback.setAudioSpectrumThreshold(i) // ? what val is ok?

        // bind (not read only) values: global -> new playback (automatic initialization)
        playback.volumeProperty().bind(state.volumeProperty());
        playback.balanceProperty().bind(state.balanceProperty());
        playback.muteProperty().bind(state.muteProperty());
        playback.rateProperty().bind(state.rateProperty());
        realTime.real_seek = state.getRealTime();
        realTime.curr_sek = Duration.ZERO;
        // register listener/event distributors
        playback.setAudioSpectrumListener(core.spectrumListenerDistributor);
        playback.setOnEndOfMedia(core.playbackEndDistributor);
        
        // handle binding of state to player
        // handle seeking when player in invalid statuses (seek when status becomes valid)
        playback.statusProperty().addListener(new ChangeListener<Status>() {
            @Override
            public void changed(ObservableValue<? extends Status> o, Status oldV, Status newV) {
                if (newV == PLAYING || newV == PAUSED || newV == STOPPED) {
                    // bind (read only) values: new playback -> global (manual initialization)
                    state.currentTimeProperty().bind(playback.currentTimeProperty());
                    state.durationProperty().bind(playback.cycleDurationProperty());
                    state.statusProperty().bind(playback.statusProperty());
                    // make one time only
                    playback.statusProperty().removeListener(this);
                }
            }
        });
        playback.statusProperty().addListener(new ChangeListener<Status>() {
            @Override
            public void changed(ObservableValue<? extends Status> o, Status oldV, Status newV) {
                if (newV == Status.PLAYING || newV == PAUSED ) {
                    if (core.needs_seek) {
                        seek(core.seekTo);
                    }
                    // make one time only
                    playback.statusProperty().removeListener(this);
                }
            }
        });
            
            
//this can be substitued for the Listeners above // i just wanted to separate code           
//        playback.setOnPlaying(new Runnable() {
//            @Override
//            public void run() {
//                if (core.needs_bind) {
//                    // bind (read only) values: new playback -> global (manual initialization)
//                    activeState.currentTimeProperty().bind(playback.currentTimeProperty());
//                    activeState.totalTimeProperty().bind(playback.cycleDurationProperty());
//                    activeState.statusProperty().bind(playback.statusProperty());
//                }
//                if (core.needs_seek) {
//                    seek(core.seekTo);
//                }
//            }
//        });
//        playback.setOnPaused(new Runnable() {
//            @Override
//            public void run() {
//                if (core.needs_bind) {
//                    // bind (read only) values: new playback -> global (manual initialization)
//                    activeState.currentTimeProperty().bind(playback.currentTimeProperty());
//                    activeState.totalTimeProperty().bind(playback.cycleDurationProperty());
//                    activeState.statusProperty().bind(playback.statusProperty());
//                }
//                if (core.needs_seek) {
//                    seek(core.seekTo);
//                }
//            }
//        });
//        playback.setOnStopped(new Runnable() {
//            @Override
//            public void run() {
//                if (core.needs_bind) {
//                    // bind (read only) values: new playback -> global (manual initialization)
//                    activeState.currentTimeProperty().bind(playback.currentTimeProperty());
//                    activeState.totalTimeProperty().bind(playback.cycleDurationProperty());
//                    activeState.statusProperty().bind(playback.statusProperty());
//                }
//            }
//        });
        
        // initialize (read only) values
            //status && duration(auto)(item_length) && current time (auto)(0)
        if (state.getStatus() == MediaPlayer.Status.PLAYING) {
            playback.play();
        } else
        if (state.getStatus() == MediaPlayer.Status.PAUSED) {
            playback.pause();
        } else {
            playback.stop();
        }
    }
    
    /**
     * Closes playback.
     * Prevents 'double playback'.
     */
    private static void destroyPlayback() {
        if (playback == null) return;
        playback.volumeProperty().unbind();
        playback.balanceProperty().unbind();
        playback.muteProperty().unbind();
        playback.rateProperty().unbind();
        state.currentTimeProperty().unbind();
        state.durationProperty().unbind();
        state.statusProperty().unbind();
        playback.stop();
        playback.dispose();
//        playback = null;
        core.needs_bind = true;
    }
}
