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
import javafx.scene.Node;
import javafx.util.Duration;
import static javafx.util.Duration.ZERO;
import static javafx.util.Duration.millis;
import util.Util;
import util.collections.Tuple2;

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
    
    public Anim then(Runnable r) {
        setOnFinished(r==null ? null : e -> r.run());
        return this;
    }
    
    
    public void playFrom() {
        playFrom(getCurrentTime().toMillis()/getCycleDuration().toMillis());
    }
    
    public void playBFrom() {
        playBFrom(getCurrentTime().toMillis()/getCycleDuration().toMillis());
    }
    
    public void playFrom(double position) {
        super.playFrom(getCycleDuration().multiply(position));
    }
    
    public void playBFrom(double position) {
        setRate(-1);
        super.playFrom(getCycleDuration().subtract(getCycleDuration().multiply(position)));
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
    

    
    /**
     * Animation appliers.
     * Consumers of animation position value - double of range 0-1, applying it
     * in some arbitrary way.
     */
    public static interface Affectors {
        
        /** Affector that scales node's x and y. */
        public static Consumer<Double> scaleXY(Node n) {
            return x -> Util.setScaleXY(n,x);
        }
    }
    
    /**
     * Animation position transformers. Transform linear 0-1 animation position
     * function into different 0-1 function to produce nonlinear animation.
     */
    public static interface Interpolators {
        
        /**
         * Returns interpolator as sequential combination of interpolators. Use 
         * to achieve not continuous function by putting the interpolators
         * one after another and then mapping the resulting f back to 0-1.
         * The final interpolator will be divided into subranges in which each
         * respective interpolator will be used with the input in the range
         * mapped back to 0-1.
         * <p>
         * For example pseudocode: of(0.2,x->0.5, 0.8,x->0.1) will
         * produce interpolator returning 0.5 for x <=0.2 and 0.1 for x > 0.2 
         * 
         * @param interpolators couples of range and interpolator. Ranges must
         * give sum 1 and must be in an increasing order
         * 
         * @throws IllegalArgumentException if ranges dont give sum of 1
         */
        public static Function<Double,Double> of(Tuple2<Double,Function<Double,Double>>... interpolators) {
            if(Stream.of(interpolators).mapToDouble(i->i._1).sum()!=1)
                throw new IllegalArgumentException("sum of interpolator fractions must be 1");
            
            return x -> {
                double p1=0;
                for(Tuple2<Double,Function<Double,Double>> i : interpolators) {
                    double p2=p1+i._1;
                    if(x<=p2) return i._2.apply((x-p1)/(p2-p1));
                    p1=p2;
                }
                throw new RuntimeException("Combined interpolator out of value at: " + x);
            };
        }
        
        /** Returns reverse interpolator, which produces 1-interpolated_value. */
        public static Function<Double,Double> reverse(Function<Double,Double> i) {
            return x -> 1-i.apply(x);
        }
        
        /** Returns reverse interpolator, which produces 1-interpolated_value. */
        public static Function<Double,Double> reverse(Interpolator i) {
            return x -> 1-i.interpolate(0d,1d,(double)x);
        }
        
        public static Function<Double,Double> isAround(double point_span, double... points) {
            return at -> {
                for(double p : points)
                    if(at>p-point_span && at<p+point_span)
                        return 0d;
                return 1d;
            };
        }

        public static Function<Double,Double> isAroundMin1(double point_span, double... points) {
            return at -> {
                if(at<points[0]-point_span) return 0d;
                for(double p : points)
                    if(at>p-point_span && at<p+point_span)
                        return 0d;
                return 1d;
            };
        }
        public static Function<Double,Double> isAroundMin2(double point_span, double... points) {
            return at -> {
                if(at<points[0]-point_span) return 0d;
                for(double p : points)
                    if(at>p-point_span && at<p+point_span)
                        return abs((at-p+point_span)/(point_span*2)-0.5);
                return 1d;
            };
        }
        public static Function<Double,Double> isAroundMin3(double point_span, double... points) {
            return at -> {
                if(at<points[0]-point_span) return 0d;
                for(double p : points)
                    if(at>p-point_span && at<p+point_span)
                        return sqrt(abs((at-p+point_span)/(point_span*2)-0.5));
                return 1d;
            };
        }
        
        public static final Function<Double,Double> reverse = at -> 1-at;
    }
}
