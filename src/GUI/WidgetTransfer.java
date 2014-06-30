
package GUI;

import Layout.Component;
import Layout.Container;
import Layout.LayoutManager;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Used for drag transfer of components.
 * 
 * It serializes IDs of the component - source of the transfer and its parent
 * container during transfer and provides methods to access them back.
 * @author uranium
 */
public class WidgetTransfer implements Serializable {
    private static final long serialVersionUID = 11L;
    
    private final UUID widgetID;
    private final UUID containerID;
    
    public WidgetTransfer(Component widget, Container container) {
        Objects.requireNonNull(widget);
        Objects.requireNonNull(container);
        this.widgetID = widget.getID();
        this.containerID = container.getID();
    }
    
    /**
     * @return the parent container of the component, never null.
     * @throws RuntimeException if container will not be found. Happens only in
     * case of internal logic error and should never happen. because this method
     * is likely to be executed during drag operations thorn exceptions might
     * not be displayed and substituted with a javaFX drag and drop error output
     * which unfortunately hides the specifics of the exception.
     */
    public Container getContainer() {
        return LayoutManager.getLayouts().flatMap(l->l.getAllContainers().stream())
                .filter(c->containerID.equals(c.getID()))
                .findAny()
                .orElseThrow(()-> new RuntimeException("Widget not found"));
    }
    
    /**
     * @return the component requesting the transfer never null
     * @throws RuntimeException if container will not be found. Happens only in
     * case of internal logic error and should never happen. because this method
     * is likely to be executed during drag operations thorn exceptions might
     * not be displayed and substituted with a javaFX drag and drop error output
     * which unfortunately hides the specifics of the exception.
     */
    public Component getWidget() {
        return LayoutManager.getLayouts().flatMap(l->l.getAllChildren().stream())
                .filter(w->widgetID.equals(w.getID()))
                .findAny()
                .orElseThrow(()-> new RuntimeException("Widget not found"));
    }

    @Override
    public String toString() {
        return widgetID + " " + containerID;
    }
    
    
}
