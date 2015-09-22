
package Layout.container.bicontainer;

import javafx.geometry.Orientation;

import Layout.Component;
import Layout.container.Container;

/**
 * Pure container implementation of {@link BiContainer BiContainer}.
 * <p>
 * This implementation can not handle non-Container children and instead
 * delegates their handling by wrapping them into {@link UniContainer Unicontainer}
 * <p>
 * @author uranium
 */
public class BiContainerPure extends BiContainer {

    public BiContainerPure(Orientation orientation) {
        super(orientation);
    }

    /**
     * Adds the widget as child.
     * Since there is only one child, the index parameter is ignored.
     * @param c widget or container. Null value will clear all children.
     * @param index is can only take 1 or 2 value. other values will do
     * nothing.
     */
    @Override
    public void addChild(Integer index, Component c) {
        if(index == null) return;

        if (index<1 || index>2)
            throw new IndexOutOfBoundsException("Index " + index + " not supported. Only null,1,2 values supported.");

        // wrap if component is widget
        if(!(c instanceof Container))
            getChildren().put(index, null);
        else getChildren().put(index, c);
        load();
        setParentRec();
    }
}