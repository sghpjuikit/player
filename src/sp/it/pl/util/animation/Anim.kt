@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package sp.it.pl.util.animation

import javafx.animation.Interpolator
import javafx.animation.ParallelTransition
import javafx.animation.SequentialTransition
import javafx.animation.Transition
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.event.EventHandler
import javafx.util.Duration
import sp.it.pl.util.functional.asArray
import sp.it.pl.util.functional.invoke
import sp.it.pl.util.units.divMillis
import sp.it.pl.util.units.minus
import sp.it.pl.util.units.times
import java.lang.Math.abs
import java.lang.Math.sqrt
import java.util.function.DoubleConsumer
import java.util.stream.Stream
import kotlin.jvm.JvmField as F
import kotlin.jvm.JvmOverloads as O
import kotlin.jvm.JvmStatic as S

private typealias IF = (Double) -> Double
private typealias Block = () -> Unit

/**
 * Features:
 * * Animation effect: Animation has [applier] that applies the animation position as a side effect.
 * * Interpolation: Convenience handling of interpolator as function mapping double value between 0-1 intervals
 * * Reverse play: Convenience methods to play forward/backward from current position for open/close effects
 * * Action: Simple action execution when animation ends or goes back
 * * Fluent API: For example [dur] or [intpl].
 *
 * This makes lots of practical effects very easy. The phase that runs forward (rate==1) is called 'opening' phase,
 * while the backward phase is called 'closing'. See [playClose] and [playOpen].
 *
 * For example animation used to show and hide button would fade it from 0 to 1 during the opening phase. If the
 * closing is invoked (before the opening finishes) the button will start fading out from its current opacity, not
 * the end (as usual).
 * And worse, using separate transitions would risk non-deterministic behavior or stopping the other transition, using
 * reverse interpolators and additional complexity.
 */
open class Anim: Transition {

    /** The side effect of the animation called in each loop. */
    @kotlin.jvm.JvmField val applier: DoubleConsumer
    /** Position of the animation after the interpolation transformation. May be outside of 0-1 range. */
    @kotlin.jvm.JvmField val position: DoubleProperty = SimpleDoubleProperty(0.0)
    /**
     * Whether animation should start from beginning/end if it is at the end/beginning. Default true.
     *
     * Affects families of methods [playFromDir], [playOpen], [playClose].
     *
     * Set to false if animation should not play if it already finished. For example [playClose] will play
     * animation from current position back to 0, but if the position already is 0, false will cause nothing to
     * happen, while true will play the animation again from 1 to 0.
     */
    @kotlin.jvm.JvmField var playAgainIfFinished = true

    /** Creates animation with specified frame rate and side effect called at every frame. */
    constructor(targetFPS: Double, sideEffect: DoubleConsumer): super(targetFPS) {
        this.applier = sideEffect
    }

    /** Creates animation with default frame rate and specified side effect called at every frame. */
    constructor(sideEffect: DoubleConsumer) {
        this.applier = sideEffect
    }

    /** Creates animation with default frame rate and specified duration and side effect called at every frame. */
    constructor(length: Duration, sideEffect: DoubleConsumer): this(sideEffect) {
        cycleDuration = length
    }

    /**
     *  Applies the [applier] at specified position (no interpolation is applied) and returns this (fluent style).
     *  Useful prior to animation start to avoid glitches at 0.0 or other key points.
     */
    fun applyAt(position: Double) = apply { applier(position) }

    /**
     *  Applies the [applier] at current [position] and returns this (fluent style).
     *  Useful prior to animation start to avoid glitches at 0.0 or other key points.
     */
    fun applyNow() = applyAt(position.value)

    /** Sets animation duration and returns this (fluent style). */
    fun dur(duration: Duration) = apply { cycleDuration = duration }

    /** Sets animation delay and returns this (fluent style). */
    fun delay(delay: Duration) = apply { this.delay = delay }

    /** Sets animation interpolator and returns this (fluent style). */
    fun intpl(interpolator: Interpolator) = apply { this.interpolator = interpolator }

    /** Sets animation interpolator and returns this (fluent style). */
    fun intpl(interpolator: IF) = intpl(
            object: Interpolator() {
                override fun curve(t: Double): Double {
                    return interpolator(t)
                }
            }
    )

