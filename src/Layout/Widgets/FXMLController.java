/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import java.io.File;
import static java.util.Objects.nonNull;

/**
 * Template for fxml based Controller implementation.
 * 
 * @author uranium
 */
abstract public class FXMLController implements Controller<FXMLWidget> {
    
    private FXMLWidget widget;
    
    public FXMLController() {}
    
    /** {@inheritDoc} */
    @Override
    public void setWidget(FXMLWidget w) {
        if (nonNull(widget)) throw new AssertionError("Controller already"
                + " has assigned widget. The relationship should be final.");
        widget = w;
    }
    
    /** {@inheritDoc} */
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
    
    /** {@inheritDoc} */
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
