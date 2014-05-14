/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import Configuration.Config;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import utilities.Log;

/**
 * Generic widget based on .fxml file. 
 * <p>
 * This class wraps any desired .fxml file denoted by its location into widget
 * so it can be recognized by application as a component for layout.
 * <p>
 * Widget is loaded dynamically from its location. It adopts the standard fxml +
 * controller pattern. The controller object must however implement {@link Controller}
 * interface.
 * More on the creation process of this widget: {@link FXMLWidgetFactory}
 * <p>
 * @author uranium
 */
public final class FXMLWidget extends Widget<FXMLController> {
    /** 
     * URL pointing towards .fxml file of this widget. Also plays role in
     * serialization as it is determines the type of widget.
     */
    private final URL url;
    
    /**
     * @param name of the widget. Permanent.
     * @param aResource {@see #url}
     */
    FXMLWidget(String name, URL aResource) {
        super(name);
        url = aResource;
    }

    @Override
    public Node load() {
        try {
            controller = (FXMLController) getFactory().instantiateController();
            controller.setWidget(this);
            
            FXMLLoader loader = new FXMLLoader();
                       loader.setLocation(url);
                       loader.setController(controller);
            
            Node node = (Node) loader.load();

            // apply settings
            configs.forEach((Config c) -> { // dont shorten this it causes some errors
                setField(c.name, c.value);
            });
            configs.clear();
            
            controller.refresh();
            return node;
        } catch (IOException ex) {ex.printStackTrace();
            Log.mess("Widget " + name + " failed to load. " + ex.getMessage() );
            return null;
        }
    }

    @Override
    public FXMLWidgetFactory getFactory() {
        return (FXMLWidgetFactory) super.getFactory();
    }
    
    /** Returns location of the widget's parent directory. Never null. */
    public File getLocation() {
        return new File(url.getFile()).getParentFile();
    }

}