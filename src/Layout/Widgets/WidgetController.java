/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import GUI.Components.SimpleConfigurator;
import GUI.ContextManager;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Template for any Controller implementation.
 * 
 * @author uranium
 */
abstract public class WidgetController implements Controller{
    private Widget widget;
    

    @Override
    public void setWidget(Widget w) {
        widget = w;
    }
    /**
     * Returns widget in relationship with this Controller object.
     * @return associated widget or null if none.
     */
    @Override
    public Widget getWidget() {
        return widget;
    }
    
    /**
     * Initializes the controller.
     * Dont invoke this method, it is called automatically at widget's creation.
     * Invoking this method will have no effect.
     */
    abstract public void initialize();
    
    /**
     * Refreshes the GUI. This method should be used to bring GUI up to date with
     * any changes, like changed properties, outdated layout, etc.
     * Implementations of this method should reassure that every part of the gui
     * that can be outdated will be refreshed.
     * There can be case that widget has nothing to refresh, such implementations
     * will do nothing.
     * 
     * This method is called automatically when widget is loaded, after it has been
     * initialized. Therefore, leave out any refresh related code from initialize
     * methods, constructors, etc... and only make sure that once the construction
     * of the widget is done, it is fully prepared to have this method invoked.
     */
    @Override
    abstract public void refresh();
    
    public String getPath() {
        throw new UnsupportedOperationException("not supported yet");
    }
    
    /**
     * This method installs configuration/settings gui implementation for this
     * widget. In this context, installing means injecting show settings panel
     * behavior into specified Node object's onMouseRightButtonClicked() method.
     * 
     * The settings will reflect current properties of the widget and 
     * allow them to be changed by user, which will automatically call this
     * controller's refresh() method.
     * Its important to implement refresh() method sufficiently so it will reflect
     * all possible changes.
     * 
     * Also, the introspection of the widget's properties makes it possible to
     * discover only those properties that have been initialized. But they are
     * initialized lazily.
     * Therefore, it is recommended practice to manually initialize all properties.
     * 
     * It is possible to use this method to install the settings to any node in
     * the application.
     * @param node 
     */
    public void installSettings(Node node) {
        installSettings(node, MouseButton.SECONDARY);
    }
    /**
     * More parametrized version of previous method.
     * @param node
     * @param button button that activates show settings behavior on mouse click
     */
    public void installSettings(Node node, MouseButton button) {
        node.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            if (event.getButton().equals(button))
                ContextManager.openFloatingWindow(new SimpleConfigurator(widget, () -> refresh()).getPane(), "Settings");
        });
    }
    
}
