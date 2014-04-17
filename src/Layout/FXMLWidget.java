/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout;

import Configuration.Config;
import Configuration.Configuration;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import utilities.FileUtil;
import utilities.Log;

/**
 * Generic widget object. Used for widgets loaded dynamically from their
 * respective .fxml files. Practically speaking, this class wraps any desired
 * .fxml file into Widget interface so it can be recognized by application as a
 * widget.
 * @author uranium
 */
public final class FXMLWidget extends Widget {
    private final URL fxmlUrl;
    
    FXMLWidget(String aName, URL aResource) {
        name = aName;
        fxmlUrl = aResource;
    }

    @Override
    public Node load() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(fxmlUrl);
            
            //******************************************************************
            // load controlelr separately ( dunno why it fails to load on its own
            URL dir = new File(Configuration.WIDGET_FOLDER).toURI().toURL();
            URL[] urls = new URL[1];
                  urls[0] = dir;
            URLClassLoader controllerLoader = new URLClassLoader(urls);
            Class cn;
            Object cntrl = null;
            
            try {
                String controllerName = FileUtil.getName(fxmlUrl.toURI()) + "Controller";
                cn = controllerLoader.loadClass(controllerName);
                cntrl = cn.newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | URISyntaxException ex) {
                Logger.getLogger(FXMLWidget.class.getName()).log(Level.SEVERE, null, ex);
            }
        
            loader.setController(cntrl);
            
            //******************************************************************
            
            Node node = (Node) loader.load();
            controller = (Controller) loader.getController();
            controller.setWidget(this);
            
            // apply settings
            configs.forEach((Config c) -> { // dont shorthen this it causes some errors
                setField(c.name, c.value);
            });
            configs.clear();
            
            controller.refresh();
            return node;
        } catch (IOException ex) {
            Log.mess("Widget " + name + " failed to load. " + ex.getMessage() );
            return null;
        }
    }
}
