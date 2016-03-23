/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package layout.container.freeformcontainer;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;

import layout.area.FreeFormArea;
import layout.Component;
import layout.container.Container;

/**
 */
public class FreeFormContainer extends Container<FreeFormArea> {

    private final Map<Integer,Component> children = new HashMap();


    @Override
    public Map<Integer, Component> getChildren() {
        return children;
    }

    @Override
    public void addChild(Integer index, Component c) {
        if(index == null) return;

        if(c==null) children.remove(index);
        else children.put(index, c);

        if(ui!=null) ui.loadWindow(index, c);
        setParentRec();
    }

    @Override
    public void removeChild(Integer index) {
        ui.closeWindow(index);
        children.remove(index);
    }


    @Override
    public Integer getEmptySpot() {
        return null;
    }

    @Override
    public Node load() {
        if(ui==null) ui = new FreeFormArea(this);
        ui.load();
        return ui.getRoot();
    }

    @Override
    public void show() {
//        super.show();
        if(ui!=null) ui.show();
    }

    @Override
    public void hide() {
//        super.hide();
//        if(gui !=null) gui.widgets.values().forEach(Area::hide);
                if(ui!=null) ui.hide();
    }

}
