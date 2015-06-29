/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Widgets;

import Layout.Widgets.Features.Feature;
import java.util.List;
import static util.functional.Util.stream;
import static util.functional.Util.toS;

/**
 *
 * @author Plutonium_
 */
public interface WidgetInfo {
    
    /**
     * Name of the widget. "" by default.
     */
    String name();

    /**
     * Description of the widget.
     */
    String description();

    /**
     * Version of the widget
     */
    String version();

    /**
     * Author of the widget
     */
    String author();

    /**
     * Main developer of the widget.
     * @return
     */
    String programmer();

    /**
     * Co-developer of the widget.
     */
    String contributor();

    /**
     * Last time of change.
     * @return
     */
    String year();

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
    String howto();

    /**
     * Any words from the author, generally about the intention behind or bugs
     * or plans for the widget or simply unrelated to anything else.
     * <p>
     * For example: "To do: simplify user interface." or: "Discontinued."
     * @return
     */
    String notes();

    /** @return widget group. Default {@link Widget.Group.UNKNOWN} */
    Widget.Group group();
    
    /** @return all implemented features */
    List<Feature> getFeatures();
    
    /** @return true if widget's controller implements given feature */
    boolean hasFeature(Class feature);
    
    /** @return true if widget's controller implements all given features */
    default boolean hasFeatures(Class... features){
        return stream(features).allMatch(this::hasFeature);
    };

    /** Returns widget info as string. */
    default String toStr() {
        List<Feature> fs = getFeatures();
        String info = "";
        info += "\n\n"
             + "Widget\n\n"
             + "Name:\n\t" + name() + "\n"
             + (description().isEmpty() ? "" : "Info:\n\t" + description() + "\n")
             + (notes().isEmpty() ? "" : notes() + "\n")
             + (howto().isEmpty() ? "" : howto()  + "\n")
             + "Features:"
             + (fs.isEmpty() ? "\n\tnone" 
                             : toS(fs, f -> "\n\t" + f.name() + " - " + f.description()));
        
        return info;
    }

    
}
