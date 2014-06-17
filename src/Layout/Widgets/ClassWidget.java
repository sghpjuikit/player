/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets;

import javafx.scene.Node;
import utilities.Log;

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
    private final Class<? extends Node> class_type;
    
    ClassWidget(String name, Class<? extends Node> type) {
        super(name);
        class_type = type;
    }

    @Override
    public Node load() {
        try {
            Node node = (Node) Class.forName(class_type.getName()).newInstance();
            if (node instanceof Controller) {
                rememberConfigs();
                controller = (Controller) node;
                controller.setWidget(this);
                restoreConfigs();
                controller.refresh();
            }            
            return node;
        } catch (ClassCastException | InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            Log.mess("Widget " + name + " failed to load. "+ex.getMessage());
            return Widget.EMPTY().load();
        }
    }
}
