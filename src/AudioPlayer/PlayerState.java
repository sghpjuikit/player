/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer;

import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playback.PlaybackState;
import AudioPlayer.playlist.PlaylistState;
import Serialization.PlaybackStateConverter;
import Serialization.PlaylistItemConverter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import main.App;
import util.dev.Log;

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
    @XStreamOmitField
    public final PlaylistState playlist;
    public final List<PlaybackState> playbacks = new ArrayList<>();
    public final List<PlaylistState> playlists = new ArrayList<>();
    // for serialization only, active state flags
    private UUID playlist_id;
    private UUID playback_id;

    /**
     * Creates default state.
     */
    public PlayerState() {
        playback = PlaybackState.getDefault();
        playlist = PlaylistState.getDefault();
        playbacks.add(playback);
        playlists.add(playlist);
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
            playlists.forEach(PlaylistState::suspend);
            playback.realTime.set(PLAYBACK.getRealTime());
            suspendPlayback();
            playback_id = playback.getId();
            playlist_id = UUID.fromString(playlist.getId().toString()); // without string conversion serialization errored out... weird
            
            XStream x = new XStream(new DomDriver());
            x.autodetectAnnotations(true);
            x.registerConverter(new PlaybackStateConverter());
            x.registerConverter(new PlaylistItemConverter());
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
            playlists.clear();
            playlists.addAll(ps.playlists);
            playback.change(getPb(ps.playback_id));
            playlist.change(getPl(ps.playlist_id));
            playlist.activate();
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
    private PlaylistState getPl(UUID id) {
        for (PlaylistState s: playlists) {
            if (s.getId().equals(id))
                return s;
        }
        return null;
    }  
}