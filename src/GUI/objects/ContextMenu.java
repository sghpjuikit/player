/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects;

import GUI.ContextManager;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.ScaleTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import utilities.functional.functor.Procedure;

/**
 *
 * @author uranium
 */
public abstract class ContextMenu extends Positionable {
    
    public enum ElementType {
        BUTTON,
        CIRCLE;
    }
    
    public Duration animation = Duration.millis(200);
    private boolean showAnimationIn = true;
    private boolean showAnimationOut = true;
    
    // variables
    public Object userData;
    
    // gui
    final AnchorPane menu = new AnchorPane();
    final List<ContextElement> elements = new ArrayList<>();
    private boolean canClose = false;
    
    // effects
    private final ScaleTransition in;
    private final ScaleTransition out;
    
    public ContextMenu() {
        menu.setVisible(false);
        menu.setPickOnBounds(false); // == mouse transparent without making children as well
        menu.setPrefSize(1, 1); // so it will contract and tightly envelop children
        getDisplay().getChildren().add(menu);
        
        // build animations
        in = new ScaleTransition(animation, menu);
        //in.setFromX(0); 
        in.setToX(1);
        //in.setFromY(0); 
        in.setToY(1);
        in.setOnFinished(e -> {
            showMenu();
        });
        out = new ScaleTransition(animation, menu);
        //out.setFromX(1); 
        out.setToX(0);
        //out.setFromY(1); 
        out.setToY(0);
        out.setOnFinished(e -> {
            closeMenu();
        });
        
        //black bgr for debugging purposes
//        Circle r = new Circle();
//        r.radiusProperty().bind(menu.heightProperty().divide(2));
//        r.centerXProperty().bind(menu.widthProperty().divide(2));
//        r.centerYProperty().bind(menu.heightProperty().divide(2));
//        menu.getChildren().add(r);
//        r.setOpacity(0.5);
//        r.setFill(Paint.valueOf("WHITE"));
//        r.setEffect(new BoxBlur(13, 13, 1));
//        r.setMouseTransparent(true);
    }
    
    public void add(String name, String tooltip, final Procedure behavior) {
        ContextElement e = null;
        switch(ContextManager.contextMenuItemType) {
            case BUTTON: e = new ContextButtonElement(this, name, tooltip, behavior); break;
            case CIRCLE: e = new ContextCircleElement(this, name, tooltip, behavior); break;
            default:
        }
        elements.add(e);
        menu.getChildren().add(e.getElement());
            if (e instanceof ContextCircleElement) {
                Label l = ((ContextCircleElement)e).getLabel();
                menu.getChildren().add(l);
            }
    }
    
    public void show(double X, double Y, Object data) {
        if (elements.isEmpty()) return;
        userData = data;
        
        open(X, Y);
        
        canClose = false;
        if(showAnimationIn) in.play();
        else showMenu();
        menu.setVisible(true);
    }
    abstract void open(double X, double Y);
    public void close() {
        if (showAnimationOut) out.play();
        else closeMenu();
    }
    private void closeMenu() {
        if (!canClose) return;
        menu.setVisible(false);
        ContextManager.onMenuClose();
    }
    private void showMenu() {
        canClose = true;
    }
    /**
     * Returns true if the menu started opening and havent ended closing. False
     * if it isnt visible at all.
     * @return 
     */
    public boolean isOpen() {
        return menu.isVisible();
    }
    
//    @Override
//    public double getLayoutX() {
//        return menu.getLayoutX();
//    }
//    @Override
//    public double getLayoutY() {
//        return menu.getLayoutY();
//    }
//    @Override
//    public void setLayoutX(double val) {
//        menu.setLayoutX(val);
//        offScreenFix();
//    }
//    @Override
//    public void setLayoutY(double val) {
//        menu.setLayoutY(val);
//        offScreenFix();
//    }
    public double getElementWidth() {
        return (elements.isEmpty()) ? 0 : elements.get(0).getWidth();
    }
    public double getElementHeight() {
        return (elements.isEmpty()) ? 0 : elements.get(0).getHeight();
    }
    @Override
    public final AnchorPane getDisplay() {
//        return Window.getActive().contextPane;
        return ContextManager.contextPane;
    }
    @Override
    public AnchorPane getPane() {
        return menu;
    }
    
    public void allowAnimations(boolean val) {
        showAnimationIn = val;
        showAnimationOut = val;
    }
}