/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import GUI.ContextManager;
import Layout.WidgetImpl.SimpleConfigurator;
import java.io.File;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Template for fxml based Controller implementation.
 * <p>
 * See {@link Controller} for information about the lifecycle of this object
 * @author uranium
 */
abstract public class FXMLController implements Controller<FXMLWidget> {
    private FXMLWidget widget;
    
    public FXMLController() {
    }

    @Override
    public void setWidget(FXMLWidget w) {
        widget = w;
    }
    /**
     * Returns widget in relationship with this Controller object.
     * @return associated widget or null if none.
     */
    @Override
    public FXMLWidget getWidget() {
        return widget;
    }
    
    /**
     * Initializes the controller. Use as a constructor.
     * <p>
     * If the contorller makes use of the {@link Configuration.IsConfig} anotated properties,
     * they will not be initialized and their values shouldnt be used in this
     * method.
     * <p>
     * Dont invoke this method, it is called automatically at widget's creation.
     * Invoking this method will have no effect.
     */
    abstract public void init();
    
    /**
     * Refreshes the GUI. This method should be used to bring GUI up to date with
     * any changes, like changed {@link Configuration.IsConfig properties}, 
     * outdated layout, etc. This method is responsible that every aspect of the
     * gui that needs refresh will be refreshed.
     * There can be case that widget has nothing to refresh, such implementations
     * will do nothing.
     * <p>
     * This method is called automatically when widget is loaded, after it has been
     * initialized. At this point {@link Configuration.IsConfig properties} of 
     * the widget are available.
     * <p>
     * @developer note: Leave out any refresh related code from initialize
     * methods, constructors, etc... and only make sure that after initialize
     * method is called the object is fully prepared to have this method invoked.
     */
    @Override
    abstract public void refresh();
    
    /** 
     * Returns specified resource file from the widget's location.
     * @param filename of the file with extension. For example: "bgr.jpg"
     */
    public File getResource(String filename) {
        return new File(getWidget().getLocation(),filename);
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
