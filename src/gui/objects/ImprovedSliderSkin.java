/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects;

import javafx.scene.control.Slider;
import javafx.scene.control.skin.SliderSkin;
import javafx.scene.layout.StackPane;

import util.animation.Anim;

import static javafx.geometry.Orientation.VERTICAL;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.util.Duration.millis;
import static util.type.Util.getFieldValue;

/**
 * SliderSkin skin that adds animations & improved usability - track expands on mouse hover.
 *
 * @author Martin Polakovic
 */
public class ImprovedSliderSkin extends SliderSkin {

    public ImprovedSliderSkin(Slider slider) {
        super(slider);

        // install hover animation
        StackPane track = getFieldValue(this, StackPane.class, "track");
        Anim v = new Anim(millis(350),p -> track.setScaleX(1+p*p));
        Anim h = new Anim(millis(350),p -> track.setScaleY(1+p*p));
        slider.addEventHandler(MOUSE_ENTERED, e -> {
            if(slider.getOrientation()==VERTICAL) v.playOpen();
            else h.playOpen();
        });
        slider.addEventHandler(MOUSE_EXITED, e -> {
            if(slider.getOrientation()==VERTICAL) v.playClose();
            else h.playClose();
        });
    }
}
