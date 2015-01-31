/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout;

import Layout.Areas.ContainerNode;
import Layout.Areas.FreeFormArea;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.Node;

/**
 */
public class FreeFormContainer extends Container {
    
    private final Map<Integer,Component> children = new HashMap();
    @XStreamOmitField
    private FreeFormArea gui;
    

    @Override
    public Map<Integer, Component> getChildren() {
        return children;
    }

    @Override
    public void addChild(Integer index, Component c) {
        if(index == null) return;
                
        if(c==null) {
            throw  new IllegalArgumentException("Removing null child");
        } else {
            if(c instanceof Container) {
                Container.class.cast(c).parent = this;
            } else throw new UnsupportedOperationException("Non containers currently not supported");
        }
        
        children.put(index, c);
        load();
        initialize();
    }

    @Override
    public void removeChild(Integer index) {
        gui.removeWindow(index);
        children.remove(index);
    }
    

    @Override
    public Integer getEmptySpot() {
        return null;
    }

    @Override
    public Node load() {
        if(gui==null) gui = new FreeFormArea(this);
        
        gui.load();
        
        return gui.getRoot();
    }

    @Override
    public ContainerNode getGraphics() {
        return gui;
    }
    
}
