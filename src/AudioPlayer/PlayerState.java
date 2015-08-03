/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javafx.collections.ObservableListBase;

import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playback.PlaybackState;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistManager;
import Serialization.PlaybackStateConverter;
import Serialization.PlaylistItemConverter;
import com.sun.javafx.collections.ObservableListWrapper;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;
import main.App;
import unused.Log;

/**
 * @author uranium
 * 
 * Low level representation of state of player used for
 * serialization of player's state to maintain state across sessions..
 * Immutable.
 */
public final class PlayerState {
    @XStreamOmitField
    public final PlaybackState playback;
    public final List<PlaybackState> playbacks = new ArrayList<>();
    public final List<Playlist> playlists = new ArrayList<>();
    private UUID playlist_id;
    private UUID playback_id;

    /**
     * Creates default state.
     */
    public PlayerState() {
        playback = PlaybackState.getDefault();
        playbacks.add(playback);
    }
    
    public void suspendPlayback() {
        PlaybackState p = getPb(playback.getId());
        if (p != null)
            p.change(playback);
        else {
            PlaybackState s = PlaybackState.getDefault();
                          s.change(playback);
            playbacks.add(s);
        }
    }
    
    public void serialize() {
        try {
            playback.realTime.set(PLAYBACK.getRealTime());
            suspendPlayback();
            playback_id = playback.getId();
            playlist_id = UUID.fromString(PlaylistManager.active.toString());
            
            playlists.clear();
            playlists.addAll(PlaylistManager.playlists);
            
            XStream x = new XStream(new DomDriver());
            x.autodetectAnnotations(true);
            x.registerConverter(new PlaybackStateConverter());
            x.registerConverter(new PlaylistItemConverter());
            x.omitField(ObservableListBase.class, "listenerHelper");
            x.omitField(ObservableListBase.class, "changeBuilder");
            x.omitField(ObservableListWrapper.class, "elementObserver");
            x.toXML(this, new BufferedWriter(new FileWriter(App.PLAYER_STATE_FILE())));
        } catch (IOException ex) {
            Log.err("Unable to save player state into the file: "+ App.PLAYER_STATE_FILE());
        }
    }
    
    public void deserialize() {
        try {
            XStream x = new XStream(new DomDriver());
            x.autodetectAnnotations(true);
            x.registerConverter(new PlaybackStateConverter());
            x.registerConverter(new PlaylistItemConverter());
            PlayerState ps = (PlayerState) x.fromXML(new File(App.PLAYER_STATE_FILE()));
            
            playbacks.clear();
            playbacks.addAll(ps.playbacks);
            playback.change(getPb(ps.playback_id));
            
            playlists.clear();
            playlists.addAll(ps.playlists);
            PlaylistManager.playlists.addAll(playlists);
            PlaylistManager.active = ps.playlist_id;
            
        } catch (ClassCastException | StreamException ex) {
            Log.err("Unable to load player state from the file: "+ App.PLAYER_STATE_FILE() + 
                    ". The file not found or content corrupted. Loading default state. ");
        }
    }
    
    
    private PlaybackState getPb(UUID id) {
        for (PlaybackState s: playbacks) {
            if (s.getId().equals(id))
                return s;
        }
        return null;
    }
}