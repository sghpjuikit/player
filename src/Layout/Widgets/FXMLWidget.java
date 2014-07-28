/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.io.File;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import utilities.Log;

/**
 * Generic widget based on .fxml file. 
 * <p>
 * This class wraps any desired .fxml file denoted by its location into widget
 * so it can be recognized by the application as a component for layout.
 * <p>
 * Widget is loaded dynamically from its location. It adopts the standard fxml +
 * controller pattern. The controller object must however in addition implement 
 * {@link FXMLController} interface.
 * More on the creation process of this widget: {@link FXMLWidgetFactory}
 * <p>
 * @author uranium
 */
public final class FXMLWidget extends Widget<FXMLController> {
    
    @XStreamOmitField
    private Node root;  // cache loaded root to avoid loading more than once
    
    FXMLWidget(String name) {
        super(name);
    }

    @Override
    public Node load() {
        try {
            // if widget has already loaded once, return
            // 1 attaching root to the scenegraph will automatically remove it
            //   from its old location
            // 2 this guarantees that widget loads only once, which means:
            //   - graphics will be constructed only once
            //   - -||- controller, controller will always be in control of
            //     the correct graphics - normally we would have to load both
            //     graphics and controller multiple times because we can not
            //     assign new graphics to old controller
            // 3 entire state of the widget is intact with the exception of
            //   initial load at deserialisation.
            //   This also makes deserialisation the only time when configs
            //   need to be taken care of manually
            if(root!=null) return root;
            
            // else load initially
            
            if(controller!=null) {
                rememberConfigs();
                controller.OnClosing();
            }
            // get controller instance
            controller = getFactory().instantiateController();
            controller.setWidget(this);
            
            FXMLLoader loader = new FXMLLoader();
                       loader.setLocation(getFactory().url);
                       loader.setController(controller);
            root = loader.load();
            
            controller.init();
            restoreConfigs();
            controller.refresh();
            
            return root;
        } catch (IOException ex) {ex.printStackTrace();
            Log.err("Widget " + name + " failed to load. " + ex.getMessage() );
            // inject empty content to prevent application crash
            return Widget.EMPTY().load();
        }
    }

    @Override
    public FXMLWidgetFactory getFactory() {
        return (FXMLWidgetFactory) super.getFactory();
    }
    
    /** Returns location of the widget's parent directory. Never null. */
    public File getLocation() {
        // we need to manually fix encoding so spaces in names dont cause errors
        String s = getFactory().url.getPath().replace("%20"," ");
        return new File(s).getParentFile();
    }

}