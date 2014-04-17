
package GUI.objects;

import Configuration.Configuration;
import GUI.ContextManager;
import javafx.animation.ScaleTransition;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import utilities.functional.functor.Procedure;

/**
 *
 * @author uranium
 */
public class ContextCircleElement extends ContextElement{
    private static final double size = 20;
    private static final double text_gap = 20;
    
    private final ContextMenu parent;
    private final Circle e = new Circle(size/2);
    private final Label label = new Label();
    private final ScaleTransition scale = new ScaleTransition(animDur,e);
    
    public ContextCircleElement(final ContextMenu _parent, String name, String tooltip, final Procedure behavior) {
        parent = _parent;
        e.setOpacity(0.4);
        e.setStroke(Paint.valueOf("black"));
        label.setMaxSize(Label.USE_COMPUTED_SIZE,Label.USE_COMPUTED_SIZE);
        label.setMinSize(Label.USE_COMPUTED_SIZE,Label.USE_COMPUTED_SIZE);
        label.setPrefSize(Label.USE_COMPUTED_SIZE,Label.USE_COMPUTED_SIZE);
            
        final double SF = Configuration.scaleFactor;
        e.setOnMousePressed( t -> {
            behavior.run();
            if(ContextManager.closeMenuOnAction)
                parent.close();
        });
        e.setOnMouseEntered( t -> {
            label.setScaleX(1+SF);
            label.setScaleY(1+SF);
            e.toFront(); // in case the elements overlap/nearby
            if (!allowAnimation) return;
            scale.stop();
            scale.setToX(1+SF);
            scale.setToY(1+SF);
            scale.play();
        });
        e.setOnMouseExited( t -> {
            label.setScaleX(1);
            label.setScaleY(1);
            if (!allowAnimation) return;
            scale.stop();
            scale.setToX(1);
            scale.setToY(1);
            scale.play();
        });
        label.setText(name);
        Tooltip.install(e, new Tooltip(tooltip));
    }
    
    @Override
    public Circle getElement() {
        return e;
    }
    
    @Override
    public double getHeight() {
        return size;
    }

    @Override
    public double getWidth() {
        return size;
    }
    
    public Label getLabel() {
        return label;
    }
    
    @Override
    public void relocate(double x, double y, double angle) {
        e.relocate(x, y);
        
        positionLabel(x, y, angle, 1, 1);
        
        double H = parent.getDisplay().getHeight();    // display height
        double W = parent.getDisplay().getWidth();     // display width
        
//        int xsign = 1;
//        int ysign = 1;
//        if (label.getLayoutX()+label.getWidth() > W  &&
//            label.getLayoutY()+label.getHeight()> H)    positionLabel(x, y, angle, -1, -1);
//        else
//        if (label.getLayoutX() < W  &&
//            label.getLayoutY() < H)    positionLabel(x, y, angle, -1, -1);
//        else
//        if (label.getLayoutY()+label.getHeight()> H)    positionLabel(x, y, angle, 1, -1);
//        else
//        if (label.getLayoutY() < H)                     positionLabel(x, y, angle, -1, 1);
//        else
        
        
//        if (label.getLayoutX()+label.getWidth() > W)    positionLabel(x, y, angle, -1, 1);
//        else
//        if (label.getLayoutX() < W)                     positionLabel(x, y, angle, -1, 1);
//        else
//        if (label.getLayoutY()+label.getHeight()> H)    positionLabel(x, y, angle, 1, -1);
//        else
//        if (label.getLayoutY() < H)                     positionLabel(x, y, angle, 1, -1);
    }
    
    private void positionLabel(double x, double y, double angle, int xsign, int ysign) {
        // position label

        
        double sin = xsign * text_gap * -Math.sin(angle); 
        double cos = ysign * text_gap * Math.cos(angle);
        
        double X = x + cos;
        double Y = y + sin;
        
        Y -= label.getHeight()/2;
        if (cos < 0) X -= label.getWidth();
        
        label.relocate(X,Y);
    }

}