/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.window.pane;

import java.util.List;

import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

import gui.objects.window.Resize;
import gui.objects.window.stage.WindowBase.Maximized;

import static gui.objects.window.stage.WindowBase.Maximized.ALL;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.*;

/**
 * Window implemented as a {@link javafx.scene.layout.Pane} for in-application windows.
 *
 * @author Martin Polakovic
 */
public class WindowPane {
    
    public final AnchorPane owner;
    public final AnchorPane root = new AnchorPane();
    
    private double _x = 100;
    private double _y = 100;
    private double _w = 0;
    private double _h = 0;
    protected final ReadOnlyObjectWrapper<Resize> _resizing = new ReadOnlyObjectWrapper(Resize.NONE);
    protected final ReadOnlyBooleanWrapper _moving = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper _focused = new ReadOnlyBooleanWrapper(false);
    protected final ReadOnlyBooleanWrapper _fullscreen = new ReadOnlyBooleanWrapper(false);
    
    public final DoubleProperty x = new SimpleDoubleProperty(50) {
        @Override public void set(double nv) {
            int tmp = (int) ceil(nv);
            double v = tmp + tmp%2;
            if (snappable.get()) {
                v = mapSnap(v, v+w.get(), w.get(), owner.getWidth());
                v = mapSnapX(v, v+w.get(), w.get(), owner.getChildren());
            }
            if (offscreenFixOn.get())
                v = offScreenXMap(v);
            super.set(v);
            root.setLayoutX(v);
        }
    };
    public final DoubleProperty y = new SimpleDoubleProperty(50) {
        @Override public void set(double nv) {
            int tmp = (int) ceil(nv);
            double v = tmp + tmp%2;
            if (snappable.get()) {
                v = mapSnap(v, v+h.get(), h.get(), owner.getHeight());
                v = mapSnapY(v, v+h.get(), h.get(), owner.getChildren());
            }
            if (offscreenFixOn.get())
                v = offScreenYMap(v);
            super.set(v);
            root.setLayoutY(v);
        }
    };
//    public final DoubleProperty w = new SimpleDoubleProperty(root.getWidth()){
//        {
//            bindBidirectional(root.prefWidthProperty());
//        }
//        @Override public void set(double nv) {
//            int tmp = (int) ceil(nv);
//            double v = tmp + tmp%2;
//            super.set(v);
//        }
//    };
//    public final DoubleProperty h = new SimpleDoubleProperty(root.getHeight()){
//        {
//            bindBidirectional(root.prefHeightProperty());
//        }
//        @Override public void set(double nv) {
//            int tmp = (int) ceil(nv);
//            double v = tmp + tmp%2;
//            super.set(v);
//        }
//    };
    public final DoubleProperty w = root.prefWidthProperty();
    public final DoubleProperty h = root.prefHeightProperty();
    public final BooleanProperty visible = root.visibleProperty();
    public final DoubleProperty opacity = root.opacityProperty();
    /** Indicates whether this window is maximized. */
    public final ObjectProperty<Maximized> maximized = new SimpleObjectProperty(Maximized.NONE);
    /** Defines whether this window is resizable. */
    public final BooleanProperty movable = new SimpleBooleanProperty(true);
    /** Indicates whether the window is being moved. */
    public final ReadOnlyBooleanProperty moving = _moving.getReadOnlyProperty();
    /** Indicates whether and how the window is being resized. */
    public final ReadOnlyObjectProperty<Resize> resizing = _resizing.getReadOnlyProperty();
    /** Defines whether this window is resizable. */
    public final BooleanProperty resizable = new SimpleBooleanProperty(true);
    public final BooleanProperty snappable = new SimpleBooleanProperty(true);
    public final DoubleProperty snapDistance = new SimpleDoubleProperty(5);
    public final BooleanProperty offscreenFixOn = new SimpleBooleanProperty(true);
    public final DoubleProperty offScreenFixOffset = new SimpleDoubleProperty(0);
    public final ReadOnlyBooleanProperty focused = _focused.getReadOnlyProperty();
        
    
    public WindowPane(AnchorPane own) {
        owner = own;
        
        root.addEventFilter(MOUSE_PRESSED, e -> {
            root.toFront();
        });        
    }
    
    private final ListChangeListener<Node> focusListener = (Change<? extends Node> c) -> {
        int i = c.getList().size();
        _focused.set(i==0 ? false : c.getList().get(i-1)==root);
    };
    
    /** Opens this window. Must not be invoked when already open. */
    public void open() {
        owner.getChildren().add(root);
        owner.getChildren().addListener(focusListener);
    }
    
    /** @return whether this window is open. */
    public boolean isOpen() {
        return owner.getChildren().contains(root);
    }
    
    /** Closes this window. */
    public void close() {
        owner.getChildren().removeListener(focusListener);
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
    public void moveOnDragOf(Node n) {
        // remember coords on pressed
        n.addEventHandler(DRAG_DETECTED, e -> {
            e.consume();
            if (maximized.get()==ALL || !movable.get() || e.getButton()!=PRIMARY)
                return;
            _moving.set(true);
            startX = x.get() - e.getSceneX();
            startY = y.get() - e.getSceneY();
        });
        // move on drag - right button + ctrl - whole window
        n.addEventHandler(MOUSE_DRAGGED, e -> {
            if (moving.get()) {
                x.set(startX + e.getSceneX());
                y.set(startY + e.getSceneY());
            }
            e.consume();
        });
        // move on drag - left button - ecluding content area
        n.addEventHandler(MOUSE_RELEASED, e -> {
            if (_moving.get())
                _moving.set(false);
            e.consume();
        });
    }
    
    
    private double offScreenXMap(double d) {
        if (d < 0)
            return offScreenFixOffset.get();
        if (d + w.get() > owner.getWidth())
            return owner.getWidth() - w.get() - offScreenFixOffset.get();
        return d;
    }
    private double offScreenYMap(double d) {
        if (d < 0)
            return offScreenFixOffset.get();
        if (d + h.get() > owner.getHeight())
            return owner.getHeight() - h.get() - offScreenFixOffset.get();
        return d;
    }
    
    private double mapSnap(double x, double right, double w, double owner_width) {
        if (abs(x)<snapDistance.get())
            return 0;
        if (abs(right-owner_width)<snapDistance.get())
            return owner_width - w;
        return x;
    }
    private double mapSnapX(double x, double right, double wi, List<Node> windows) {
        for (Node n : windows) {
            if (n == this.root) continue;
            
            double wr = n.getLayoutX()+((Pane)n).getWidth(); 
            if (abs(x-wr)<snapDistance.get())
                return wr;
            
            if (abs(x+w.get()-n.getLayoutX())<snapDistance.get())
                return n.getLayoutX()-w.get();
        }
        return x;
    }
    private double mapSnapY(double y, double right, double w, List<Node> windows) {
        for (Node n : windows) {
            if (n == this.root) continue;
            
            double wr = n.getLayoutY()+((Pane)n).getHeight();
            if (abs(y-wr)<snapDistance.get())
                return wr;
            
            if (abs(y+h.get()-n.getLayoutY())<snapDistance.get())
                return n.getLayoutY()-h.get();
        }
        return y;
    }
}



