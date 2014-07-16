/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author uranium
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WidgetInfo {
    
    /**
     * Name of the widget. "" by default.
     */
    String name() default "";
    
    /**
     * Description of the widget.
     */
    String description() default "";
    
    /**
     * Version of the widget
     */
    String version() default "";
    
    /**
     * Author of the widget
     */
    String author() default "";
    
    /**
     * Main developer of the widget.
     * @return 
     */
    String programmer() default "";
    
    /**
     * Co-developer of the widget.
     */
    String contributor() default "";
    
    /**
     * Last time of change.
     * @return 
     */
    String year() default "";
    
    /**
     * How to use text.
     * <pre>
     * For example:
     * "Available actions:\n" +
     * "    Drag cover away : Removes cover\n" +
     * "    Drop image file : Adds cover\n" +
     * "    Drop audio files : Adds files to tagger\n" +
     * "    Write : Saves the tags\n" +
     * "    Open list of tagged items"
     * </pre>
     * @return 
     */
    String howto() default "";
    
    /**
     * Any words from the author, generally about the intention behind or bugs 
     * or plans for the widget or simply unrelated to anything else information.
     * <p>
     * For example: "To do: simplify user interface." or: "Discontinued."
     * @return 
     */
    String notes() default "";
    
    /**
     * Group the widget should categorize under as. Default {@link Widget.Group.UNKNOWN}
     * @return 
     */
    Widget.Group group() default Widget.Group.UNKNOWN;
}