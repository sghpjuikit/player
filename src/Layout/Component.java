
package Layout;

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
    

}
