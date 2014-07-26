
package GUI;

import Layout.Container;
import Layout.LayoutManager;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 * Used for drag transfer of components.
 * <p>
 * Serializes the index of the component within its parent and its parents ID 
 * and provides methods to access them .
 * 
 * @author uranium
 */
@Immutable
public class WidgetTransfer implements Serializable {
    private static final long serialVersionUID = 11L;
    
    private final int widgetIndex;
    private final UUID containerID;
    
    public WidgetTransfer(int childIndex, Container container) {
        if(childIndex<0) throw new RuntimeException("childindex must be non negative");
        Objects.requireNonNull(container);
        this.widgetIndex = childIndex;
        this.containerID = container.getID();
    }
    
    /**
     * @return the parent container of the transferred component, never null.
     * @throws RuntimeException if container can not be found. Happens only in
     * case of internal logic error and should never happen.
     * <p>
     * Caution: Because this method is to be executed during drag operations 
     * thrown exceptions might not be displayed and instead can be substituted 
     * with a javaFX drag and drop error output, which unfortunately hides the
     * specifics of the exception and can lead to troublesome bugs.
     */
    public Container getContainer() {
        return LayoutManager.getLayouts().flatMap(l->l.getAllContainers())
                .filter(c->containerID.equals(c.getID()))
                .findAny()
                .orElseThrow(()-> new RuntimeException("Widget not found"));
    }
    
    /**
     * Access the transferred component through this index and its parent.
     * @return the index of the child of the transferred container
     */
    public int childIndex() {
        return widgetIndex;
    }

    /** Use for debug. Prints content. */
    @Override
    public String toString() {
        return widgetIndex + " " + containerID;
    }
    
}