/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.Features;

import AudioPlayer.tagging.Metadata;

/**
 * Graphical representation of {@link Metadata}
 * 
 * @author Plutonium_
 */
@Feature(
  name = "Song info",
  description = "Displays the metadata information about the song",
  type = SongInfo.class
)
public interface SongInfo {
    /** Displays the metadata information about the song. */
    void setValue(Metadata m);
}
