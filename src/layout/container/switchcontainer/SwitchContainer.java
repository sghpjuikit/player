/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package layout.container.switchcontainer;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;

import util.conf.IsConfig;
import util.conf.IsConfigurable;
import layout.Component;
import layout.container.Container;
import util.access.V;

import static util.reactive.Util.maintain;

/**
 *
 * @author Martin Polakovic
 */
@IsConfigurable("Tabs")
public class SwitchContainer extends Container<SwitchPane> {

    @IsConfig(name = "Discrete mode (D)", info = "Use discrete (D) and forbid seamless (S) tab switching."
            + " Tabs are always aligned. Seamless mode alows any tab position.")
    public static final V<Boolean> ALIGN = new V<>(false);

    @IsConfig(name = "Switch drag distance (D)", info = "Required length of drag at"
            + " which tab switch animation gets activated. Tab switch activates if"
            + " at least one condition is fulfilled min distance or min fraction.")
    private static final V<Double> MIN_SWITCH_DIST_ABS = new V<>(150.0);

    @IsConfig(name = "Switch drag distance coeficient (D)", info = "Defines distance from edge in "
            + "percent of tab's width in which the tab switches.", min = 0, max = 1)
    private static final V<Double> MIN_SWITCH_DIST_REL = new V<>(0.15);

    @IsConfig(name = "Drag inertia (S)", info = "Inertia of the tab switch animation. "
            + "Defines distance the dragging will travel after input has been stopped. Only when ", min = 0, max = 10)
    private static final V<Double> DRAG_INERTIA = new V<>(1.5);

    @IsConfig(name = "Snap tabs (S)", info = "Align tabs when close to edge.")
    public static final V<Boolean> SNAP = new V<>(true);

    @IsConfig(name = "Snap distance coeficient (S)", info = "Defines distance from edge in "
            + "percent of tab's width in which the tab autoalignes. Setting to maximum "
            + "(0.5) has effect of always snapping the tabs, while setting to minimum"
            + " (0) has effect of disabling tab snapping.", min = 0, max = 0.5)
    private static final V<Double> SNAP_TRESHOLD_REL = new V<>(0.05);

    @IsConfig(name = "Snap distance (S)", info = "Required distance from edge at"
            + " which tabs align. Tab snap activates if"
            + " at least one condition is fulfilled min distance or min fraction.")
    public static final V<Double> SNAP_TRESHOLD_ABS = new V<>(25.0);

    @IsConfig(name = "Zoom", info = "Zoom factor.", min=0.2, max=1)
    public static final V<Double> ZOOM = new V<>(0.7);


    Map<Integer,Component> children = new HashMap<>();

    @Override
    public Map<Integer, Component> getChildren() {
        return children;
    }

    @Override
    public void addChild(Integer index, Component c) {
        if(index==null) return;

        if(c==null) children.remove(index);
        else children.put(index, c);

        if(ui!=null) ui.addTab(index, c);
        setParentRec();
    }

    @Override
    public Integer getEmptySpot() {
        int i = 0;
        while(children.containsKey(i)) {
            i = i==0 ? 1 : i>0 ? -i : -i+1;  // 0,1,-1,2,-2,3,-3, ...
        }
        return i;
    }

    @Override
    public Node load() {
        if(ui==null) {
            ui = new SwitchPane(this);

            // bind properties
            maintain(ALIGN, ui.align);
            maintain(SNAP, ui.snap);
            maintain(MIN_SWITCH_DIST_ABS, ui.switch_dist_abs);
            maintain(MIN_SWITCH_DIST_REL, ui.switch_dist_rel);
            maintain(SNAP_TRESHOLD_ABS, ui.snap_treshold_abs);
            maintain(SNAP_TRESHOLD_REL, ui.snap_treshold_rel);
            maintain(DRAG_INERTIA, ui.drag_inertia);
            maintain(ZOOM, ui.zoomScaleFactor);
        }
        children.forEach(ui::addTab);
        return ui.getRoot();
    }

}