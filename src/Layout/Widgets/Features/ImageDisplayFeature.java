/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Widgets.Features;

import java.io.File;

/**
 *
 * @author Plutonium_
 */
@FeatureName("Image display")
public interface ImageDisplayFeature extends Feature {
    
    /**
     * Displays the image.
     * 
     * @param img_file 
     */
    void showImage(File img_file);
}
