/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.spinner;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import java.util.Collections;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.Transition;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Skin;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Arc;
import javafx.util.Duration;
import static javafx.util.Duration.ZERO;
import util.graphics.fxml.ConventionFxmlLoader;

/**
 <p>
 @author Plutonium_
 */
public class Spinner extends ProgressIndicator {
    
    
    public Spinner() {
        this(1);
    }

    public Spinner(double progress) {
        // needed
        setSkin(createDefaultSkin());
        setProgress(progress);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new SpinnerSkin(this);
    }
 
    
    private static class SpinnerSkin extends BehaviorSkinBase<Spinner, BehaviorBase<Spinner>> {

        StackPane root = new StackPane();
        @FXML StackPane inner;
        @FXML StackPane outer;
        @FXML Arc inner_arc;
        @FXML Arc outer_arc;
        RotateTransition rt;
        boolean playing = false;
        
        public SpinnerSkin(Spinner spinner) {
            super(spinner, new BehaviorBase(spinner, Collections.emptyList()));
            
            // load fxml part
            new ConventionFxmlLoader(Spinner.class, root, this).loadNoEx();
            
            // register listeners
            registerChangeListener(spinner.indeterminateProperty(), "INDETERMINATE");
            registerChangeListener(spinner.progressProperty(), "PROGRESS");
            registerChangeListener(spinner.visibleProperty(), "VISIBLE");
            registerChangeListener(spinner.parentProperty(), "PARENT");
            registerChangeListener(spinner.sceneProperty(), "SCENE");
            
            outer.rotateProperty().bind(Bindings.subtract(360, inner.rotateProperty()));
            getChildren().add(root);
        }

        @Override
        public void dispose() {
            if(rt!=null) rt.stop();
            outer.rotateProperty().unbind();
        }

        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);

            if ("INDETERMINATE".equals(p)) {
                update();
            } else if ("PROGRESS".equals(p)) {
                update();
            } else if ("VISIBLE".equals(p)) {
                update();
            } else if ("PARENT".equals(p)) {
                update();
            } else if ("SCENE".equals(p)) {
                update();
            }
        }

        private void update() {
            Spinner s = getSkinnable();
            double p = s.getProgress();
            if(s.getParent()!=null && s.getScene()!=null && s.isVisible() && p!=1) {
                if(rt==null) {
                    rt = new RotateTransition(Duration.seconds(80), inner);
                    rt.setInterpolator(Interpolator.LINEAR);
                    rt.setCycleCount(Transition.INDEFINITE);
                    rt.setDelay(ZERO);
                    rt.setByAngle(360*100);
                }
                if(!playing) rt.play();
                playing = true;
            } else {
                if(playing && rt!=null) rt.pause();
                playing = false;
            }
        }
    
    }
}
