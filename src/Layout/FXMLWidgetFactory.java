/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import utilities.Log;

/**
 *
 * @author uranium
 */
public class FXMLWidgetFactory extends WidgetFactory {
    URL resourceU;
    String resourceS;
    
    /**
     * For Class based widgets Class type must extend Node.
     *
     * @param _name
     * @param resource
     */
    public FXMLWidgetFactory(String _name, String resource) {
        super(_name);
        resourceS = resource;
        initInfo();
    }
    /**
     * For Class based widgets Class type must extend Node.
     *
     * @param _name
     * @param resource
     */
    public FXMLWidgetFactory(String _name, URL resource) {
        super(_name);
        resourceU = resource;
        initInfo();
    }
    
    @Override
    public Widget create() {
        try {
            Widget w = new FXMLWidget(getName(), new File(resourceS).toURI().toURL());
            return w;
        } catch (MalformedURLException ex) {
            Log.mess("Widget "+name+" not created. Malformed url : "+resourceS);
            return null;
        }
    }
    
    private void initInfo() {
        Widget  tmp = create();
                tmp.load();
        initInfo(tmp);
                tmp = null;
    }
}
