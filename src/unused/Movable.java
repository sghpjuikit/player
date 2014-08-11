/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unused;

import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

/**
 *
 * @author uranium
 */
abstract public class Movable extends Positionable {
    /**
     * Is maximized. Can not be set externally - exclusively for internal use.
     */
    private boolean maximized = false;
    /**
     * Can be moved
     */
    private boolean movable = true;
    /**
     * Can be maximized/minimized
     */
    private boolean expandable = true;
    /**
     * Can be resized
     */
    private boolean resizable = true;
    private double diffX; // global side-output value, dont use
    private double diffY; // global side-output value, dont use
    
    /**
     * Can be moved
     */
    public boolean isMovable() {
        return movable;
    }
    /**
     * Can be moved
     */
    public void setMovable(boolean val) {
        movable = val;
    }
   /**
     * Can be maximized/minimized
     */
    public boolean isExpandable() {
        return expandable;
    }
   /**
     * Can be maximized/minimized
     */    
    public void setExpandable(boolean val) {
        expandable = val;
    }
   /**
     * Can be resized
     */
    public boolean isResizable() {
        return expandable;
    }
   /**
     * Can be resized
     */    
    public void setResizable(boolean val) {
        expandable = val;
    }
    
   /**
     * Is maximized
     */
    public boolean isMaximized() {
        return maximized;
    }
    public void toggleMaximize() {
        if (isMaximized())
            demaximize();
        else
            maximize();
    }
    public void maximize() {
        if (!expandable) return;
        maximized = true;
        relocate(0, 0);
        getPane().setPrefSize(getDisplay().getWidth(), getDisplay().getHeight());
    }
    public void demaximize() {
        if (!expandable) return;
        maximized = false;
        getPane().setPrefSize(-1,-1); // = set to computed size = set to content size
        centerAlign();
    }
    
    /**
     * Invoking this method will apply default moving behavior for self - root
     * pane inside display. See positionable class for details.
     * Default moving behavior supports move by mouse drag by bgr and ctrl+right
     * click mouse drag anywhere
     */
    void installMovingBehavior() {
        AnchorPane pane = getPane();
        // remember coords on pressed
        pane.addEventFilter(MouseEvent.MOUSE_PRESSED, (MouseEvent e) -> {
            diffX = e.getX();
            diffY = e.getY();
        });
        // move on drag - right button + ctrl - whole window
        pane.addEventFilter(MouseEvent.MOUSE_DRAGGED, (MouseEvent e) -> {
            if (isMaximized() || !isMovable() || (!e.isControlDown() && e.getButton()!=MouseButton.SECONDARY))
                return;
            Point2D p = new Point2D(e.getX(), e.getY());
                    p = pane.localToParent(p);
            pane.relocate(p.getX()-diffX, p.getY()-diffY);
            offScreenFix();
        });
        // move on drag - left button - ecluding content area
        pane.addEventHandler(MouseEvent.MOUSE_DRAGGED, (MouseEvent e) -> {
            if (isMaximized() || !isMovable() ||  e.getButton() == MouseButton.SECONDARY)
                return;
            Point2D p = new Point2D(e.getX(), e.getY());
                    p = pane.localToParent(p);
            pane.relocate(p.getX()-diffX, p.getY()-diffY);
            offScreenFix();
        });
    }

}
