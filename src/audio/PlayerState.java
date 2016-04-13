/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package audio;

import java.io.File;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import audio.playback.PLAYBACK;
import audio.playback.PlaybackState;
import audio.playlist.Playlist;
import audio.playlist.PlaylistManager;
import main.AppSerializer;
import main.AppSerializer.SerializationException;

import static main.App.APP;
import static util.functional.Util.stream;

/**
 * @author Martin Polakovic
 *
 * Low level representation of state of player used for
 * serialization of player's state to maintain state across sessions..
 * Immutable.
 */
public final class PlayerState {

    private static final AppSerializer X = APP.serializators;
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
        File f = new File(APP.DIR_USERDATA, "playerstate.cfg");
        try {
            return X.fromXML(PlayerState.class, f);
        } catch (SerializationException ex) {
            LOGGER.error("Unable to load player state from the file {}. "
                    + "Loading default state.", f);
            return new PlayerState();
        }
    }

    public void serialize() {
        File f = new File(APP.DIR_USERDATA, "playerstate.cfg");
        try {
            X.toXML(this, f);
        } catch (SerializationException ex) {
            LOGGER.error("Unable to save player state into the file {}", f);
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
        playback = stream(playbacks).findAny(pb -> pb.getId().equals(playback_id)).orElseGet(PlaybackState::getDefault);
        PlaylistManager.playlists.addAll(playlists);
        PlaylistManager.active = playlist_id;
        return this;
    }

    private void suspendPlayback() {
        PlaybackState p = stream(playbacks).findAny(pb -> pb.getId().equals(playback_id)).orElse(null);
        if (p != null)
            p.change(playback);
        else {
            PlaybackState s = PlaybackState.getDefault();
                          s.change(playback);
            playbacks.add(s);
        }
    }
}