    /** Sets [onFinished] returns this (fluent style). */
    fun then(block: Block?) = apply {
        onFinished = block?.let { EventHandler { block() } }
    }

    override fun interpolate(at: Double) {
        position.value = at
        applier(at)
    }

    /** @return true if not stopped or paused. */
    fun isRunning(): Boolean = currentTime!=Duration.ZERO && currentTime.lessThan(totalDuration)

    /** Equivalent to if (forward) [playOpen] else [playClose] */
    fun playFromDir(forward: Boolean) = if (forward) playOpen() else playClose()

    /**
     * Plays this animation onward from beginning if stopped else from its current position.
     *
     * Useful for animations that are used to both 'open' and 'close', i.e.,
     * are used to play with rate 1 and rate -1 to reverse-play their effect.
     */
    fun playOpen() {
        val p = if (!playAgainIfFinished && currentTime==cycleDuration) 0.0 else currentTime divMillis cycleDuration
        stop()
        playOpenFrom(p)
    }

    /**
     * Plays this animation backward from end if stopped else from its current position.
     *
     * Useful for animations that are used to both 'open' and 'close', i.e.,
     * are used to play with rate 1 and rate -1 to reverse-play their effect.
     */
    fun playClose() {
        val p = if (!playAgainIfFinished && currentTime.toMillis()==0.0) 1.0 else currentTime divMillis cycleDuration
        stop()
        playCloseFrom(1-p)
    }

    fun playOpenFrom(position: Double) {
        rate = 1.0
        super.playFrom(cycleDuration*position)
    }

    fun playCloseFrom(position: Double) {
        rate = -1.0
        super.playFrom(cycleDuration-cycleDuration*position)
    }

    fun playCloseDo(block: Block?) {
        onFinished = block?.let {
            EventHandler {
                onFinished = null
                it()
            }
        }
        playClose()
    }

    fun playOpenDo(block: Block?) {
        onFinished = block?.let {
            EventHandler {
                onFinished = null
                it()
            }
        }
        playOpen()
    }

    fun playOpenDoClose(blockMiddle: Block?) {
        playOpenDo {
            blockMiddle?.invoke()
            onFinished = null
            playClose()
        }
    }

    fun playCloseDoOpen(blockMiddle: Block?) {
        playCloseDo {
            blockMiddle?.invoke()
            onFinished = null
            playOpen()
        }
    }

    fun playOpenDoCloseDo(blockMiddle: Block?, blockEnd: Block) {
        playOpenDo {
            blockMiddle?.invoke()
            playCloseDo(blockEnd)
        }
    }

    fun playCloseDoOpenDo(blockMiddle: Block?, blockEnd: Block) {
        playCloseDo {
            blockMiddle?.invoke()
            playOpenDo(blockEnd)
        }
    }

    /**
     * Animation position transformers.
     * Transform linear 0-1 animation position function into different 0-1 function to produce nonlinear animation.
     */
    class Interpolators {

        /** Denotes partial interpolator representing a part of a (probably) non-continuous interpolator. */
        class Subrange(val fraction: Double, val interpolator: IF)

        private class Range(val start: Double, val end: Double, val interpolator: IF)

