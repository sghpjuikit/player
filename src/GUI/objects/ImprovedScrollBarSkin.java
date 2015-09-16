/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects;

import javafx.scene.control.ScrollBar;
import javafx.scene.layout.StackPane;

import com.sun.javafx.scene.control.skin.ScrollBarSkin;

import util.animation.Anim;

import static javafx.geometry.Orientation.VERTICAL;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.util.Duration.millis;
import static util.Util.getFieldValue;

/**
 * ScrollBar skin that adds animations & improved usability - thumb expands on mouse hover.
 * 
 * @author Plutonium_
 */
public class ImprovedScrollBarSkin extends ScrollBarSkin {

    public ImprovedScrollBarSkin(ScrollBar scrollbar) {
        super(scrollbar);

        StackPane thumb = getFieldValue(this, StackPane.class, "thumb");
        Anim v = new Anim(millis(350),p -> thumb.setScaleX(1+p*p));
        Anim h = new Anim(millis(350),p -> thumb.setScaleY(1+p*p));
        getSkinnable().addEventHandler(MOUSE_ENTERED, e -> {
            if(scrollbar.getOrientation()==VERTICAL) v.playOpen();
            else h.playOpen();
        });
        getSkinnable().addEventHandler(MOUSE_EXITED, e -> {
            if(scrollbar.getOrientation()==VERTICAL) v.playClose();
            else h.playClose();
        });
    }

}
