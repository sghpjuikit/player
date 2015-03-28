/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.Animation;

import static java.util.Objects.requireNonNull;
import java.util.function.Consumer;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.util.Duration;

/**
 <p>
 @author Plutonium_
 */
public class Anim extends Transition {
    public final Consumer<Double> affector;
    public final DoubleProperty position = new SimpleDoubleProperty(0);

    public Anim(Consumer<Double> affector) {
        super();
        requireNonNull(affector);
        this.affector = affector;
    }
    public Anim(Duration length, Consumer<Double> affector) {
        this(affector);
        setCycleDuration(length);
    }
    public Anim(Duration length, Interpolator i, Consumer<Double> affector) {
        this(affector);
        setCycleDuration(length);
        setInterpolator(i);
    }
    
    @Override
    protected void interpolate(double frac) {
        position.set(frac);
        affector.accept(frac);
    }
    
    public <T extends Transition> T chain(T t) {
        setOnFinished(e -> t.play());
        return t;
    }
    
    public void playFrom(double position) {
        super.playFrom(getCycleDuration().multiply(position));
    }
    
    public void playFromEnd(double position) {
        setRate(-1);
        super.playFrom(getCycleDuration());
    }
    
    public void closeAndDo(Runnable action) {
        setOnFinished(a -> action.run());
        setRate(-1);
        playFrom(1-position.get());
    }
    public void openAndDo(Runnable action) {
        setOnFinished(a -> action.run());
        setRate(1);
        playFrom(position.get());
    }
    
}
