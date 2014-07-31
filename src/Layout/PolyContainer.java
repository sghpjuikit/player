
package Layout;

import Layout.Areas.ContainerNode;
import Layout.Areas.TabArea;
import Layout.Widgets.Controller;
import Layout.Widgets.Widget;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

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
    public ContainerNode getGraphics() {
        return gui;
    }
    
    /** {@inheritDoc} */
    @Override
    public Node load() {
        // lazy-init gui
        if (gui == null) gui = new TabArea(this);
        
        // load gui
        root.getChildren().setAll(gui.getRoot());
        AnchorPane.setBottomAnchor(gui.getRoot(), 0.0);
        AnchorPane.setLeftAnchor(gui.getRoot(), 0.0);
        AnchorPane.setRightAnchor(gui.getRoot(), 0.0);
        AnchorPane.setTopAnchor(gui.getRoot(), 0.0);
        
        // we need to initialize the tabs
            // grab a copy og the children
        Map<Integer,Component> cs = new HashMap();
        cs.putAll(children);
            // prevent selection change in illegal state
        gui.selectComponentPreventLoad(true);
            // remove children from layout graph & scene graph
        gui.removeAllComponents();
        children.clear();
            // reinitialize
        gui.addComponents(cs.values());
            // reload old selection
        gui.selectComponent(properties.getI("selected"));
        
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
            if(rem instanceof Widget) {
                Controller con = Widget.class.cast(rem).getController();System.out.println("closing previous " + index + " " + con);
                if(con!=null) con.OnClosing();
            }
            // remove from layout graph
            children.remove(index);
            if(c instanceof Container)
            // remove from scene graph
            gui.removeComponent(c);
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
     * @param from
     * @param to 
     */
    public void moveChild(int from, int to) {
        // avoid pointless operation
        if(from==to || children.size()<2) return;
        
        // we will reorder children and then reload similar like in load() method
            // grab a copy of the children
        List<Component> old =  new ArrayList();
                        old.addAll(children.values());
            // get component to move return if nothing to do
        Component c = old.get(from);
        if (c==null) return;
            // reorder
        old.remove(c);
        old.add(to, c);
            // change selection if selected child is moving
        if(from==properties.getI("selected")) properties.set("selected", to);
            // prevent selection change in illegal state
        gui.selectComponentPreventLoad(true);
            // remove children from layout graph & scene graph
        gui.removeAllComponents();
        children.clear();
            // reinitialize
        gui.addComponents(old);
            // reload old selection
        gui.selectComponent(properties.getI("selected"));
    }
    
}
