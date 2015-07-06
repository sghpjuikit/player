
package Layout;

import Layout.Widgets.Widget;
import Layout.Widgets.WidgetManager;
import java.util.UUID;
import javafx.scene.Node;
import util.collections.map.PropertyMap;

/**
 * @author uranium
 * 
 * Defines wrapper of loadable graphical component.
 * Basis for wrappers - containers or wrapped widgets.
 */
public abstract class Component {
    
    /** Unique ID. Permanent. Persists application life cycle. */
    public final UUID id = UUID.randomUUID();
    /** Simple storage. Persists application life cycle. */
    public final PropertyMap properties = new PropertyMap();

    
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
