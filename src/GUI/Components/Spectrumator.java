/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.Components;

import GUI.ItemHolders.Spectrum;
import Layout.Widgets.Controller;
import Layout.Widgets.Widget;
import javafx.scene.layout.AnchorPane;

/**
 *
 * @author Plutonium_
 */
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
        AnchorPane.setBottomAnchor(spectr,0.0);
        AnchorPane.setTopAnchor(spectr,0.0);
        AnchorPane.setRightAnchor(spectr,0.0);
        AnchorPane.setLeftAnchor(spectr,0.0);
    }
    
    
    
    private Widget widget;
    
    @Override public void refresh() {
    }

    @Override public void setWidget(Widget w) {
        widget = w;
    }

    @Override public Widget getWidget() {
        return widget;
    }
    
}
