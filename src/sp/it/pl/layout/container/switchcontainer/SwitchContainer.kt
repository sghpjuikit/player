/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sp.it.pl.layout.container.switchcontainer

import javafx.scene.Node
import sp.it.pl.layout.Component
import sp.it.pl.layout.container.Container
import sp.it.pl.main.MultiConfigurableBase
import sp.it.pl.main.Settings
import sp.it.pl.main.between
import sp.it.pl.main.cv
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.reactive.attachFrom
import java.util.HashMap

class SwitchContainer: Container<SwitchPane>() {

    private val children = HashMap<Int, Component>()

    override fun getChildren(): Map<Int, Component> = children

    override fun addChild(index: Int?, c: Component?) {
        if (index==null) return

        if (c==null) children.remove(index)
        else children[index] = c

        ui?.addTab(index, c)
        setParentRec()
    }

    override fun getEmptySpot(): Int = generateSequence(0) { if (it>0) -it else -it+1 } // 0,1,-1,2,-2,3,-3, ...
            .first { !children.containsKey(it) }

    override fun load(): Node {
        if (ui==null) {
            ui = SwitchPane(this).also {
                it.align attachFrom align
                it.snap attachFrom snap
                it.switchDistAbs attachFrom minSwitchDistAbs
                it.switchDistRel attachFrom minSwitchDistRel
                it.snapThresholdAbs attachFrom snapThresholdAbs
                it.snapThresholdRel attachFrom snapThresholdRel
                it.dragInertia attachFrom dragInertia
                it.zoomScaleFactor attachFrom zoom
            }
        }
        children.forEach { i, c -> ui.addTab(i, c) }
        return ui.root
    }

    companion object: MultiConfigurableBase("${Settings.UI}.Tabs") {

        @IsConfig(name = "Discrete mode (D)", info = "Use discrete (D) and forbid seamless (S) tab switching."
                +" Tabs are always aligned. Seamless mode allows any tab position.")
        private val align by cv(false)

        @IsConfig(name = "Switch drag distance (D)", info = "Required length of drag at"
                +" which tab switch animation gets activated. Tab switch activates if"
                +" at least one condition is fulfilled min distance or min fraction.")
        private val minSwitchDistAbs by cv(150.0)

        @IsConfig(name = "Switch drag distance coefficient (D)", info = "Defines distance from edge in "
                +"percent of tab's width in which the tab switches.")
        private val minSwitchDistRel by cv(0.15).between(0.0, 1.0)

        @IsConfig(name = "Drag inertia (S)", info = "Inertia of the tab switch animation. "
                +"Defines distance the dragging will travel after input has been stopped. Only when ")
        private val dragInertia by cv(1.5).between(0.0, 10.0)

        @IsConfig(name = "Snap tabs (S)", info = "Align tabs when close to edge.")
        private val snap by cv(true)

        @IsConfig(name = "Snap distance coefficient (S)", info = "Defines distance from edge in "
                +"percent of tab's width in which the tab auto-aligns. Setting to maximum "
                +"(0.5) has effect of always snapping the tabs, while setting to minimum"
                +" (0) has effect of disabling tab snapping.")
        private val snapThresholdRel by cv(0.05).between(0.0, 0.5)

        @IsConfig(name = "Snap distance (S)", info = "Required distance from edge at"
                +" which tabs align. Tab snap activates if"
                +" at least one condition is fulfilled min distance or min fraction.")
        private val snapThresholdAbs by cv(25.0)

        @IsConfig(name = "Zoom", info = "Zoom factor")
        private val zoom by cv(0.7).between(0.2, 1.0)
    }

}