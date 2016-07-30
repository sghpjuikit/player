package util.animation;

import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Stream;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Transition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.util.Duration;

import util.functional.Functors.Ƒ1;
import util.functional.Functors.Ƒ2;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static javafx.util.Duration.ZERO;
import static javafx.util.Duration.millis;
import static util.dev.Util.noØ;
import static util.functional.Util.forEachIStream;

/**
 * Features:
 * <ul>
 * <li> Animation effect<p/>
 * Each animator has an 'applier' which is a double {@link java.util.function.DoubleConsumer} that takes
 * the animation value as parameter (after it is interpolated) and applies it.
 * <p/>
 * The applier can do anything from setting opacity or position
 * <li> Simple interpolation<p/>
 * Interpolator is a function transforming double parameter p from 0-1 interval
 * to different double within the same interval.
 * <p/>
 * Interpolation can be set as mathematical double to double function. Reversing
 * interpolator is simply a matter of calling it with argument 1-p, where p is double
 * from 0-1 interval.
 * <p/>
 * Developer has a choice to set interpolator or use it within the applier itself,
 * or using both (which can reduce interpolator complexity).
 * <li> Reversibility<p/>
 * Can be played backwards to 'revert' its effect even when it is playing. The phase
 * that runs forward (rate==1) is called 'opening' phase while the backward
 * phase is called 'closing'.
 * <li> Fluent API for frequently used methods. For example {@link #dur(double)}
 * or {@link #intpl(java.util.function.DoubleUnaryOperator)} ) }
 * <p/>
 * For example animation used to show and hide button using opacity would
 * fade it from 0 to 1 during the opening phase. If the closing is invoked
 * the button will fade out from its current opacity, not from its maximum (1),
 * which would be the case with standard transition. Even worse, using separate
 * transition would require stopping the other transition, using reverse
 * interpolator, etc.
 * <p/>
 * See {@link #playClose()} and {@link #playOpen() }.
 * </ul>
 *
 * @author Martin Polakovic
 */
public class Anim extends Transition {

    public final DoubleConsumer applier;
    public final DoubleProperty position = new SimpleDoubleProperty(0);

    /** Creates animation with specified frame rate and sideffect called at every frame. */
    public Anim(double targetFPS, DoubleConsumer sideEffect) {
        super(targetFPS);
        noØ(sideEffect);
        this.applier = sideEffect;
    }

    /** Creates animation with default framerate and specified sideffect called at every frame. */
    public Anim(DoubleConsumer sideEffect) {
        noØ(sideEffect);
        this.applier = sideEffect;
    }

    /**
     * Convenience constructor.
     * Creates animation with default framerate and specified duration and sideffect called at every frame.
     */
    public Anim(Duration length, DoubleConsumer sideEffect) {
        this(sideEffect);
        setCycleDuration(length);
    }

