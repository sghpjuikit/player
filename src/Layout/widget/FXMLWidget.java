/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget;

import java.io.File;
import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import Layout.widget.controller.FXMLController;

/**
 * Widget based on .fxml file and {@link FXMLController}.
 * <p>
 * Widget is loaded from its location. It adopts the .fxml + controller pattern.
 * 
 * @see FXMLWidgetFactory
 * @author uranium
 */
public final class FXMLWidget extends Widget<FXMLController> {
    
    FXMLWidget(String name, WidgetFactory factory) {
        super(name,factory);
    }

    @Override
    protected Node loadInitial() throws IOException {
        // load controller graphics
        FXMLLoader loader = new FXMLLoader();
                   loader.setLocation(getFactory().url);
                   loader.setController(controller);
        Node n = loader.load();

        controller.init();
        restoreConfigs();
        controller.refresh();

        return n;
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