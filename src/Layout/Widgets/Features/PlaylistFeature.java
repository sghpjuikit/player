/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Widgets.Features;

/**
 * Defines playlist feature - functionality of a widget for playlist.
 * 
 * @author Plutonium_
 */
public interface PlaylistFeature extends Feature {
    
    /** {@inheritDoc} */
    @Override
    public default String getFeatureName() {
        return "Contains playlist";
    }
    
}
