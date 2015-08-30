/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout;

import java.io.ObjectStreamException;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import Layout.Widgets.Widget;

import static util.functional.Util.list;


/**
 *
 * @author Plutonium_
 */
public class SwitchContainer extends Container {
    
    @XStreamOmitField
    SwitchPane graphics;
    Map<Integer,Component> children = new HashMap<>();
    
    @Override
    public Map<Integer, Component> getChildren() {
        return children;
    }

    @Override
    public void addChild(Integer index, Component c) {
        if(index==null) return;
        
        if(c==null) {
            children.remove(index);
            load();
            setParentRec();
        } else if(c instanceof Container) {
            children.put(index, c);
            Container.class.cast(c).setParent(this);
            setParentRec();
            load();
        } else if (c instanceof Widget) {
            UniContainer wrap = new UniContainer();
            addChild(index, wrap);
            wrap.setChild(c);
        }
    }

    @Override
    public Integer getEmptySpot() {
        int i = 0;
        while(children.get(i)!=null) {
            i = i==0 ? 1 : i>0 ? -i : -i-1;  // 0,1,-1,2,-2,3,-3, ...
        }
        return i;
    }

    @Override
    public Node load() {
        if(graphics==null) graphics = new SwitchPane(this);
        new HashMap<>(children).forEach((i,c) -> graphics.addTab(i,(Container)c));
        return graphics.getRoot();
    }

    @Override
    public SwitchPane getGraphics() {
        return graphics;
    }
    
    /** Invoked just before the serialization. */
    protected Object writeReplace() throws ObjectStreamException {
        // both an optimization & bugfix
        // close empty children containers
        list(getChildren().values()).forEach(c -> {
            if(c instanceof Container && ((Container)c).getAllWidgets().count()==0)
                removeChild(c);
        });
        
        return this;
    }
    
}
