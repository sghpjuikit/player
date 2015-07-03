/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import Layout.Widgets.controller.Controller;
import javafx.scene.Node;
import util.dev.Log;

/**
 * Generic widget object. Used for widgets loaded dynamically by instantiating
 * their object's class. Practically speaking, this class wraps any desired Node
 * into Widget interface so it can be recognized by application as a widget.
 * This is done by providing the Node's class info which is then instantiated
 * during loading. Note that the wrapped object must also implement Controller
 * interface or extend one of its implementations.
 * 
 * @author uranium
 */
public class ClassWidget extends Widget<Controller> {
    
    ClassWidget(String name, ClassWidgetFactory factory) {
        super(name,factory);
    }

    @Override
    public Node loadInitial() {                    
        try {
            Node node = (Node) getFactory().getControllerClass()
                        .getDeclaredConstructor(ClassWidget.class)
                        .newInstance(this);
                
            rememberConfigs();
            controller = Controller.class.cast(node);
            
            restoreConfigs();
            controller.refresh();
            
            return node;
        } catch (Exception e) {
            Log.err("Widget " + name + " failed to load. " + e.getMessage());
            return Widget.EMPTY().load();
        } 
    }
    
}
