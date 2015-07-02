/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.controller;

import Layout.Widgets.FXMLWidget;
import Layout.Widgets.controller.io.Inputs;
import Layout.Widgets.controller.io.Outputs;
import java.io.File;
import java.net.MalformedURLException;
import javafx.scene.layout.Pane;
import static util.dev.Util.require;

/**
 * Controller for {@link FXMLWidget}
 * 
 * @author uranium
 */
abstract public class FXMLController implements Controller<FXMLWidget> {
    
    private FXMLWidget widget;
    public final Outputs outputs = new Outputs();
    public final Inputs inputs = new Inputs();
    
    /** {@inheritDoc} */
    @Override
    public void setWidget(FXMLWidget w) {
        require(widget==null);
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
    
    public void loadSkin(String filename, Pane root) {
        try {
            root.getStylesheets().add(getResource(filename).toURI().toURL().toString());
        } catch (MalformedURLException ex) {
        
        }
    }

    /** {@inheritDoc} */
    @Override
    public Outputs getOutputs() {
        return outputs;
    }
    
    /** {@inheritDoc} */
    @Override
    public Inputs getInputs() {
        return inputs;
    }
    
}
