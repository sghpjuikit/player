
package Layout.Widgets.controller;

import java.util.List;

import Configuration.CachedConfigurable;
import Layout.Areas.Area;
import Layout.Layout;
import Layout.Widgets.Widget;
import Layout.Widgets.controller.io.Inputs;
import Layout.Widgets.controller.io.Outputs;
import Layout.Widgets.feature.Feature;
import gui.pane.IOPane;

/**
 * Controller is an object defining behavior of some graphical object and acts
 * as a bridge between that object and the rest of application. Controllers are
 * used to manage the content and control and define its behavior for example of 
 * fxml graphics.
 * <p>
 * Controller can be considered behavior defining object and also API of the
 * object to allow external control. Controller can make object fully autonomous
 * an inaccessible or provide certain degree of access from outside.
 * For more about how to use Controller without relying on type safety, read
 * {@link Feature}.
 * <p>
 * Encapsulation is still valid approach and it is recommended to make methods
 * and fields public only when trying to export state or functionality when using
 * the controller when knowing its full class 
 * <p>
 * Implements {@link Configuration.Configurable} interface which is useful for 
 * user customization of internal properties.
 * <p>
 * It is intended for controller to implement {@link Layout.Widgets.Features.Feature}
 * interfaces.
 * <p>
 * This is also marker interface. It helps flag objects that act as controllers,
 * even if they dont provide any public control. This way it is more clear whether
 * Object is its own controller, which is also possible. Such cases could be
 * required by frameworks that require Controllers.
 * <pre>
 * Below is the lifecycle of the fxml controller:
 * - widget loads (controller is null)
 * - controller is instantiated (constructor invoked)
 * - controller is assigned widget permanently ({@link #setWidget(Layout.Widgets.Widget)}) invoked
 * - controller is initialized {@link #init()} invoked
 * - configs hae their value set
 * - controller is refreshed (cnfigs applied) {@link #refresh()} invoked
 * -----
 * - controller lives
 * -----
 * - controller frees its resources permanently {@link #close()} invoked
 * - controller is garbage collected
 * </pre>
 * 
 * @author uranium
 */
public interface Controller<W extends Widget> extends CachedConfigurable<Object> {
    
    /**
     * Refreshes the controller state.
     * <p>
     * Brings GUI up to date ensuring all potential changes are accounted for.
     * Invoked after widget loads for the 1st time or manually by the user from
     * user interface.
     * <p>
     * Is invoked on user request (to bring the widget state up to date) or when
     * widget is loaded 1st time.
     * At this point all {@link Configuration.IsConfig configs} 
     * are available and have their values set, but not applied - that should be
     * executed in this method.
     * <p>
     * In case that widget has nothing to refresh, such implementations
     * could do nothing.
     */
    public void refresh();
    
    /**
     * Returns widget in relationship with this Controller object.
     * @return associated widget or null if none.
     */
    public W getWidget();
    
    /**
     * Executes immediately before widget is closed. Widget is not
     * expected to be used after this method is invoked. Use to free resources.
     * Note that incorrect or no releasing of the resources (such as listeners) 
     * might prevent this controller from being garbage collected.
     * Default implementation does nothing.
     */
    default void close(){}
    
    /**
     * Returns whether widget displays any content. Empty controller generally
     * means there is no content in the wodget whatsoever, but it depends
     * on implementation.
     * <p>
     * By default this method returns false;
     * 
     * @return 
     */
    default boolean isEmpty() { return false; }
    
    /**
     * Returns the graphics of the container where this widget is loaded. The area
     * provides access to the layout graph.
     *
     * @return area within which the widget this controller is associated with is
     * loaded in.*/
    default public Area getArea() {
        Object o = getWidget().load().getUserData();
        if(o instanceof Area) return (Area)o;
        return null; //throw new IllegalStateException(getWidget().getName() + "'s controller cant access Area");
    }
    
    Outputs getOutputs(); 
    Inputs getInputs(); 
    
    /** @return all implemented features */
    default List<Feature> getFeatures() {
        return getWidget().getFactory().getFeatures();
    }
    
    public default IOPane getActivityNode() {
        return getArea()==null ? null : getArea().actionpane;
    }
}
