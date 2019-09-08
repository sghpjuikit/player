package sp.it.pl.layout.widget.feature

import sp.it.pl.audio.playlist.Playlist

/** Is bound to and manages a single playlist. */
@Feature(name = "Playlist", description = "Is bound to and manages a single playlist", type = PlaylistFeature::class)
interface PlaylistFeature {

   /** @return managed playlist */
   val playlist: Playlist

}