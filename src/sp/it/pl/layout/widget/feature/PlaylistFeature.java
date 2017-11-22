package sp.it.pl.layout.widget.feature;

import sp.it.pl.audio.playlist.Playlist;

/**
 * Stores list of songs to play.
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