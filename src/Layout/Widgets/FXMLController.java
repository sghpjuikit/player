/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import java.io.File;

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
     * Refreshes the GUI. This method should be used to refresh state of the
     * controller - bring GUI up to date with
     * any changes, like changed {@link Configuration.IsConfig configs}, 
     * outdated layout, etc. This method is responsible that every aspect of the
     * gui that needs refresh will be refreshed.
     * There can be case that widget has nothing to refresh, such implementations
     * could do nothing.
     * <p>
     * This method is called automatically when widget is loaded, after it has been
     * initialized. At this point {@link Configuration.IsConfig configs} of 
     * the widget are available and have their values set.
     * This method is also called when any config changes value.
     * <p>
     * developer note: Leave out any refresh related code from initialize
     * methods, constructors, etc... and only make sure that after initialize
     * method is called the object is fully prepared to have this method invoked.
     * <p>
     * Generally, put scene graph building to {@link #init()} and use this one
     * for state refresh.
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
    
}
