/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.container.switchcontainer;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;

import Layout.Component;
import Layout.container.Container;

/**
 *
 * @author Plutonium_
 */
public class SwitchContainer extends Container<SwitchPane> {

    Map<Integer,Component> children = new HashMap<>();

    @Override
    public Map<Integer, Component> getChildren() {
        return children;
    }

    @Override
    public void addChild(Integer index, Component c) {System.out.println(index + " " + c);
        if(index==null) return;

        if(c==null) children.remove(index);
        else children.put(index, c);

        if(ui!=null) ui.addTab(index, c);
        setParentRec();
    }

    @Override
    public Integer getEmptySpot() {
        int i = 0;
        while(children.get(i)!=null) {
            i = i==0 ? 1 : i>0 ? -i : -i+1;  // 0,1,-1,2,-2,3,-3, ...
        }
        return i;
    }

    @Override
    public Node load() {
        if(ui==null) ui = new SwitchPane(this);
        new HashMap<>(children).forEach((i,c) -> ui.addTab(i,(Container)c));
        return ui.getRoot();
    }

}