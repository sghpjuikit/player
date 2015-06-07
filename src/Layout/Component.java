
package Layout;

import Layout.Widgets.Widget;
import Layout.Widgets.WidgetManager;
import java.util.UUID;
import javafx.scene.Node;

/**
 * @author uranium
 * 
 * Defines wrapper of loadable graphical component.
 * Basis for wrappers - containers or wrapped widgets.
 */
public abstract class Component {
    final private UUID id = UUID.randomUUID();
    
    /** @return  unique ID. */
    public UUID getID() {
        return id;
    }
    
    /** @return name */
    abstract public String getName();
    
    /**
     * Loads the graphical element this container wraps.
     * @return root node of the loaded container
     */
    abstract public Node load();
    
    /** @return whether this component is currently open*/
    public boolean isOpen() {
        // check if this isnt standalone widget (not in a layout)
        if(WidgetManager.standaloneWidgets.contains(this)) return true;
        
        Component c = this;
        Component p = this;
        do {
            p = c instanceof Widget ? null : ((Container)c).getParent();
            if(p!=null) c = p;
        } while(p!=null);
        
        // top container is always layout
        return c instanceof Layout;
    }
}
