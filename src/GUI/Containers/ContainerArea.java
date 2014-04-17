/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.Containers;

import Layout.Container;
import Layout.Widget;

/**
 * The container - area relationship is the whole for holding widgets and
 * components within the layout. Container makes up for the abstract side, this
 * object represents the gui part.
 * @author uranium
 */
public interface ContainerArea {
    
    /**
     * @return container this area is associated with. The container - area
     * relationship is the whole for holding widgets and components within the
     * layout. Container makes up for the abstract side, this object represents
     * the gui part.
     */
    public Container getContainer();
    /**
     * @return currently active widget.
     */
    public Widget getWidget();
    /**
     * @return true if there is at least one active widget, false otherwise. 
     */
    default public boolean hasWidget() {
        return (getWidget() != null);
    }
    public void loadWidget(Widget w);
    /**
     * Refresh active widget. Refreshes the wrapped widget by calling its
     * refresh() method from its controller. Some widgets might not support
     * this behavior.
     */
    public void refreshWidget();
    public void close();
}
