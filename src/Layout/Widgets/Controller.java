
package Layout.Widgets;

import Configuration.Configurable;
import Layout.Layout;
import Layout.Widgets.Features.Feature;
import javafx.scene.Node;
import util.Closable;

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
public interface Controller<W extends Widget> extends Configurable<Object>, AbstractController<W>, Closable {
    
    /**
     * 
     * Refreshes the controller state.
     * <p>
     * Brings GUI up to date ensuring all potential changes are accounted for.
     * <p>
     * Designer should strive to make the controller capable of handling any 
     * state changes individually, when they happen, but sometimes
     * it isnt possible. For example an external source (I/O) change can happen.
     * <p>
     * This method is called automatically when widget is loaded, after it has been
     * initialized to restore its state.
     * At this point all {@link Configuration.IsConfig configs} of 
     * are available and have their values set, but not applied - that should be
     * executed in this method.
     * <p>
     * Because configs are a state of this controller, this method should 
     * always apply all configs.
     * It is guaranteed that all configs have their values initialized (set) when
     * this method is ran for the first time after loading the widget.
     * <p>
     * In case that widget has nothing to refresh, such implementations
     * could do nothing.
     * <p>
     * Default implementation of this method does nothing.
     * <p>
     * dev note: This method should always apply all of the applicable state to
     * ensure consistency. It should not be invoked unless needed - it is advised
     * to be able to apply every aspect of the state individually and only invoke
     * this method to load initial state during deserialization or on direct user
     * request.
     */
    public void refresh();
    
    /**
     * Create relationship between this controller and specified widget. Usually
     * the relationship is permanent.
     * This method must be called before the widget loads.
     * @param w 
     */
    @Override
    public void setWidget(W w);
    
    /**
     * Returns widget in relationship with this Controller object.
     * @return associated widget or null if none.
     */
    @Override
    public W getWidget();
    
    /**
     * {@inheritDoc}
     * <p>
     * Executes immediately before widget is closed. Widget is not
     * expected to be used after this method is invoked. Use to free resources.
     * Note that incorrect or no releasing of the resources (such as listeners) 
     * might prevent this controller from being garbage collected.
     * Default implementation does nothing.
     */
    @Override
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
    
    default public Node getActivityNode() {
        return null;
    }
}
