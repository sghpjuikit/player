/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package layout.widget.feature;

import java.io.File;
import java.util.List;

/**
 * Displays image.
 *
 * @author Plutonium_
 */
@Feature(
  name = "Image display",
  description = "Displays image",
  type = ImageDisplayFeature.class
)
public interface ImageDisplayFeature {
    
    /**
     * Displays the image.
     * 
     * @param img_file to display
     */
    void showImage(File img_file);
    
    /**
     * Attempts to displays the images. Depends on implementation. By default
     * 1st image is displayed if available.
     * 
     * @param img_files to display
     */
    default void showImages(List<File> images) {
        if(!images.isEmpty())
            showImage(images.get(0));
    }
}
