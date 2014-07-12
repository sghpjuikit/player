/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback;

import AudioPlayer.playlist.ItemSelection.PlayingItemSelector;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

/**
 * Captures state of playback.
 */
public final class PlaybackState {
    private UUID id;
    private final VolumeProperty volume;
    private final BalanceProperty balance;
    private final ObjectProperty<PlayingItemSelector.LoopMode> loopMode;
    private final ObjectProperty<MediaPlayer.Status> status;
    private final ObjectProperty<Duration> duration;
    private final ObjectProperty<Duration> currentTime;
    private final ObjectProperty<Duration> realTime;
    private final BooleanProperty mute;
    private final DoubleProperty rate;
    
    private int played_item_index;
    private URI played_item;
    private UUID played_playlist;

    private PlaybackState(UUID _id) {
        id = _id;
        volume = new VolumeProperty();
        balance = new BalanceProperty();
        loopMode = new SimpleObjectProperty<>(PlayingItemSelector.LoopMode.PLAYLIST);
        status = new SimpleObjectProperty<>(MediaPlayer.Status.UNKNOWN);
        duration = new SimpleObjectProperty<>(Duration.ZERO);
        currentTime = new SimpleObjectProperty<>(Duration.ZERO);
        realTime = new SimpleObjectProperty<>(Duration.ZERO);
        mute = new SimpleBooleanProperty(false);
        rate = new SimpleDoubleProperty(1);
    }
    private PlaybackState() {
        this(UUID.randomUUID());
    }

    public double getVolume() {
        return volume.get();
    }

    public void setVolume(Volume val) {
        volume.set(val);
    }

    public void setVolume(double val) {
        volume.set(val);
    }

    public DoubleProperty volumeProperty() {
        return volume.volumeProperty();
    }
    
    public double getBalance() {
        return balance.get();
    }

    public void setBalance(double val) {
        balance.set(val);
    }
    
    public void setBalance(Balance val) {
        balance.set(val);
    }
    
    public DoubleProperty balanceProperty() {
        return balance.balanceProperty();
    }
    
    public PlayingItemSelector.LoopMode getLoopMode() {
        return loopMode.get();
    }

    public void setLoopMode(PlayingItemSelector.LoopMode val) {
        loopMode.set(val);
    }

    public ObjectProperty<PlayingItemSelector.LoopMode> loopModeProperty() {
        return loopMode;
    }

    public MediaPlayer.Status getStatus() {
        return status.get();
    }

    public void setStatus(MediaPlayer.Status val) {
        status.set(val);
    }

    public ObjectProperty<MediaPlayer.Status> statusProperty() {
        return status;
    }

    public Duration getCurrentTime() {
        return currentTime.get();
    }

    public void setCurrentTime(Duration val) {
        currentTime.set(val);
    }

    public ObjectProperty<Duration> currentTimeProperty() {
        return currentTime;
    }
    
    public Duration getRealTime() {
        return realTime.get();
    }

    public void setRealTime(Duration val) {
        realTime.set(val);
    }

    public ObjectProperty<Duration> realTimeProperty() {
        return realTime;
    }

    public Duration getDuration() {
        return duration.get();
    }

    public void setDuration(Duration val) {
        duration.set(val);
    }

    public ObjectProperty<Duration> durationProperty() {
        return duration;
    }

    public boolean getMute() {
        return mute.get();
    }

    public void setMute(boolean val) {
        mute.set(val);
    }

    public BooleanProperty muteProperty() {
        return mute;
    }

    public double getRate() {
        return rate.get();
    }

    public void setRate(double val) {
        rate.set(val);
    }

    public DoubleProperty rateProperty() {
        return rate;
    }

    public UUID getId() {
        return id;
    }
    /**
     * Changes this state's property values to that of another state. Use this
     * to switch between multiple states.
     * @param to
     */
    public void change(PlaybackState to) {
        if (to == null) return;
        id = to.getId();
        volume.set(to.getVolume());
        balance.set(to.getBalance());
        loopMode.set(to.getLoopMode());
        status.unbind();
        status.set(to.getStatus());
        duration.unbind();
        duration.set(to.getDuration());
        currentTime.unbind();
        currentTime.set(to.getCurrentTime());
        realTime.set(to.getRealTime());
        mute.set(to.getMute());
        rate.set(to.getRate());
    }
    
    @Override
    public boolean equals(Object o) {
        if(this==o) return true; // this line can make a difference
        
        if( o instanceof PlaybackState)
            return id.equals(((PlaybackState)o).id);
        else if( o instanceof UUID)
            return id.equals(o);
        else
            return false;        
    }
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + Objects.hashCode(this.id);
        return hash;
    }
    
    @Override
    public String toString() {
        String out = "";
        out = out + "Id: " + this.id.toString() + "\n";
        out = out + "Total Time: " + this.getDuration().toString() + "\n";
        out = out + "Current Time: " + this.getCurrentTime().toString() + "\n";
        out = out + "Real Time: " + this.getRealTime().toString() + "\n";
        out = out + "Volume: " + this.getVolume() + "\n";
        out = out + "Balance: " + this.getBalance() + "\n";
        out = out + "Rate: " + this.getRate() + "\n";
        out = out + "Mute: " + this.getMute() + "\n";
        out = out + "Playback Status: " + this.getStatus().toString() + "\n";
        out = out + "Loop Mode: " + this.getLoopMode().toString() + "\n";
        return out;
    }
    
    /**
     * @return new default state
     */
    public static PlaybackState getDefault() {
        return new PlaybackState();
    }
    public static PlaybackState getDefault(UUID _id) {
        return new PlaybackState(_id);
    }
}