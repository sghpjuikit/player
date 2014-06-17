
package Layout.Widgets;

import Configuration.Configurable;

/**
 * Controller is an object defining behavior of some graphical object and acts
 * as a bridge between that object and the rest of application. Controllers are
 * used to manage the content and control and define its behavior for example of 
 * fxml graphics.
 * <p>
 * Controller can be considered behavior defining object and also API of the
 * object to allow external control. Controller can make object fully autonomous
 * an inaccessible or provide certain degree of access from outside.
 * <p>
 * Encapsulation is still valid approach and it is recommended to make methods
 * and fields public only when trying to export functionality.
 * <p>
 * Controller is also communication channel between the object's behavior and its
 * enviroment capable of providing only necessary information handled somewhere
 * else. An example could be serialization where controller could represent a
 * disposable entity turning widget into light-weight object by Controller. This
 * separates logic of the object depending on where the control for given aspect
 * lies.
 * <p>
 * Implements {@link Configuration.Configurable} interface which is useful for 
 * user customization of internal properties.
 * For application and leveraging of this functionality refer to the Configurable
 * class.
 * <p>
 * This is also marker interface. It helps flag objects that act as controllers,
 * even if they dont provide any public control. This way it is more clear whether
 * Object is its own controller, which is also possible. Such cases could be
 * required by some wrappers that require their graphics to have Controllers.
 * <pre>
 * Below is the lifecycle of the fxml controller:
 * - widget loads
 * - controller is instantiated (constructor invoked)
 * - controller is assigned widget permanently
 * - controller is initialized by invoking the {@link #init()} method
 * - properties are assigned
 * - controller is refreshed by invoking the {@link #refresh()} method
 * -----
 * - controller lives
 * -----
 * - controller frees its resources permanently by invoking the {@link #OnClosing()} method
 * - controller is garbage collected
 * </pre>
 * Note that incorrect or no releasing of the resources in this' {@link #OnClosing()}
 * method might prevent this controller from being garbage collected.
 * <p>
 * To read some more on this, see the implementations.
 * <p>
 * @author uranium
 */
public interface Controller<W extends Widget> extends Configurable, AbstractController<W> {
    
    /**
     * Basic implementation of this method does nothing.
     */
    public void refresh();
    
    /**
     * Create relationship between this controller and specified widget. Usually
     * the relationship is permanent.
     * This method must be called before the gui loads.
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
     * Guaranteed to execute immediately before widget is closed. Widget is not
     * expected to be used after this method is invoked. Use to free resources.
     * Note that incorrect or no releasing of the resources (such as listeners) 
     * might prevent this controller from being garbage collected.
     * Default implementation does nothing.
     */
    default void OnClosing(){}
    
    /**
     * Returns whether widget displays any content. Empty controller generally
     * means there is no information in the wodget whatsoever, but it depends
     * on implementation.
     * <p>
     * By default this method returns false;
     * @return 
     */
    default boolean isEmpty() { return false; }
}
