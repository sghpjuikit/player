
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
 * the abstract class type to avoid misuse. Do not use this class as non-pure
 * Container. See addChild and the exception.
 */
abstract public class BiContainer extends Container {
    
    private final Map<Integer, Component> children = new HashMap();
    @XStreamOmitField
    Splitter graphics;
    
    public BiContainer(Orientation orientation) {
        properties.set("orient", orientation);
    }
    
    @Override
    public Splitter getGraphics() {
        return graphics;
    }
    
    @Override
    public Node load() {
        // lazy load (needed because of the serialization ommiting this field)
        if (graphics == null) graphics = new Splitter(this);
        
        if (children.get(1) == null) {
            Container c = new UniContainer(graphics.getChild1Pane());
            children.put(1, c);
        }
        if (children.get(2) == null) {
            Container c = new UniContainer(graphics.getChild2Pane());
            children.put(2, c);
        }
        
        graphics.setChild1(children.get(1));
        graphics.setChild2(children.get(2));
        
        return graphics.getRoot();
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
    public void addChild(Integer index, Component c) {System.out.println("bicont " + index + " " + c);System.out.println(this);
        if(index == null) return;
        
        if (index<1 || index>2)
            throw new IndexOutOfBoundsException("Index " + index + " not supported. Only null,1,2 values supported.");
        
        if(c!=null) {
            if(!(c instanceof Container)) 
                throw new UnsupportedOperationException("Non containers currently not supported");
            Container.class.cast(c).parent = this;
        }
        children.put(index, c);
        load();
        initialize();
    }
    
    public void switchCildren() {
        Component c1 = children.get(1);
        Component c2 = children.get(2);
        children.clear();
        children.put(1, c2);
        children.put(2, c1);
        load();
    }
}