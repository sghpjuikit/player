/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.Animation;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static java.util.Objects.requireNonNull;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Transition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.util.Duration;
import static javafx.util.Duration.ZERO;
import static javafx.util.Duration.millis;

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
        this(length,affector);
        setInterpolator(i);
    }
    
    
    public Anim dur(Duration d) {
        setCycleDuration(d);
        return this;
    }
    
    public Anim dur(double durMs) {
        setCycleDuration(millis(durMs));
        return this;
    }
    
    public Anim delay(Duration d) {
        setDelay(d);
        return this;
    }
    
    public Anim delay(double delayMs) {
        setDelay(millis(delayMs));
        return this;
    }
    
    public Anim intpl(Interpolator i) {
        setInterpolator(i);
        return this;
    }
    
    public Anim intpl(Function<Double,Double> i) {
        setInterpolator(new Interpolator() {
            @Override
            protected double curve(double t) {
                return i.apply(t);
            }
        });
        return this;
    }
    
    public Anim intpl(double i) {
        return intpl(at -> i);
    }
    
    @Override
    protected void interpolate(double frac) {
        position.set(frac);
        affector.accept(frac);
    }
    
    
//    public <T extends Transition> T then(T t) {
//        setOnFinished(e -> t.play());
//        return t;
//    }
    
    public Anim then(Runnable r) {
        setOnFinished(e -> r.run());
        return this;
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
    
    
    
    
    
    public static Transition seq(Transition... ts) {
        return seq(ZERO, ts);
    }
    public static Transition seq(Duration delay, Transition... ts) {
        Transition t = new SequentialTransition(ts);
        t.setDelay(delay);
        return t;
    }
    public static Transition seq(Duration delay, Stream<Transition> ts) {
        Transition t = new SequentialTransition(ts.toArray(Transition[]::new));
        t.setDelay(delay);
        return t;
    }
    
    public static Transition par(Transition... ts) {
        return par(ZERO, ts);
    }
    public static Transition par(Stream<Transition> ts) {
        return par(ZERO, ts);
    }
    public static Transition par(Duration delay, Stream<Transition> ts) {
        return par(delay, ts.toArray(Transition[]::new));
    }
    public static Transition par(Duration delay, Interpolator i, Stream<Transition> ts) {
        Transition t =  par(delay, ts.toArray(Transition[]::new));
        t.setInterpolator(i);
        return t;
    }
    public static Transition par(Duration delay, Transition... ts) {
        Transition t = new ParallelTransition(ts);
        t.setDelay(delay);
        return t;
    }
    
    public static double isAround(double at, double point_span, double... points) {
        for(double p : points)
            if(at>p-point_span && at<p+point_span)
                return 0;
        return 1;
    }
    
    public static double isAroundMin1(double at, double point_span, double... points) {
        if(at<points[0]) return 0;
        for(double p : points)
            if(at>p-point_span && at<p+point_span)
                return 0;
        return 1;
    }
    public static double isAroundMin2(double at, double point_span, double... points) {
        if(at<points[0]) return 0;
        for(double p : points)
            if(at>p-point_span && at<p+point_span)
                return abs((at-p+point_span)/(point_span*2)-0.5);
        return 1;
    }
    public static double isAroundMin3(double at, double point_span, double... points) {
        if(at<points[0]) return 0;
        for(double p : points)
            if(at>p-point_span && at<p+point_span)
                return sqrt(abs((at-p+point_span)/(point_span*2)-0.5));
        return 1;
    }
}
