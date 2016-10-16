package layout.widget.feature;

import audio.playlist.Playlist;

/**
 * Stores list of songs to play.
 * 
 * @author Martin Polakovic
 */
@Feature(
	name = "Playlist",
	description = "Stores list of songs to play",
	type = PlaylistFeature.class
)
public interface PlaylistFeature {

	/** @return playlist */
	Playlist getPlaylist();

}