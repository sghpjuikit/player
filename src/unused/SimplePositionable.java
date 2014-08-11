/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unused;

import javafx.scene.layout.AnchorPane;

/**
 * Simple implementation of Positionable.
 * Allows any AnchorPane object to utilize behavior of Positionable. 
 * @author uranium
 */
public class SimplePositionable extends Positionable {
    AnchorPane pane;
    AnchorPane display;
    
    public SimplePositionable(AnchorPane _pane, AnchorPane _display) {
        pane = _pane;
        display = _display;
    }
    
    @Override
    public AnchorPane getPane() {
        return pane;
    }
    
    @Override
    public AnchorPane getDisplay() {
        return display;
    }
}
