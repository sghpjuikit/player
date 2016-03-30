/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.animation;

import java.util.function.Consumer;

import com.sun.javafx.tk.Toolkit;
import com.sun.scenario.animation.AbstractMasterTimer;
import com.sun.scenario.animation.shared.TimerReceiver;

/**
 * Timer, that executes behavior in each frame while it is active.
 * <p/>
 * The methods {@link #start()} and {@link #stop()} allow to start and stop the timer.
 *
 * @author Martin Polakovic
 */
public final class Loop {

    private final AbstractMasterTimer timer = Toolkit.getToolkit().getMasterTimer();
    private final TimerReceiver timerReceiver;
    private boolean active;

    /**
     * Creates a new loop.
     *
     * @param behavior behavior to execute. Takes 1 parameter - The timestamp of the current frame
     * given in nanoseconds. This value will be the same for all {@code AnimationTimers} called
     * during one frame.
     */
    public Loop(Consumer<Long> behavior) {
        this.timerReceiver = behavior::accept;
    }

    /** Creates a new loop. */
    public Loop(Runnable behavior) {
        this.timerReceiver = now -> behavior.run();
    }

    /** Starts this loop. Once it is started, the behavior will be called in every frame. */
    public void start() {
        if (!active) {
            timer.addAnimationTimer(timerReceiver);
            active = true;
        }
    }

    /** Stops this loop. It can be activated again by calling {@link #start()}. */
    public void stop() {
        if (active) {
            timer.removeAnimationTimer(timerReceiver);
            active = false;
        }
    }
}