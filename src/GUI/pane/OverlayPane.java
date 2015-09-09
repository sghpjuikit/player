/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.pane;

import javafx.scene.effect.BoxBlur;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

import gui.objects.Window.stage.Window;
import util.animation.Anim;

import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.util.Duration.millis;
import static util.graphics.Util.setAnchors;

/**
 * Pane laying 'above' window creating 'overlay' bgr above it. This implementation has no content.
 *
 * @author Plutonium_
 */
public class OverlayPane extends StackPane {

    private static final String ROOT_STYLECLASS = "overlay-pane";
    static final String CONTENT_STYLECLASS = "overlay-pane-content";

    public OverlayPane() {
        setVisible(false);

        setEffect(blurfront);
        getStyleClass().add(ROOT_STYLECLASS);
        setOnMouseClicked(e -> {
            if(e.getButton()==SECONDARY && isShown()) {
                hide();
                e.consume();
            }
        });
        addEventFilter(KeyEvent.ANY, e -> {
            System.out.println("key det " + e.getCode());
            if(e.getEventType()==KeyEvent.KEY_PRESSED && e.getCode()==ESCAPE && isShown()) {
                hide();
            }
            e.consume();
        });
//        setOnKeyPressed(e -> {  // isnt quite working yet, something consuming ESCAPEs?
//            if(e.getCode()==ESCAPE && isShown()) {
//                hide();
//                e.consume();
//            }
//        });

    }

    public void show() {
        if(!isShown())
            animStart();
    }

    public boolean isShown() {
        return isVisible();
    }

    public void hide() {
        if(isShown())
            animation.playCloseDo(this::animEnd);
    }

/****************************************** ANIMATION *********************************************/

    BoxBlur blurback = new BoxBlur(0,0,3);
    BoxBlur blurfront = new BoxBlur(0,0,3);
    AnchorPane bgr;
    // depth of field transition
    Anim animation = new Anim(millis(350),this::animDo).intpl(x->x*x);

    private void animStart() {
        // attach to scenegraph
        AnchorPane root = Window.getActive().root;
        if(!root.getChildren().contains(this)) {
            root.getChildren().add(this);
            setAnchors(this,0d);
            toFront();
        }
        // show
        setVisible(true);
        requestFocus();
//        setFocused(true);
        bgr = Window.getActive().content;
        Window.getActive().front.setEffect(blurback);
        Window.getActive().back.setEffect(blurback);
        animation.playOpenDo(null);
    }

    private void animDo(double x) {
        if(bgr==null) return; // bugfix, not 100% sure its necessary

        bgr.setOpacity(1-x*0.5);
        setOpacity(x);
        // unfocus bgr
        blurback.setHeight(15*x*x);
        blurback.setWidth(15*x*x);
        bgr.setScaleX(1-0.02*x);
        bgr.setScaleY(1-0.02*x);
        // focus this
        blurfront.setHeight(28*(1-x*x));
        blurfront.setWidth(28*(1-x*x));
        // zoom in effect - make it appear this pane comes from the front
        setScaleX(1+2*(1-x));
        setScaleY(1+2*(1-x));
    }

    private void animEnd() {
        setVisible(false);
        Window.getActive().content.setEffect(null);
        Window.getActive().header.setEffect(null);
        bgr=null;
    }

}
