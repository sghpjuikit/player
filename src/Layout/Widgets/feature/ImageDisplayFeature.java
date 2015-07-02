/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Widgets.feature;

import java.io.File;

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
     * @param img_file 
     */
    void showImage(File img_file);
}
