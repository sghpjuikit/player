/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.WidgetImpl;

import Layout.Widgets.ClassWidget;
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import Layout.Widgets.controller.ClassController;
import gui.objects.Spectrum;
import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;
import static util.Util.setAnchors;

/**
 *
 * @author Plutonium_
 */
@IsWidget
@Layout.Widgets.Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Spectrumator",
    description = "PDisplays real time audio spectrum of playback",
    howto = "",
    notes = "",
    version = "0.6",
    year = "2014",
    group = Widget.Group.VISUALISATION
)
public class Spectrumator extends ClassController  {
    private Spectrum spectr = new Spectrum();
    
    public Spectrumator() {
        spectr.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        spectr.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        spectr.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        
        
        this.getChildren().add(spectr);
        setAnchors(spectr, 0);
        
        // we only need to start listening once, so do it here rather than in
        // refresh(). In fact that would have an effect of multiplying the
        // listener
        spectr.startListening();
    }
    
    @Override
    public void onClose() {
        spectr.stopListening();
    }
    
}