        companion object {

            /**
             * Returns interpolator as sequential combination of interpolators. Use
             * to achieve not continuous function by putting the interpolators
             * one after another and then mapping the resulting f back to 0-1.
             * The final interpolator will be divided into subranges in which each
             * respective interpolator will be used with the input in the range
             * mapped back to 0-1.
             *
             * For example pseudo code: of(0.2,x->0.5, 0.8,x->0.1) will
             * produce interpolator returning 0.5 for x <=0.2 and 0.1 for x > 0.2
             *
             * @param subranges interpolators of ranges covering range 0-1 in an increasing order
             * @throws RuntimeException if ranges do not give sum of 1
             */
            @S fun of(vararg subranges: Subrange): IF {
                if (subranges.sumByDouble { it.fraction }!=1.0)
                    throw RuntimeException("Sum of interpolator range must be 1.0")

                val ranges = ArrayList<Range>()
                var p1 = 0.0
                for (subrange in subranges) {
                    ranges += Range(p1, p1+subrange.fraction, subrange.interpolator)
                    p1 += subrange.fraction
                }

                return { at ->
                    ranges.find { at>=it.start && at<=it.end }
                            ?.let { it.interpolator((at-it.start)/(it.end-it.start)) }
                            ?: throw RuntimeException("Combined interpolator out of value at: $at")
                }
            }

            /** Returns reverse interpolator, which produces 1-interpolated_value. */
            @S fun reverse(i: IF): IF = { 1-i(it) }

            /** Returns reverse interpolator, which produces 1-interpolated_value. */
            @S fun reverse(i: Interpolator): IF = { 1-i.interpolate(0.0, 1.0, it) }

            @S fun isAround(proximity: Double, vararg points: Double): IF = { at ->
                points.find { at>it-proximity && at<it+proximity }
                        ?.let { 0.0 }
                        ?: 1.0
            }

            @S fun isAroundMin1(proximity: Double, vararg points: Double): IF = { at ->
                if (at<points[0]-proximity) 0.0
                else points.find { it>it-proximity && it<it+proximity }
                        ?.let { 0.0 }
                        ?: 1.0
            }

            @S fun isAroundMin2(proximity: Double, vararg points: Double): IF = { at ->
                if (at<points[0]-proximity) 0.0
                else points.find { it>it-proximity && it<it+proximity }
                        ?.let { abs((at-it+proximity)/(proximity*2)-0.5) }
                        ?: 1.0
            }

            @S fun isAroundMin3(proximity: Double, vararg points: Double): IF = { at ->
                if (at<points[0]-proximity) 0.0
                else points.find { it>it-proximity && it<it+proximity }
                        ?.let { sqrt(abs((at-it+proximity)/(proximity*2)-0.5)) }
                        ?: 1.0
            }
        }
    }

    companion object {

        /** @return animation with specified frame rate and side effect called at every frame. */
        @S fun anim(targetFPS: Double, sideEffect: (Double) -> Unit) = Anim(targetFPS, DoubleConsumer(sideEffect))

        /** @return animation with default frame rate and specified side effect called at every frame. */
        @S fun anim(sideEffect: (Double) -> Unit) = Anim(DoubleConsumer(sideEffect))

        /** @return animation with default frame rate and specified duration and side effect called at every frame. */
        @S fun anim(length: Duration, sideEffect: (Double) -> Unit) = Anim(length, DoubleConsumer(sideEffect))

        /** @return sequential animation playing the specified animations sequentially */
        @S @O fun animSeq(vararg ts: Transition, init: SequentialTransition.() -> Unit = {}) = SequentialTransition(*ts).apply { init() }

        /** @return sequential animation playing the specified animations sequentially */
        @S @O fun animSeq(ts: Stream<Transition>, init: SequentialTransition.() -> Unit = {}) = animSeq(*ts.asArray()).apply { init() }

        @S fun <T> animSeq(animated: List<T>, animFactory: (Int, T) -> Transition) = animSeq(animated.mapIndexed(animFactory).stream())

        /** @return parallel animation playing the specified animations in parallel */
        @S @O fun animPar(ts: Stream<Transition>, init: ParallelTransition.() -> Unit = {}) = animPar(*ts.asArray()).apply { init() }

        /** @return parallel animation playing the specified animations in parallel */
        @S @O fun animPar(vararg ts: Transition, init: ParallelTransition.() -> Unit = {}) = ParallelTransition(*ts).apply { init() }

        @S fun <T> animPar(animated: List<T>, animFactory: (Int, T) -> Transition) = animPar(animated.mapIndexed(animFactory).stream())

        @S fun mapTo01(x: Double, from: Double, to: Double): Double = when {
            x<=from -> 0.0
            x>=to -> 1.0
            else -> (x-from)/(to-from)
        }

        @S fun map01To010(x: Double, right: Double): Double {
            val left = 1-right
            return when {
                x<=left -> mapTo01(x, 0.0, left)
                x>=right -> 1-mapTo01(x, right, 1.0)
                else -> 1.0
            }
        }

        @S fun mapConcave(x: Double): Double = 1-abs(2*(x*x-0.5))

    }
}