    /**
     * Convenience constructor.
     * Creates animation with default framerate and specified duration, interpolator and sideffect called at every frame.
     */
    public Anim(Duration length, Interpolator i, DoubleConsumer sideEffect) {
        this(length, sideEffect);
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

    public Anim intpl(DoubleUnaryOperator i) {
        setInterpolator(new Interpolator() {
            @Override
            protected double curve(double t) {
                return i.applyAsDouble(t);
            }
        });
        return this;
    }

    public Anim intpl(double i) {
        return intpl(at -> i);
    }


    @Override
    protected void interpolate(double at) {
        position.set(at);
        applier.accept(at);
    }

    public Anim then(Runnable r) {
        setOnFinished(r==null ? null : e -> r.run());
        return this;
    }


    /** Returns true if not stopped or paused. */
    public boolean isRunning() {
        return getCurrentTime().lessThan(getTotalDuration());
    }

    /** *  Equivalent to {@code if (forward) playOpen(); else playClose();} */
    public void playFromDir(boolean forward) {
        if (forward) playOpen(); else playClose();
    }

    /**
     * Plays this animation onward from beginning if stopped else from its
     * current position.
     * <p/>
     * Useful for animations that are used to both 'open' and 'close', i.e.,
     * are used to play with rate 1 and rate -1 to reverse-play their effect.
     */
    public void playOpen() {
        double p = getCurrentTime().toMillis()/getCycleDuration().toMillis();
//               p = p==0 ? 1 : p; // should be or not?
        stop();
        playOpenFrom(p);
    }

    /**
     * Plays this animation backward from beginning if stopped else from its
     * current position.
     * <p/>
     * Useful for animations that are used to both 'open' and 'close', i.e.,
     * are used to play with rate 1 and rate -1 to reverse-play their effect.
     */
    public void playClose() {
        double p = getCurrentTime().toMillis()/getCycleDuration().toMillis();
//               p = p==0 ? 1 : p; // should be or not?
        stop();
        playCloseFrom(1-p);
    }

    public void playOpenFrom(double position) {
        setRate(1);
        super.playFrom(getCycleDuration().multiply(position));
    }

    public void playCloseFrom(double position) {
        setRate(-1);
        super.playFrom(getCycleDuration().subtract(getCycleDuration().multiply(position)));
    }

    public void playCloseDo(Runnable action) {
        setOnFinished(action==null ? null : a -> {
            setOnFinished(null);
            action.run();
        });
        playClose();
    }

    public void playOpenDo(Runnable action) {
        setOnFinished(action==null ? null : a -> {
            setOnFinished(null);
            action.run();
        });
        playOpen();
    }

    public void playOpenDoClose(Runnable middle) {
        playOpenDo(() -> {
            if (middle!=null) middle.run();
            setOnFinished(null);
            playClose();
        });
    }
    public void playCloseDoOpen(Runnable middle) {
        playCloseDo(() -> {
            if (middle!=null) middle.run();
            setOnFinished(null);
            playOpen();
        });
    }
    public void playOpenDoCloseDo(Runnable middle, Runnable end) {
        playOpenDo(() -> {
            if (middle!=null) middle.run();
            playCloseDo(end);
        });
    }
    public void playCloseDoOpenDo(Runnable middle, Runnable end) {
        playCloseDo(() -> {
            if (middle!=null) middle.run();
            playOpenDo(end);
        });
    }

/********************************** UTILITIES *********************************/

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

    public static Transition seq(Stream<Transition> ts) {
        Transition t = new SequentialTransition(ts.toArray(Transition[]::new));
        return t;
    }

    public static <T> Transition seq(List<T> animated, Ƒ2<Integer,T,Transition> animFactory) {
        return seq(forEachIStream(animated,animFactory));
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

    public static <T> Transition par(List<T> animated, Ƒ2<Integer,T,Transition> animFactory) {
        return par(forEachIStream(animated,animFactory));
    }

    public static double mapTo01(double x, double from, double to) {
        if (x<=from) return 0;
        if (x>=to) return 1;
        return (x-from)/(to-from);
    }

    public static double mapConcave(double x) {
        return 1-abs(2*(x*x-0.5));
    }


    /**
     * Animation appliers.
     * Consumers of animation position value - double of range 0-1, applying it
     * in some arbitrary way.
     */
    public interface Applier {

        /** Applier that scales node's x and y. */
        static DoubleConsumer scaleXY(Node n) {
            return x -> util.graphics.Util.setScaleXY(n,x);
        }

        /**
         * Text interpolator for 'text typing effect'. Creates function returning string substrings
         * of all lengths from 0 to string length. Linear and uses rounding (Math.floor).
         *
         * @param text
         * @return function transforming {@code <0,1>} double input into substrings of the provided
         * string, from beginning to character at the position best reflected by the input.
         */
        static Ƒ1<Double,String> typeText(String text) {
            int length = text.length();
            return x -> text.substring(0, (int) Math.floor(length * x));
        }
    }

    /**
     * Animation position transformers. Transform linear 0-1 animation position
     * function into different 0-1 function to produce nonlinear animation.
     */
    public interface Interpolators {
        /**
         * Returns interpolator as sequential combination of interpolators. Use
         * to achieve not continuous function by putting the interpolators
         * one after another and then mapping the resulting f back to 0-1.
         * The final interpolator will be divided into subranges in which each
         * respective interpolator will be used with the input in the range
         * mapped back to 0-1.
         * <p/>
         * For example pseudocode: of(0.2,x->0.5, 0.8,x->0.1) will
         * produce interpolator returning 0.5 for x <=0.2 and 0.1 for x > 0.2
         *
         * @param subranges couples of range and interpolator. Ranges must
         * give sum 1 and must be in an increasing order
         *
         * @throws IllegalArgumentException if ranges do not give sum of 1
         */
        @SafeVarargs
        static DoubleUnaryOperator of(Subrange... subranges) {
            if (Stream.of(subranges).mapToDouble(i -> i.fraction).sum() != 1)
                throw new IllegalArgumentException("sum of interpolator fractions must be 1");

            return x -> {
                double p1=0;
                for (Subrange subrange : subranges) {
                    double p2 = p1+subrange.fraction;
                    if (x<=p2) return subrange.interpolator.applyAsDouble((x-p1)/(p2-p1));
                    p1 = p2;
                }
                throw new RuntimeException("Combined interpolator out of value at: " + x);
            };
        }

        /** Returns reverse interpolator, which produces 1-interpolated_value. */
        static DoubleUnaryOperator reverse(DoubleUnaryOperator i) {
            return x -> 1-i.applyAsDouble(x);
        }

        /** Returns reverse interpolator, which produces 1-interpolated_value. */
        static DoubleUnaryOperator reverse(Interpolator i) {
            return x -> 1-i.interpolate(0d,1d, x);
        }

        static DoubleUnaryOperator isAround(double point_span, double... points) {
            return at -> {
                for (double p : points)
                    if (at>p-point_span && at<p+point_span)
                        return 0d;
                return 1d;
            };
        }

        static DoubleUnaryOperator isAroundMin1(double point_span, double... points) {
            return at -> {
                if (at<points[0]-point_span) return 0d;
                for (double p : points)
                    if (at>p-point_span && at<p+point_span)
                        return 0d;
                return 1d;
            };
        }
        static DoubleUnaryOperator isAroundMin2(double point_span, double... points) {
            return at -> {
                if (at<points[0]-point_span) return 0d;
                for (double p : points)
                    if (at>p-point_span && at<p+point_span)
                        return abs((at-p+point_span)/(point_span*2)-0.5);
                return 1d;
            };
        }
        static DoubleUnaryOperator isAroundMin3(double point_span, double... points) {
            return at -> {
                if (at<points[0]-point_span) return 0d;
                for (double p : points)
                    if (at>p-point_span && at<p+point_span)
                        return sqrt(abs((at-p+point_span)/(point_span*2)-0.5));
                return 1d;
            };
        }

	    DoubleUnaryOperator reverse = at -> 1-at;

	    /** Denotes partial interpolator representing a part of a (probably) non-continuous interpolator. */
	    class Subrange {
		    public final double fraction;
		    public final DoubleUnaryOperator interpolator;

		    public Subrange(double from, DoubleUnaryOperator interpolator) {
			    this.fraction = from;
			    this.interpolator = interpolator;
		    }
	    }
    }
}