
package Layout;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import Layout.Areas.Splitter;

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
public class BiContainer extends Container {

    private final Map<Integer, Component> children = new HashMap();
    @XStreamOmitField
    Splitter ui;

    /** Orientation of this container. */
    public final ObjectProperty<Orientation> orientation = new SimpleObjectProperty(VERTICAL);


    public BiContainer(Orientation o) {
        orientation.set(o);
    }

    @Override
    public Splitter getGraphics() {
        return ui;
    }

    @Override
    public Node load() {
        // lazy load (needed because of the serialization ommiting this field)
        if (ui == null) ui = new Splitter(this);

        if (children.get(1) == null) {
            Container c = new UniContainer(ui.getChild1Pane());
            children.put(1, c);
        }
        if (children.get(2) == null) {
            Container c = new UniContainer(ui.getChild2Pane());
            children.put(2, c);
        }

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

        if(c instanceof Container)
            Container.class.cast(c).setParent(this);

        children.put(index, c);
        load();
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
        // WTH does this not work
//        return (children.get(1)==null) ? 1 : (children.get(2)==null) ? 2 : null;
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