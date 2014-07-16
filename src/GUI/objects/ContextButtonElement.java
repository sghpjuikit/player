/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects;

import GUI.ContextManager;
import utilities.functional.functor.Procedure;
import javafx.animation.ScaleTransition;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;

/**
 *
 * @author uranium
 */
public final class ContextButtonElement extends ContextElement{
    private static final double HEIGHT = 20;
    private static final double WIDTH = 80;
    
    private final Context_Menu parent;
    private final Button e = new Button();
    private final ScaleTransition scale = new ScaleTransition(animDur,e);
    
    public ContextButtonElement(final Context_Menu _parent, String name, String tooltip, final Procedure behavior) {
        parent = _parent;
        e.setText(name);
        e.setOnMousePressed((MouseEvent t) -> {
            behavior.run();
            if(ContextManager.closeMenuOnAction)
                parent.close();
        });
        e.setOnMouseEntered((MouseEvent t) -> {
            e.toFront(); // in case the elements overlap/nearby
            if (!allowAnimation) return;
            scale.stop();
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        });
        e.setOnMouseExited((MouseEvent t) -> {
            if (!allowAnimation) return;
            scale.stop();
            scale.setToX(1);
            scale.setToY(1);
            scale.play();
        });
        e.setPrefHeight(HEIGHT);
        e.setPrefWidth(WIDTH);
        e.setMaxHeight(HEIGHT);
        e.setMinHeight(HEIGHT);
        e.setMaxWidth(WIDTH);
        e.setMinWidth(WIDTH);
        e.setTooltip(new Tooltip(tooltip));
    }
    
    @Override
    public Button getElement() {
        return e;
    }
    @Override
    public double getHeight() {
        return HEIGHT;
    }

    @Override
    public double getWidth() {
        return WIDTH;
    }
    
    @Override
    public void relocate(double x, double y, double andgle) {
        e.relocate(x, y);
    }
    
//    @Override
//    public void setAdjustableSize(boolean val) {
//        adjustable_size = val;
//        if (val) e.setPrefSize(0, 0);
//        else e.setPrefSize(WIDTH, WIDTH);
//    }
}
