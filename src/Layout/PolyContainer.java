
package Layout;

import Layout.Areas.TabArea;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import util.TODO;

/**
 * Implementation of {@link Container Container} containing multiple components.
 * <p>
 * @author uranium
 */
public final class PolyContainer extends Container {
    
    private final Map<Integer, Component> children = new HashMap<>();
    @XStreamOmitField
    private TabArea gui;
    
    @Override
    public TabArea getGraphics() {
        return gui;
    }
    
    /** {@inheritDoc} */
    @Override
    public Node load() {
        // lazy-init gui
        if (gui == null) {
            gui = new TabArea(this);
        }
        
        // load gui
        root.getChildren().setAll(gui.getRoot());
        AnchorPane.setBottomAnchor(gui.getRoot(), 0.0);
        AnchorPane.setLeftAnchor(gui.getRoot(), 0.0);
        AnchorPane.setRightAnchor(gui.getRoot(), 0.0);
        AnchorPane.setTopAnchor(gui.getRoot(), 0.0);
        
        // we need to initialize the tabs
        // no need to take car eof selection, since we do not change it
        // it is restored in gui.addComponents()
            // grab a copy og the children
        Map<Integer,Component> cs = new HashMap();
        cs.putAll(children);
        
//        gui.selectComponentPreventLoad(true);
            // remove children from layout graph & scene graph
        gui.removeAllComponents();
        children.clear();
            // reinitialize
        gui.addComponents(cs.values());
//        gui.selectComponent(properties.getI("selected"));
        
        return gui.getRoot();
    }

    /** {@inheritDoc} */
    @Override
    public void addChild(Integer index, Component c) {
        if(index==null) return;
        // empty child at index
        if(c==null) {
            Component rem = children.get(index);
            // close child
//            if(rem instanceof Widget) {
//                Controller con = Widget.class.cast(rem).getController();
//                if(con!=null) con.OnClosing();
//            }
            // remove from layout graph
            children.remove(index);
//            if(c instanceof Container)
            // remove from scene graph
            gui.removeComponent(rem);
        // add child at new at index
        } else {
            if (c instanceof Container) Container.class.cast(c).parent = this;
            children.put(index, c);
            gui.add(c);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public Map<Integer, Component> getChildren() {
        return children;
    }
    
    /**
     * Change index of the child at specified index to some other index. This 
     * will reposition the child within the area.
     * @param from old index
     * @param to new index
     */
    public void moveChild(int from, int to) {
        // avoid pointless operation
        // there are actually two positions that imply no change
        // each tab has two such indexes - to the left and to the right
        // they are: old_index and old_index+1
        if(from==to || from+1==to || children.size()<1) return;
        
        // get component to move return if nothing to do
        Component c = children.get(from);
        if (c==null) return;
        
        // we will reorder children and reflect in gui without loading
            // make sure there are no index gaps by turning children to list 
        List<Component> newOrder =  new ArrayList();
                        newOrder.addAll(children.values());
            // reorder
        newOrder.remove(c);
            // the removal might change the index, calculate new one
        int i = to<=from ? to : to-1;
        newOrder.add(i, c);
            // reflect new order in children
        children.clear();
        for(int j=0; j<newOrder.size(); j++) children.put(j, newOrder.get(j));
            // reflect in gui
        gui.moveTab(from, to);
    }
    
    @Override
    @TODO(purpose = TODO.Purpose.UNIMPLEMENTED)
    public Integer getEmptySpot() {
        return null;
    }
}
