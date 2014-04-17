/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects;

import javafx.scene.Node;
import javafx.util.Duration;


/**
 *
 * @author uranium
 */
public abstract class ContextElement {
    static Duration animDur = Duration.millis(80);
    static boolean allowAnimation = true;
//    boolean adjustable_size = true;
    
    abstract public Node getElement();
    abstract public double getHeight();
    abstract public double getWidth();
    
    /**
     * Relocates element at specified coordinates.*/
    abstract public void relocate(double x, double y, double angle);
    
    /**
     * Relocates element so specified coordinates point to exact centre of this
     * element.
     */
    public void relocateCenter(double x, double y, double angle) {
        relocate(x - getWidth()/2, y - getHeight()/2, angle);
    }
    
    public void allowAnimation(boolean val) {
        allowAnimation = val;
    }
    public void setAnimationDuration(double val) {
        animDur = Duration.millis(val);
    }
//    /**
//     * Set true to allow element resize as needed based on content.
//     * Default true.
//     * @param val 
//     */
//    public void setAdjustableSize(boolean val) {
//        adjustable_size = val;
//    }
}
