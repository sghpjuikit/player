/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.feature;

import java.io.File;
import java.util.List;

/**
 * Displays images.
 *
 * @author Plutonium_
 */
@Feature(
  name = "Images display",
  description = "Displays images",
  type = ImagesDisplayFeature.class
)
public interface ImagesDisplayFeature {
    
    /**
     * Displays the images.
     * 
     * @param img_files 
     */
    void showImages(List<File> img_files);
}