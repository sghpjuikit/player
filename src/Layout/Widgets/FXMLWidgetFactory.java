/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import Layout.Widgets.controller.FXMLController;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import util.File.FileUtil;

/**
 * {@link WidgetFactory} producing {@link FXMLWidget}.
 * <p>
 * This factory is able to create the widgets dynamically from external fxml
 * files and compiled classes of their respective controller objects.
 * <p>
 * This requires a standard where controller object's class name must be the same as
 * its class file and the name must be derived from the .fxml file name (without
 * extension of course) exactly by concatenating the word 'Controller' to it.
 * <p>
 * For example Tagger.fxml would require TaggerController.class file of the
 * compiled controller.
 * <p>
 * Note that the resource (the fxml file) is not directly passed onto the widget.
 * Instead the widget requests it from its (this) factory. This avoids problems
 * with the resource being a strong dependency that could prevent widget loading
 * after the application has been moved or the widget resource is no longer
 * available.
 * <p>
 * @author uranium
 */
public final class FXMLWidgetFactory extends WidgetFactory<FXMLWidget> {
    
    URL url;
    
    /**
     * @param _name
     * @param resource
     */
    public FXMLWidgetFactory(String _name, URL resource) {
        super(_name, obtainControllerClass(resource));
        url = resource;
    }
    
    
    /** {@inheritDoc} */
    @Override
    public FXMLWidget create() {
        FXMLWidget w = new FXMLWidget(name,this);
        return w;
    }
    
    /** 
     * Instantiates the controller object for the widget this factory produces. 
     * The instantiation involves a lookup of the correct class file. For this
     * reason the Controller class name and the .fxml file must adhere to proper
     * standard.
     */
    FXMLController instantiateController() {
        try {
            return (FXMLController) getControllerClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Controller instantiation failed. ", e);
        }
    }
    
    private static Class obtainControllerClass(URL url) {
        
        try {
            URL dir = new File(url.toURI()).getParentFile().toURI().toURL();
            URL[] urls = new URL[1];
                  urls[0] = dir;
            URLClassLoader controllerLoader = new URLClassLoader(urls);

            // widget name eg.: "Tagger"
            String wname = FileUtil.getName(url.toURI());
            // parant folder name eg. : TaggerWidget
            String fname = new File(url.toURI()).getParentFile().getName();
            // controller class name eg.: "TaggerWidget.TaggerController
            String controllerName = fname + "." + wname + "Controller";

            // instantiate the controller
            return controllerLoader.loadClass(controllerName);
        } catch (ClassNotFoundException | MalformedURLException | URISyntaxException e) {
            throw new RuntimeException("Controller class loading failed from: " + url + ". ", e);
        }
    }
}
