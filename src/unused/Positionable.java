/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unused;

import javafx.scene.layout.AnchorPane;

/**
 * Defines object that is displayed and positioned on a pane - its display.
 *
 * @author uranium
 */
abstract public class Positionable {

    /**
     * Gap between border and repositioned object for offscreen fix. Object
     * can not come into this area. The exact implementation is up for offscrenFix()
     * method. This value simply creates standard, so this value can be ignored,
     * if desired so.
     */
    public static final double offScreenFixOFFSET = 10;
    
    /**
     * @return X coordinate.
     */
    public double getLayoutX() {
        return getPane().getLayoutX();
    }

    /**
     * @return Y coordinate.
     */
    public double getLayoutY() {
        return getPane().getLayoutY();
    }

    /**
     * @return Height of this element.
     */
    public double getHeight() {
        return getPane().getHeight();
    }

    /**
     * @return Width of this element.
     */
    public double getWidth() {
        return getPane().getWidth();
    }

    /**
     * Set X coordinate of this element.
     *
     * @param val
     */
    public void setLayoutX(double val) {
        getPane().setLayoutX(val);
        offScreenFix();
    }

    /**
     * Set Y coordinate of this element.
     *
     * @param val
     */
    public void setLayoutY(double val) {
        getPane().setLayoutY(val);
        offScreenFix();
    }
    
    /**
     * Set X and Y coordinate of this element.
     * @param x
     * @param y
     */
    public void relocate(double x, double y) {
        getPane().setLayoutX(x);
        getPane().setLayoutY(y);
        offScreenFix();
    }
    
    /**
     * @return The screen. Pane on which this object is displayed. The parent.
     */
    abstract public AnchorPane getDisplay();

    /**
     * @return the root pane of this object (not a parent!).
     */
    abstract public AnchorPane getPane();

    /**
     * Position this to center of its display.
     */
    public void centerAlign() {
        getPane().autosize(); // very important! without this getPane().getHeight() returns 0 and break this method
        double x = getDisplay().getWidth() / 2 - getWidth() / 2;
        double y = getDisplay().getHeight() / 2 - getHeight() / 2;
        getPane().relocate(x, y);
    }

    /**
     * Fix position of this object on the display so it doesn't extend beyond its
     * borders. This method is typically called internally in repositioning
     * methods.
     */
    public void offScreenFix() {
        double x = getLayoutX();
        double y = getLayoutY();
        double h = getHeight();
        double w = getWidth();
        if (x < 0) {
            getPane().setLayoutX(offScreenFixOFFSET);
        }
        if (y < 0) {
            getPane().setLayoutY(offScreenFixOFFSET);
        }
        if (x + w > getDisplay().getWidth()) {
            x = getDisplay().getWidth() - w - offScreenFixOFFSET;
            getPane().setLayoutX(x);
        }
        if (y + h > getDisplay().getHeight()) {
            y = getDisplay().getHeight() - h - offScreenFixOFFSET;
            getPane().setLayoutY(y);
        }
    }

}
