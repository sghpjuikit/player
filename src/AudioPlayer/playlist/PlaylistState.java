/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playlist;

import AudioPlayer.Player;
import AudioPlayer.playback.PlaybackState;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

/**
 * @author uranium
 * 
 * Captures complete state of playlist excluding the unneeded attributes like
 * selected item.
 */
public final class PlaylistState {
    private UUID id;
    public final ObservableList<PlaylistItem> playlist;
    public final ObjectProperty<PlaylistItem> playingItem = new SimpleObjectProperty<>();
    // for serialization
    private int play_index = -1;
    
    
    private PlaylistState() {
        this(UUID.randomUUID());
    }
    private PlaylistState(UUID _id) {
        id = _id;
        playlist = FXCollections.observableArrayList();
    }
    
    public PlaylistState(List<PlaylistItem> _playlist, PlaylistItem _playingItem) {
        id = UUID.randomUUID();
        playlist = FXCollections.observableArrayList(_playlist);
        playingItem.set(_playingItem);
    }
    
    /**
     * Changes this state's property values to that of another state. Use this
     * to switch between multiple states.
     * @param to
     */
    public void change(PlaylistState to) {
        if (to == null) return;
        id = to.getId();
        playlist.setAll(to.playlist);
        if (to.playingItem.get() != null)
            playingItem.set(to.playingItem.get());
        play_index = to.play_index;
    }
    
    public UUID getId() {
        return id;
    }
    public boolean isActive() {
        return Player.state.playback.equals(this);
    }
    public boolean isSuspended() {
        return !isActive();
    }
    public void suspend() {
        //if (isActive()) {
            playlist.setAll(PlaylistManager.getItems());
            play_index = PlaylistManager.indexOfPlaying();
        //}
    }
    public void activate() {
        if (play_index > -1)
            playingItem.set(playlist.get(play_index));
    }
    
    @Override
    public boolean equals(Object o) {
        if( o instanceof PlaylistState)
            return id.equals(((PlaylistState)o).id);
        else if( o instanceof UUID)
            return id.equals(o);
        else
            return false;        
    }
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + Objects.hashCode(this.id);
        return hash;
    }
    
    /**
     * @return new default state
     */
    public static PlaylistState getDefault() {
        return new PlaylistState();
    }
    public static PlaylistState getDefault(UUID _id) {
        return new PlaylistState(_id);
    }
}
