/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects;

import javafx.scene.control.ScrollBar;
import javafx.scene.control.skin.ScrollBarSkin;
import javafx.scene.layout.StackPane;

import util.animation.Anim;

import static javafx.geometry.Orientation.VERTICAL;
import static javafx.scene.input.MouseEvent.*;
import static javafx.util.Duration.millis;
import static util.type.Util.getFieldValue;

/**
 * ScrollBar skin that adds animations & improved usability - thumb expands on mouse hover.
 *
 * @author Martin Polakovic
 */
public class ImprovedScrollBarSkin extends ScrollBarSkin {
    private boolean isDragged = false;

    public ImprovedScrollBarSkin(ScrollBar scrollbar) {
        super(scrollbar);

        // install hover animation
        StackPane thumb = getFieldValue(this, StackPane.class, "thumb");
        Anim v = new Anim(millis(350),p -> thumb.setScaleX(1+p*p));
        Anim h = new Anim(millis(350),p -> thumb.setScaleY(1+p*p));
        scrollbar.addEventHandler(MOUSE_ENTERED, e -> {
            if(scrollbar.getOrientation()==VERTICAL) v.playOpen();
            else h.playOpen();
        });
        scrollbar.addEventHandler(MOUSE_EXITED, e -> {
            if(!isDragged) {
                if(scrollbar.getOrientation()==VERTICAL) v.playClose();
                else h.playClose();
            }
        });
        scrollbar.addEventHandler(DRAG_DETECTED, e -> isDragged = true);
        scrollbar.addEventHandler(MOUSE_RELEASED, e -> {
            if(isDragged) {
                isDragged = false;
                if(scrollbar.getOrientation()==VERTICAL) v.playClose();
                else h.playClose();
            }
        });
    }
}
