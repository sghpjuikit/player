
package GUI;

import Layout.Component;
import Layout.Container;
import Layout.LayoutManager;
import Layout.Widgets.Widget;
import java.io.Serializable;
import java.util.UUID;

/**
 * Used for drag transfer of components.
 * 
 * It serializes IDs of the component - source of the transfer and its parent
 * container during transfer and provides methods to access them back.
 * @author uranium
 */
public class WidgetTransfer implements Serializable {
    private static final long serialVersionUID = 1L;
    private final UUID widgetID;
    private final UUID containerID;
    
    public WidgetTransfer(Component widget, Container container) {
        this.widgetID = widget.getID();
        this.containerID = container.getID();
    }
    
    /**
     * @return the parent container of the component requesting transfer or null
     * in case of error, when the container can not be found, new empty will be
     * provided.
     */
    public Container getContainer() {
        return LayoutManager.getLayouts().flatMap(l->l.getAllContainers().stream())
                .filter(w->w.getID().equals(containerID))
                .findAny()
                .orElse(null);
    }
    
    /**
     * In case of error, when the widget can not be found, new empty will be
     * provided.
     * @return the component requesting the transfer
     */
    public Component getWidget() {
        return LayoutManager.getLayouts().flatMap(l->l.getAllChildren().stream())
                .filter(w->w.getID().equals(widgetID))
                .findAny()
                .orElse(Widget.EMPTY());
    }
}
