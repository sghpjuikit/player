
package Layout;

import Layout.Containers.Splitter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.HashMap;
import java.util.Map;
import javafx.geometry.Orientation;
import javafx.scene.Node;

/**
 * Implementation of {@link Container Container} containing two children.
 * <p>
 * @author uranium
 * 
 * Warning: do not use this class.
 * @TODO implement load() properly, currently works only for Containers. Hence
 * the abstract identifier to avoid misuse. Do not use this class as non-pure
 * Container. See addChild and the exception.
 */
abstract public class BiContainer extends Container {
    private final Map<Integer, Component> children = new HashMap<>();
    @XStreamOmitField
    protected Splitter gui;
    
    public BiContainer(Orientation orientation) {
        properties.set("orient", orientation);
    }
    
    @Override
    public Node load() {
        // lazy load (needed because of the serialization ommiting this field)
        if (gui == null) gui = new Splitter(this);
        
        if (children.get(1) == null) {
            Container c = new UniContainer(gui.getChild1Pane());
            children.put(1, c);
        }
        if (children.get(2) == null) {
            Container c = new UniContainer(gui.getChild2Pane());
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
     * { @inheritDoc }
     * Index can only take on value 1 or 2. Other values will do
     * nothing.
     * @throws UnsupportedOperationException if component not Container type.
     */
    @Override
    public void addChild(Integer index, Component w) {
        if(index == null) return;
        if (index<1 || index>2) return;
        
        if(!(w instanceof Container)) throw new UnsupportedOperationException("Non containers currently not supported");
        
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