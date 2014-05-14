/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import utilities.FileUtil;
import utilities.Log;

/**
 * {@link WidgetFactory} producing {@link FXMLWidget}.
 * <p>
 * This factory is able to create the widgets dynamically from external fxml
 * files and compiled classes of their respective controller objects. This
 * requires a standard where controller object's class name must be the same as
 * its class file and the name must be derived from the .fxml file name (without
 * extnsion of course) exactly by concatenating the word 'Controller' to it.
 * <p>
 * For example Tagger.fxml would require TaggerController.class file of the
 * compiled controller.
 * <p>
 * @author uranium
 */
public final class FXMLWidgetFactory extends WidgetFactory<FXMLWidget> {
    
    URL url;
    private Class<? extends Controller> controlelr_type;
    
    /**
     * @param _name
     * @param resource
     */
    public FXMLWidgetFactory(String _name, URL resource) {
        super(_name, instantiateController(resource).getClass());
        url = resource;
    }
    
    @Override
    public FXMLWidget create() {
        FXMLWidget w = new FXMLWidget(getName(), url);
        return w;
    }
    
    /** 
     * Instantiates the controller object for the widget this factory produces. 
     * The instantiation involves a lookup of the correct class file. For this
     * reason the Controller class name and the .fxml file must adhere to proper
     * standard.
     */
    Controller instantiateController() {
        return instantiateController(url);
    }
    
    private static FXMLController instantiateController(URL url) {
            try {
                URL dir = new File(url.toURI()).getParentFile().toURI().toURL();
                URL[] urls = new URL[1];
                      urls[0] = dir;
                URLClassLoader controllerLoader = new URLClassLoader(urls);
                Class cn;
                Object cntrl = null;
                
                // widget name eg.: "Tagger"
                String wname = FileUtil.getName(url.toURI());
                // parant folder name eg. : TaggerWidget
                String fname = new File(url.toURI()).getParentFile().getName();
                // controller class name eg.: "TaggerWidget.TaggerController
                String controllerName = fname + "." + wname + "Controller";
                
                // instantiate the controller
                cn = controllerLoader.loadClass(controllerName);
                return (FXMLController) cn.newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | MalformedURLException | URISyntaxException ex) {
                Log.err("Controller instantiation failed. " + ex.getMessage());
                return null;
            }
    }
}
