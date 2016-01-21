/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer;

import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.io.StreamException;

import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playback.PlaybackState;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistManager;
import main.App;
import main.AppSerializator;

import static util.functional.Util.find;

/**
 * @author uranium
 *
 * Low level representation of state of player used for
 * serialization of player's state to maintain state across sessions..
 * Immutable.
 */
public final class PlayerState {

    private static final AppSerializator X = App.APP.serializators;
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerState.class);

    @XStreamOmitField
    public PlaybackState playback = PlaybackState.getDefault();
    public final List<PlaybackState> playbacks = new ArrayList<>();
    public final List<Playlist> playlists = new ArrayList<>();
    private UUID playlist_id;
    private UUID playback_id;

    public PlayerState() {
        playbacks.add(playback);
    }

    public static PlayerState deserialize() {
        File FILE = new File("PlayerState.cfg");
        try {
            return X.fromXML(PlayerState.class, FILE);
        } catch (StreamException ex) {
            LOGGER.error("Unable to load player state from the file {}. "
                    + "Loading default state.", FILE);
            return new PlayerState();
        }
    }

    public void serialize() {
        File FILE = new File("PlayerState.cfg");
        try {
            X.toXML(this, FILE);
        } catch (IOException ex) {
            LOGGER.error("Unable to save player state into the file {}", FILE);
        }
    }

    /** Invoked just before the serialization. */
    protected Object writeReplace() throws ObjectStreamException {
        playback.realTime.set(PLAYBACK.getRealTime());
        suspendPlayback();

        playback_id = playback.getId();
        playlist_id = PlaylistManager.active;

        playlists.clear();
        playlists.addAll(PlaylistManager.playlists);
        return this;
    }

    /**
     * Invoked just after deserialization.
     *
     * @implSpec
     * Resolve object by initializing non-deserializable fields or providing an
     * alternative instance (e.g. to adhere to singleton pattern).
     */
    protected Object readResolve() throws ObjectStreamException {
        playback = find(playbacks, pb -> pb.getId().equals(playback_id)).orElseGet(PlaybackState::getDefault);
        PlaylistManager.playlists.addAll(playlists);
        PlaylistManager.active = playlist_id;
        return this;
    }

    private void suspendPlayback() {
        PlaybackState p = find(playbacks, pb -> pb.getId().equals(playback_id)).orElse(null);
        if (p != null)
            p.change(playback);
        else {
            PlaybackState s = PlaybackState.getDefault();
                          s.change(playback);
            playbacks.add(s);
        }
    }
}