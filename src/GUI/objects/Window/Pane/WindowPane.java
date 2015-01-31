/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Window.Pane;

import GUI.WindowBase.Maximized;
import static GUI.WindowBase.Maximized.ALL;
import GUI.objects.Window.Resize;
import javafx.beans.property.*;
import javafx.scene.Node;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.*;
import javafx.scene.layout.AnchorPane;

/**
 <p>
 @author Plutonium_
 */
public class WindowPane {
    
    public final AnchorPane owner;
    public final AnchorPane root = new AnchorPane();
    
    private double _x = 100;
    private double _y = 100;
    private double _w = 0;
    private double _h = 0;
    protected final ReadOnlyObjectWrapper<Resize> isResizing = new ReadOnlyObjectWrapper(Resize.NONE);
    protected final ReadOnlyBooleanWrapper _isMoving = new ReadOnlyBooleanWrapper(false);
    protected final ReadOnlyBooleanWrapper _fullscreen = new ReadOnlyBooleanWrapper(false);
    
    public final DoubleProperty x = new SimpleDoubleProperty(50) {
        @Override public void set(double nv) {
            double v = offScreenXMap(nv);
            super.set(v);
            root.setLayoutX(v);
        }
    };
    public final DoubleProperty y = new SimpleDoubleProperty(50) {
        @Override public void set(double nv) {
            double v = offScreenYMap(nv);
            super.set(v);
            root.setLayoutY(v);
        }
    };
    public final DoubleProperty w = root.prefWidthProperty();
    public final DoubleProperty h = root.prefHeightProperty();
    public final BooleanProperty visible = root.visibleProperty();
    public final DoubleProperty opacity = root.opacityProperty();
    
    /** Indicates whether this window is maximized. */
    public final ObjectProperty<Maximized> maximized = new SimpleObjectProperty(Maximized.NONE);
    /** Defines whether this window is resizable. */
    public final BooleanProperty movable = new SimpleBooleanProperty(true);
    /** Indicates whether the window is being moved. */
    public final ReadOnlyBooleanProperty moving = _isMoving.getReadOnlyProperty();
    /** Indicates whether and how the window is being resized. */
    public final ReadOnlyObjectProperty<Resize> resizing = isResizing.getReadOnlyProperty();
    /** Defines whether this window is resizable. */
    public final BooleanProperty resizable = new SimpleBooleanProperty(true);
    
    public final DoubleProperty offScreenFixOFFSET = new SimpleDoubleProperty(0);
    
    
    public WindowPane(AnchorPane own) {
        owner = own;
    }
    
    
    /** Opens this window. Must not be invoked when already open. */
    public void open() {
        owner.getChildren().add(root);
    }
    
    /** @return whether this window is open. */
    public boolean isOpen() {
        return owner.getChildren().contains(root);
    }
    
    /** Closes this window. */
    public void close() {
       owner.getChildren().remove(root);
    }
    
    
    /** Position this window to center of its owner. */
    public void alignCenter() {
        x.set((owner.getWidth() - w.get()) / 2);
        y.set((owner.getHeight() - h.get()) / 2);
    }
    /** Resizes to half of its owner. */
    public void resizeHalf() {
        w.set(owner.getWidth() / 2);
        h.set(owner.getHeight() / 2);
    }
    
    
    double startX, startY;
    /**
     * Installs move by dragging on provided Node, usually root.
     * pane inside display. See positionable class for details.
     * Default moving behavior supports move by mouse drag by bgr and ctrl+right
     * click mouse drag anywhere
     */
    public void installMovingBehavior(Node n) {
        // remember coords on pressed
        n.addEventFilter(DRAG_DETECTED, e -> {
            if (maximized.get()==ALL || !movable.get() || e.getButton()!=PRIMARY)
                return;
            _isMoving.set(true);
            startX = x.get() - e.getSceneX();
            startY = y.get() - e.getSceneY();
        });
        // move on drag - right button + ctrl - whole window
        n.addEventFilter(MOUSE_DRAGGED, e -> {
            if (moving.get()) {
                x.set(startX + e.getSceneX());
                y.set(startY + e.getSceneY());
            }
        });
        // move on drag - left button - ecluding content area
        n.addEventHandler(MOUSE_RELEASED, e -> {
            if(_isMoving.get())
                _isMoving.set(false);
        });
    }
    
    
    private double offScreenXMap(double d) {
        if (d < 0)
            return offScreenFixOFFSET.get();
        if (d + w.get() > owner.getWidth())
            return owner.getWidth() - w.get() - offScreenFixOFFSET.get();
        return d;
    }
    private double offScreenYMap(double d) {
        if (d < 0)
            return offScreenFixOFFSET.get();
        if (d + h.get() > owner.getHeight())
            return owner.getHeight() - h.get() - offScreenFixOFFSET.get();
        return d;
    }
    
}



