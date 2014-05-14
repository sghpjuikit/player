
package Layout.Widgets;

import Configuration.Configurable;

/**
 * Controller defines object defining behavior of some graphical object and acts
 * as a bridge between that object and the rest of application. Controllers are
 * used to control and define behavior for example of fxml graphics and turn
 * self-sustained object into controllable one.
 * <p>
 * Encapsulation is still valid approach and it is recommended to make methods
 * and fields public only when trying to export functionality.
 * <p>
 * Controller can be considered behavior defining object and also API of the
 * object to allow external control. Controller can make object fully autonomous
 * an inaccessible or provide certain degree of access from outside.
 * <p>
 * Controller is also communication channel between the object's behavior and its
 * enviroment capable of providing only necessary information handled somewhere
 * else. An example could be serialization where controller could represent a
 * disposable entity turning widget into light-weight object by Controller. This
 * separates logic of the object depending on where the control for given aspect
 * lies.
 * <p>
 * Implements Configurable interface which is useful for user customization of 
 * internal properties.
 * <p>
 * This is also marker interface. It helps flag objects that act as controllers,
 * even if they dont provide any public control. This way it is more clear whether
 * Object is its own controller, which is also possible. Such cases could be
 * required by some wrappers that require their graphics to have Controllers.
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
//    @Override
    public void setWidget(W w);
    
    /**
     * Returns widget in relationship with this Controller object.
     * @return associated widget or null if none.
     */
//    @Override
    public W getWidget();
    
    /**
     * Guaranteed to execute immediately before widget is closed. Widget is not
     * expected to be used after this method is invoked. Use to free resources.
     * Default implementation does nothing.
     */
    default void OnClosing(){}
}
