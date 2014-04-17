
package Layout;

import Configuration.Configurable;

/**
 * Controller defines object defining behavior of some graphical object and acts
 * as a bridge between that object and the rest of application. Controllers are
 * used to control and define behavior for example of fxml graphics and turn
 * unchanging object into controllable.
 * 
 * Encapsulation is still recommended and can be achieved by not providing internal
 * behavior as public. Only public methods and fields are visible from outside.
 * 
 * Full controller implementation can be considered behavior defining object and
 * also API of the object to allow controlled control of from outside.
 * Controller can make object fully standalone or give certain degree of control
 * somewhere else outside.
 * 
 * Controller is also communication channel between the object and its enviroment
 * capable of providing necessary information handled somewhere else. An example
 * could be properties serialization which can be handled somewhere else (like
 * Widget) and linked to the object by Controller.
 * 
 * This class implements Configurable interface which is useful for user
 * customization of internal properties.
 * 
 * This is also marker interface. It helps flag objects that act as controllers,
 * even if they dont provide any public control. This way it is more clear whether
 * Object is its own controller, which is also possible. Such cases could be
 * required by other wrapper classes (like Widget) that require their graphics
 * to have Controllers.
 * 
 * To see some more check the implementations.
 * 
 * @author uranium
 */
public interface Controller extends Configurable{
    
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
    public void setWidget(Widget w);
    /**
     * Returns widget in relationship with this Controller object.
     * @return associated widget or null if none.
     */
    public Widget getWidget();
}
