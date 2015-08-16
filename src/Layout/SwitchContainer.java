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
        
//        if(c==null) {
//            children.remove(index);
//            load();
//        } else  {
//            if(c instanceof UniContainer) {
//                children.put(index, c);
//                Container.class.cast(c).parent = this;
//                load();
//            } else {
//                UniContainer wrap = new UniContainer();
//                addChild(index, wrap);
//                wrap.setChild(c);
//            }
//        }
        
        
        if(c==null) {
            children.remove(index);
            load();
            initialize();
        } else if(c instanceof Container) {
            children.put(index, c);
            Container.class.cast(c).parent = this;
            initialize();
            load();
        } else if (c instanceof Widget) {
            UniContainer wrap = new UniContainer();
            addChild(index, wrap);
            wrap.setChild(c);
        }
    }

    @Override
    public Integer getEmptySpot() {
        int i = -1;
        while(true) {
            i++;
            if(!children.keySet().contains(i)) {
                return i;
            }
        }
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
        // close empty children containers
        list(getChildren().values()).forEach(c -> {
            if(c instanceof Container && ((Container)c).getAllWidgets().count()==0)
                removeChild(c);
        });
        
        return this;
    }
    
}
