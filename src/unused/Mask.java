/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package unused;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 *
 * @author Plutonium_
 */
public class Mask {
    
    private final Canvas mask;
    
    private Mask() {
//        mask = new Region();
//        Paint p = new LinearGradient(0,0,50,50,true,CycleMethod.NO_CYCLE, new Stop(0, Color.TRANSPARENT));
//        mask.setBackground(new Background(new BackgroundFill(p, CornerRadii.EMPTY, Insets.EMPTY)));
        mask = new Canvas();
        GraphicsContext gc = mask.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0,0,25,25);
    }
    
    
    public static void install(Node n) {
        n.setClip(new Mask().mask);
    }
}
