/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import javafx.animation.FadeTransition;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 *
 * @author Plutonium_
 */
public class FadeButton extends StackPane {
    public Labeled normal = new Label();
    public Labeled hover = new Label();
    
    FadeTransition in = new FadeTransition();
    FadeTransition out = new FadeTransition();
    
    public FadeButton(FontAwesomeIconName icon, int size) {
        normal = new Icon(icon, size);
        hover = new Icon(icon, size);
        
        normal.getStyleClass().setAll("fade-button");
        normal.getStyleClass().setAll("fade-button-normal");
        hover.getStyleClass().setAll("fade-button-hover");
        
        normal.opacityProperty().bind(hover.opacityProperty().multiply(-1).add(1));
        
        normal.setMouseTransparent(true);
        hover.setMouseTransparent(true);
        
        hover.setOpacity(0);
        in.setNode(hover);
        in.setToValue(1);
        in.setDuration(Duration.millis(50));
        out.setNode(hover);
        out.setToValue(0);
        out.setDelay(Duration.millis(150));
        out.setDuration(Duration.millis(350));
        
        getChildren().addAll(normal,hover);
        
        // react on hover
        hoverProperty().addListener((o,ov,nv) -> {
            if(nv) {
                out.stop();
                in.play();
            } else {
                in.stop();
                out.play();
            }
        });
    }
    
    public void setIcon(FontAwesomeIconName icon) {
          normal.setText(icon.toString());
          hover.setText(icon.toString());
    }
}
