/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.pane;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import gui.objects.window.stage.Window;
import util.access.V;
import util.animation.Anim;
import util.conf.IsConfig;
import util.reactive.SetƑ;

import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.util.Duration.millis;
import static main.App.APP;
import static util.graphics.Util.*;

/**
 * Pane laying 'above' window creating 'overlay' bgr above it.
 * <p/>
 * This implementation has no content. Rather than using {@link StackPane#getChildren()}, use
 * {@link #setContent(javafx.scene.layout.Pane)}, which also applies {@link #CONTENT_STYLECLASS}
 * styleclass on it. Content will align to center unless set otherwise.
 *
 * @author Martin Polakovic
 */
public class OverlayPane extends StackPane {

    private static final String IS_SHOWN = "visible";
    private static final String ROOT_STYLECLASS = "overlay-pane";
    static final String CONTENT_STYLECLASS = "overlay-pane-content";


    /** Display method. */
    @IsConfig(
        name = "Display method",
        info = "Can be shown per window or per screen. The latter provides more space as window "
              + "usually does not cover entire screen, but can get in the way of other apps.")
    public final V<Display> display = new V<>(Display.SCREEN_OF_MOUSE);
    /** Handlers called just after this pane was shown. */
    public final SetƑ onShown = new SetƑ();
    /** Handlers called just after this pane was hidden. */
    public final SetƑ onHidden = new SetƑ();

    public OverlayPane() {
        setVisible(false);

        getStyleClass().add(ROOT_STYLECLASS);
        setOnMouseClicked(e -> {
            if (e.getButton()==SECONDARY && isShown()) {
                hide();
                e.consume();
            }
        });
        addEventHandler(KeyEvent.ANY, e -> {
            // close on ESC press
            if (e.getEventType()==KeyEvent.KEY_PRESSED && e.getCode()==ESCAPE && isShown()) {
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
        if (!isShown()) {
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
        if (isShown()) {
            getProperties().remove(IS_SHOWN);
            animation.playCloseDo(this::animEnd);
        }
    }

    public void setContent(Pane contentRoot) {
        getChildren().add(contentRoot);
        contentRoot.getStyleClass().add(CONTENT_STYLECLASS);
    }

    public Pane getContent() {
        return getChildren().isEmpty() ? null : (Pane)getChildren().get(0);
    }

/****************************************** ANIMATION *********************************************/

    // depth of field transition
    private Display displayForHide; // prevents inconsistency in start() and stop(), see use
    private Anim animation = new Anim(30, this::animDo).dur(millis(250)).intpl(x->x*x); // lowering fps can help on fullHD+ resolutions
    private Stage stg = null;
    private BoxBlur blurBack = new BoxBlur(0,0,3);  // we need best possible quality
    private BoxBlur blurFront = new BoxBlur(0,0,1); // we do not need quality, hence iterations==1
    private Node opacityNode = null;
    private Node blurFrontNode = null;
    private Node blurBackNode = null;

    private void animStart() {
        displayForHide = display.get();
        display.get().animStart(this);
    }

    private void animDo(double x) {
        display.get().animDo(this, x);
    }

    private void animEnd() {
        // we must use the same display type as the one used
        displayForHide.animEnd(this);
    }


    public enum Display {
        WINDOW, SCREEN_OF_WINDOW, SCREEN_OF_MOUSE;

        private void animStart(OverlayPane op) {
            if (this==WINDOW) {
	            APP.windowManager.getActive().ifPresentOrElse(
	            	window -> {
			            // display overlay pane
			            AnchorPane root = window.root;
			            if (!root.getChildren().contains(op)) {
				            root.getChildren().add(op);
				            setAnchors(op,0d);
				            op.toFront();
			            }
			            op.setVisible(true);
			            op.requestFocus();     // 'bug fix' - we need focus or key events wont work

			            // apply effects (will be updated in animation)
			            //
			            // blur front performance optimization
			            // We want to apply blur on this overlay pane, but that can be potentially
			            // giant area (tests show HD and beyond can kill fps here). So we just apply
			            // the blur on the content of the overlay pane instead, which is generally
			            // smaller.
			            // More tweaking:
			            // It is possible with the overlay blur to:
			            // - disable (does not look the best)
			            // - decrease blur iteration count (we don not need super blur quality here)
			            // - decrease blur amount
			            //
			            op.opacityNode = window.content;
			            op.blurBackNode = window.subroot;
			            if (!op.getChildren().isEmpty()) op.blurFrontNode = op.getChildren().get(0);
			            op.blurBackNode.setEffect(op.blurBack);
			            op.blurFrontNode.setEffect(op.blurFront);

			            // start showing
			            op.animation.playOpenDo(null);
			            op.onShown.run();
		            },
		            () -> {
			            op.displayForHide = SCREEN_OF_MOUSE;
		            	SCREEN_OF_MOUSE.animStart(op);
		            }
	            );
            } else {
                Screen screen = this==SCREEN_OF_WINDOW
                                    ? APP.windowManager.getActive().map(Window::getScreen).orElseGet(() -> getScreen(APP.mouseCapture.getMousePosition()))
                                    : getScreen(APP.mouseCapture.getMousePosition());
                screenCaptureAndDo(screen, image -> {
                    Pane bgr = new Pane();
                         bgr.getStyleClass().add("bgr-image");   // replicate app window bgr for style & consistency
                    ImageView contentImg = new ImageView(image); // screen screenshot
                    StackPane content = new StackPane(bgr,contentImg); // we will use effect on this
                    StackPane root = new StackPane(content); // we will inject overlay pane here

                    op.stg = createFMNTStage(screen);
                    op.stg.setScene(new Scene(root));

                    // display overlay pane
                    if (!root.getChildren().contains(op)) {
                        root.getChildren().add(op);
                        op.toFront();
                    }
                    op.setVisible(true);
                    op.requestFocus();     // 'bug fix' - we need focus or key events wont work

                    // apply effects (will be updated in animation)
                    op.opacityNode = contentImg;
                    op.blurBackNode = contentImg;
                    if (!op.getChildren().isEmpty()) op.blurFrontNode = op.getChildren().get(0);
                    op.blurBackNode.setEffect(op.blurBack);
                    op.blurFrontNode.setEffect(op.blurFront);

                    op.animation.applier.accept(0d);
                    op.stg.show();
                    op.stg.requestFocus();

                    // start showing
                    op.animation.playOpenDo(null);
                    op.onShown.run();
                });
            }
        }

        private void animDo(OverlayPane op, double x) {
            if (op.opacityNode==null) return; // bug fix, not 100% sure why it is necessary

            op.opacityNode.setOpacity(1-x*0.5);
            op.setOpacity(x);
            // un-focus bgr
            op.blurBack.setHeight(15*x*x);
            op.blurBack.setWidth(15*x*x);
            op.opacityNode.setScaleX(1-0.02*x);
            op.opacityNode.setScaleY(1-0.02*x);
            // focus this
            op.blurFront.setHeight(20*(1-x*x));
            op.blurFront.setWidth(20*(1-x*x));
            // zoom in effect - make it appear this pane comes from the front
            op.setScaleX(1+2*(1-x));
            op.setScaleY(1+2*(1-x));
        }

        private void animEnd(OverlayPane op) {
            op.opacityNode.setEffect(null);
            op.blurFrontNode.setEffect(null);
            op.blurBackNode.setEffect(null);
            op.opacityNode = null;
            op.blurFrontNode = null;
            op.blurBackNode = null;
            op.onHidden.run();

            if (this==WINDOW) {
                op.setVisible(false);
            } else {
                op.stg.close();
            }
        }
    }

}