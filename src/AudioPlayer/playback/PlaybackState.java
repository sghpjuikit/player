/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback;

import AudioPlayer.playback.state.BalanceProperty;
import AudioPlayer.playback.state.VolumeProperty;
import AudioPlayer.playlist.sequence.PlayingSequence;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

import javafx.beans.property.*;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

/**
 * Captures state of playback.
 */
public final class PlaybackState {
    private UUID id;
    public final VolumeProperty volume;
    public final BalanceProperty balance;
    public final ObjectProperty<PlayingSequence.LoopMode> loopMode;
    public final ObjectProperty<MediaPlayer.Status> status;
    public final ObjectProperty<Duration> duration;
    public final ObjectProperty<Duration> currentTime;
    public final ObjectProperty<Duration> realTime;
    public final BooleanProperty mute;
    public final DoubleProperty rate;
    
    private int played_item_index;
    private URI played_item;
    private UUID played_playlist;

    private PlaybackState(UUID _id) {
        id = _id;
        volume = new VolumeProperty();
        balance = new BalanceProperty();
        loopMode = new SimpleObjectProperty<>(PlayingSequence.LoopMode.PLAYLIST);
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
        volume.set(to.volume.get());
        balance.set(to.balance.get());
        loopMode.set(to.loopMode.get());
        status.unbind();
        status.set(to.status.get());
        duration.unbind();
        duration.set(to.duration.get());
        currentTime.unbind();
        currentTime.set(to.currentTime.get());
        realTime.set(to.realTime.get());
        mute.set(to.mute.get());
        rate.set(to.rate.get());
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
                out = out + "Total Time: " + duration.get().toString() + "\n";
                out = out + "Current Time: " + currentTime.get().toString() + "\n";
                out = out + "Real Time: " + realTime.get().toString() + "\n";
                out = out + "Volume: " + volume + "\n";
                out = out + "Balance: " + balance + "\n";
                out = out + "Rate: " + rate + "\n";
                out = out + "Mute: " + mute + "\n";
                out = out + "Playback Status: " + status.get().toString() + "\n";
                out = out + "Loop Mode: " + loopMode.get().toString() + "\n";
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
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    public class TimeProperty extends SimpleDoubleProperty {
        
    }
}