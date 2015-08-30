/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import Layout.Areas.Area;
import Layout.Areas.ContainerNode;
import Layout.Areas.FreeFormArea;

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
            
        } else {
            if(c instanceof Container) {
                Container.class.cast(c).setParent(this);
            }
        }
        
        children.put(index, c);
        gui.loadWindow(index, c);
        setParentRec();
    }

    @Override
    public void removeChild(Integer index) {
        gui.closeWindow(index);
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

    @Override
    public void show() {
        super.show();
        if(gui !=null) gui.widgets.values().forEach(Area::show);
    }

    @Override
    public void hide() {
//        super.hide();
//        if(gui !=null) gui.widgets.values().forEach(Area::hide);
                if(gui!=null) gui.hide();
    }
    
}
