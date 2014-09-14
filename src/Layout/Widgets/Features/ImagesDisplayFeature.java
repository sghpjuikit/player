/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.Features;

import java.io.File;
import java.util.List;

/**
 *
 * @author Plutonium_
 */
@FeatureName("Images display")
public interface ImagesDisplayFeature extends Feature {
    
    /**
     * Displays the images.
     * 
     * @param img_files 
     */
    void showImages(List<File> img_files);
}