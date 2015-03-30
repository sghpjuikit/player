/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Spinner;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import java.io.IOException;
import java.util.Collections;
import javafx.animation.RotateTransition;
import javafx.animation.Transition;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Skin;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import static javafx.util.Duration.ZERO;

/**
 <p>
 @author Plutonium_
 */
public class Spinner extends ProgressIndicator {
    
    
    public Spinner() {
        this(0);
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
        RotateTransition rt;
        boolean playing = false;
        
        public SpinnerSkin(Spinner spinner) {
            super(spinner, new BehaviorBase(spinner, Collections.emptyList()));
            
            FXMLLoader fxmlLoader = new FXMLLoader(Spinner.class.getResource("Spinner.fxml"));
            fxmlLoader.setController(this);
            fxmlLoader.setRoot(root);
            try {
                fxmlLoader.load();
            } catch (IOException ex) {
                throw new RuntimeException("SimpleConfiguratorComponent source data coudlnt be read.");
            }
            
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
            if(s.getParent()!=null && s.getScene()!=null && p!=1 && p!=0) {
//            if(s.getParent()!=null && s.getScene()!=null && s.isVisible() && p!=1 && p!=0) {
                if(rt==null) {
                    rt = new RotateTransition(Duration.seconds(80), inner);
                    rt.setCycleCount(Transition.INDEFINITE);
                    rt.setDelay(ZERO);
                    rt.setByAngle(360*100);
                }
                if(!playing) rt.play();
                playing = true;
            } else {
                if(playing) rt.pause();
                playing = false;
            }
        }
    
    }
}
