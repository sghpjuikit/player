/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.Features;

import java.io.File;

/**
 * File system viewer.
 * 
 * @author Plutonium_
 */
@Feature(
  name = "Song info",
  description = "Displays the metadata information about the song",
  type = FileExplorerFeature.class
)
public interface FileExplorerFeature {
    /** Explores file in the file system hierarchy. */
    void exploreFile(File f);
}