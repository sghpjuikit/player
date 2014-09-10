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
public interface ImageDisplayFeature extends Feature {
    
    /** {@inheritDoc} */
    @Override
    public default String getFeatureName() {
        return "Displays image";
    }
    
    /**
     * Display the image file.
     * @param img_file 
     */
    void showImage(File img_file);
}
