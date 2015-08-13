/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.function.Consumer;

import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import org.reactfx.EventSource;
import org.reactfx.Subscription;

import static java.time.Duration.ofMillis;
import static javafx.scene.input.MouseEvent.*;

/**
 * Experimental utilities and ideas.
 * 
 * @author Plutonium_
 */
public class UtilExp {

    public static Subscription hovered(Node n, Consumer<? super Boolean> handler) {
        EventSource<Boolean> h = new EventSource<>();
        EventHandler<MouseEvent> htrue = e -> h.push(true);
        EventHandler<MouseEvent> hfalse = e -> h.push(false);
        ChangeListener<Boolean> hx = (o,ov,nv) -> h.push(nv);
        
        n.hoverProperty().addListener(hx);
//        n.addEventFilter(MOUSE_MOVED, htrue);
//        n.addEventFilter(MOUSE_ENTERED_TARGET, htrue);
//        n.addEventFilter(MOUSE_EXITED_TARGET, hfalse);
        Subscription s = h.successionEnds(ofMillis(50)).subscribe(handler);
        
        return Subscription.multi(
                s,
                () -> n.removeEventFilter(MOUSE_MOVED, htrue),
                () -> n.removeEventFilter(MOUSE_ENTERED_TARGET, htrue),
                () -> n.removeEventFilter(MOUSE_EXITED_TARGET, hfalse),
                () -> n.hoverProperty().removeListener(hx)
        );
    }
    
    
    
    //http://www.coderanch.com/t/622070/JavaFX/java/control-Tooltip-visible-time-duration
    /**
     * Tooltip behavior is controlled by a private class javafx.scene.control.Tooltip$TooltipBehavior.
     * All Tooltips share the same TooltipBehavior instance via a static private member BEHAVIOR, which
     * has default values of 1sec for opening, 5secs visible, and 200 ms close delay (if mouse exits from node before 5secs).
     *
     * The hack below constructs a custom instance of TooltipBehavior and replaces private member BEHAVIOR with
     * this custom instance.
     */
    public static void setupCustomTooltipBehavior(int openDelayInMillis, int visibleDurationInMillis, int closeDelayInMillis) {
        try {
             
            Class TTBehaviourClass = null;
            Class<?>[] declaredClasses = Tooltip.class.getDeclaredClasses();
            for (Class c:declaredClasses) {
                if (c.getCanonicalName().equals("javafx.scene.control.Tooltip.TooltipBehavior")) {
                    TTBehaviourClass = c;
                    break;
                }
            }
            if (TTBehaviourClass == null) {
                // abort
                return;
            }
            Constructor constructor = TTBehaviourClass.getDeclaredConstructor(
                    Duration.class, Duration.class, Duration.class, boolean.class);
            if (constructor == null) {
                // abort
                return;
            }
            constructor.setAccessible(true);
            Object newTTBehaviour = constructor.newInstance(
                    new Duration(openDelayInMillis), new Duration(visibleDurationInMillis),
                    new Duration(closeDelayInMillis), false);
            if (newTTBehaviour == null) {
                // abort
                return;
            }
            Field ttbehaviourField = Tooltip.class.getDeclaredField("BEHAVIOR");
            if (ttbehaviourField == null) {
                // abort
                return;
            }
            ttbehaviourField.setAccessible(true);
             
            // Cache the default behavior if needed.
            Object defaultTTBehavior = ttbehaviourField.get(Tooltip.class);
            ttbehaviourField.set(Tooltip.class, newTTBehaviour);
             
        } catch (Exception e) {
            System.out.println("Aborted setup due to error:" + e.getMessage());
        }
    }
}
