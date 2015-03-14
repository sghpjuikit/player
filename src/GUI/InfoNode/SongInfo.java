/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.InfoNode;

import AudioPlayer.tagging.Metadata;
import Layout.Widgets.Features.Feature;

/**
Graphical representation of {@link Metadata}
 <p>
 @author Plutonium_
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
