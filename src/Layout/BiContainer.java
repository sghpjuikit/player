
package Layout;

import GUI.Containers.Splitter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.HashMap;
import java.util.Map;
import javafx.geometry.Orientation;
import javafx.scene.Node;

/**
 * @author uranium
 * 
 * Container with two children separated by divider.
 */
public class BiContainer extends Container {
    private final Map<Integer, Component> children = new HashMap<>();
    @XStreamOmitField
    private Splitter gui = new Splitter(this);
    
    public BiContainer(Orientation orientation) {
        properties.set("orient", orientation);
    }
    
    @Override
    public Node load() {
        // lazy load (needed because of the serialization ommiting this field)
        if (gui == null) gui = new Splitter(this);
        if (children.get(1) == null) {
            Container c = new UniContainer(gui.getChild1());
            children.put(1, c);
        }
        if (children.get(2) == null) {
            Container c = new UniContainer(gui.getChild2());
            children.put(2, c);
        }
        
        gui.setChild1(children.get(1));
        gui.setChild2(children.get(2));
        
        return gui.getPane();
    }

    /**
     * @return the children
     */
    @Override
    public Map<Integer, Component> getChildren() {
        return children;
    }
    
    /**
     * Adds the widget as child.
     * Since there is only one child, the index parameter is ignored.
     * @param w widget or container. Null value will clear all children.
     * @param index is can only take 1 or 2 value. other values will do
     * nothing.
     */
    @Override
    public void addChild(int index, Component w) {
        if (!(w instanceof Container)) return;
        if (index<1 || index>2) return;
        ((Container)w).parent = this;
        children.put(index, w);
        load();
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
    public void show() {
        super.show();
        gui.show();
    }
    @Override
    public void hide() {
        super.hide();
        gui.hide();
    }
}