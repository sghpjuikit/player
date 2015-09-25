
package Layout.container.bicontainer;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;

import Layout.Areas.Splitter;
import Layout.Component;
import Layout.container.Container;

import static javafx.geometry.Orientation.VERTICAL;

/**
 * Implementation of {@link Container Container} containing two children.
 * <p>
 * @author uranium
 *
 * Warning: do not use this class.
 * @TODO implement load() properly, currently works only for Containers. Hence
 * the abstract class type to avoid misuse. Do not use this class as non-pure
 * Container. See addChild and the exception.
 */
public class BiContainer extends Container<Splitter> {

    /** Orientation of this container. */
    public final ObjectProperty<Orientation> orientation = new SimpleObjectProperty<>(VERTICAL);
    private final Map<Integer, Component> children = new HashMap<>();


    public BiContainer(Orientation o) {
        orientation.set(o);
    }

    @Override
    public Node load() {
        // lazy load (needed because of the serialization ommiting this field)
        if (ui == null) ui = new Splitter(this);

        ui.setChild1(children.get(1));
        ui.setChild2(children.get(2));

        return ui.getRoot();
    }

    /**
     * @return the children
     */
    @Override
    public Map<Integer, Component> getChildren() {
        return children;
    }

    /**
     * { @inheritDoc }
     * Index can only take on value 1 or 2. Other values will do
     * nothing.
     * @throws UnsupportedOperationException if component not Container type.
     */
    @Override
    public void addChild(Integer index, Component c) {
        if(index == null) return;
        if (index<1 || index>2)
            throw new IndexOutOfBoundsException("Index " + index + " not supported. Only null,1,2 values supported.");

        if(c==null) children.remove(index);
        else children.put(index, c);

        if(ui!=null) ui.setComponent(index, c);
        setParentRec();
    }

    public void switchCildren() {
        Component c1 = children.get(1);
        Component c2 = children.get(2);
        children.clear();
        children.put(1, c2);
        children.put(2, c1);
        load();
    }

    @Override
    public Integer getEmptySpot() {
        if(children.get(1)==null) return 1;
        if(children.get(2)==null) return 2;
        else return null;
    }

    @Override
    public void show() {
//        super.show();
        if(ui!=null) ui.show();
    }

    @Override
    public void hide() {
//        super.hide();
//        if(gui !=null) gui.widgets.values().forEach(Area::hide);
                if(ui!=null) ui.hide();
    }

}