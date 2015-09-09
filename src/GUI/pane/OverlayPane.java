/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.pane;

import javafx.scene.effect.BoxBlur;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import gui.objects.Window.stage.Window;
import util.animation.Anim;

import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.util.Duration.millis;
import static util.graphics.Util.setAnchors;

/**
 * Pane laying 'above' window creating 'overlay' bgr above it.
 * <p>
 * This implementation has no content. Rather than using {@link StackPane#getChildren()}, use
 * {@link #setContent(javafx.scene.layout.Pane)}, which also applies {@link #CONTENT_STYLECLASS}
 * styleclass on it. Content will align to center unless set otherwise.
 *
 * @author Plutonium_
 */
public class OverlayPane extends StackPane {

    private static final String IS_SHOWN = "visible";
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
            // close on ESC press
            if(e.getEventType()==KeyEvent.KEY_PRESSED && e.getCode()==ESCAPE && isShown()) {
                hide();
            }
            // prevent events from propagating
            // user should not be able to interact with UI below
            e.consume();
        });

    }

    /**
     * Shows this pane. The content should be set before calling this method.
     * @see #setContent(javafx.scene.layout.Pane)
     */
    public void show() {
        if(!isShown()) {
            getProperties().put(IS_SHOWN,IS_SHOWN);
            animStart();
        }
    }

    /** Returns true iff {@link #show()} has been called and {@link #hide()} not yet. */
    public boolean isShown() {
        return getProperties().containsKey(IS_SHOWN);
    }

    /** Hides this pane. */
    public void hide() {
        if(isShown()) {
            getProperties().remove(IS_SHOWN);
            animation.playCloseDo(this::animEnd);
        }
    }

    public void setContent(Pane contentRoot) {
        getChildren().add(contentRoot);
        contentRoot.getStyleClass().add(CONTENT_STYLECLASS);
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
        requestFocus();     // 'bug fix' - we need focus or key events wont work
        bgr = Window.getActive().content;
        Window.getActive().front.setEffect(blurback);
        Window.getActive().back.setEffect(blurback);
        animation.playOpenDo(null);
    }

    private void animDo(double x) {
        if(bgr==null) return; // bugfix, not 100% sure why it is necessary

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
