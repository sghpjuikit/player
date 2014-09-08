/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.WidgetImpl;

import GUI.objects.Spectrum;
import Layout.Widgets.Controller;
import Layout.Widgets.Widget;
import javafx.scene.layout.AnchorPane;
import utilities.Util;

/**
 *
 * @author Plutonium_
 */
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
public class Spectrumator extends AnchorPane implements Controller<Widget>  {
    private Spectrum spectr = new Spectrum();
    
    public Spectrumator() {
        initialize();
    }
    
    private void initialize() {
        spectr.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        spectr.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        spectr.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        
        
        this.getChildren().add(spectr);
        Util.setAPAnchors(spectr, 0);
        
        // we only need to start listening once, so do it here rather than in
        // refresh(). In fact that would have an effect of multiplying the
        // listener
        spectr.startListening();
    }
    
    
    
    private Widget widget;
    
    @Override public void refresh() {        
    }

    @Override
    public void OnClosing() {
        spectr.stopListening();
    }

    @Override public void setWidget(Widget w) {
        widget = w;
    }

    @Override public Widget getWidget() {
        return widget;
    }
    
}
