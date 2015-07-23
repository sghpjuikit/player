/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.graphics;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 *
 * @author Plutonium_
 */
public class Util {
    
    public static HBox layHorizontally(double gap, Pos align, Node... nodes) {
        HBox l = new HBox(gap, nodes);
             l.setAlignment(align);
        return l;
    }
    
    public static VBox layVertically(double gap, Pos align, Node... nodes) {
        VBox l = new VBox(gap, nodes);
             l.setAlignment(align);
        return l;
    }
}
