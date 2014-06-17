
package GUI.Traits;

import Configuration.Configuration;
import javafx.animation.ScaleTransition;
import javafx.beans.property.ObjectProperty;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 *
 * @author uranium
 */
public interface ScaleOnHoverTrait extends HoverableTrait {
    
    /**
     * Duration of the scaling animation effect when transitioning to gover state.
     */
    public ObjectProperty<Duration> DurationOnHoverProperty();
    
    /**
     * Sets value of {@link #DurationOnHoverProperty() onHoverDurationProperty}.
     */
    default public void setDurationOnHover(Duration value) {
        DurationOnHoverProperty().set(value);
    }
    
    /**
     * Returns value of {@link #DurationOnHoverProperty() onHoverDurationProperty}.
     */
    default public Duration getDurationOnHover() {
        return DurationOnHoverProperty().get();
    }
    
    /**
     * Installs scaling behavior on mouse over/exit.Works properly only
     * if ScaleProperty of the control is 1.
     * This method must be first invoked or the behavior will not function. It
     * should be invoked in constructor and only once per object's life cycle.
     */
    default public void installScaleOnHover() {
        double diff =  Configuration.scaleFactor;
        ScaleTransition t = new ScaleTransition(getDurationOnHover(), getNode());
                        t.setToX(1+diff);
                        t.setToY(1+diff);
        ScaleTransition y = new ScaleTransition(getDurationOnHover(), getNode());
                        y.setToX(1);
                        y.setToY(1);
        getNode().addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
            if (Configuration.allowAnimation)
                if(isHoverable())
                    t.play();
        });
        getNode().addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            if (Configuration.allowAnimation)
                if(isHoverable())
                    y.play();
     
        });
    }
}

