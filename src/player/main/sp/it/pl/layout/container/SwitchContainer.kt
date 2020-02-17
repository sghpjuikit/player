package sp.it.pl.layout.container

import javafx.scene.Node
import sp.it.pl.layout.Component
import sp.it.pl.layout.SwitchContainerDb
import sp.it.util.access.v
import sp.it.util.collections.filterNotNullValues
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.conf.between
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.reactive.syncFrom
import java.util.HashMap
import sp.it.pl.main.AppSettings.ui.tabs as conf

class SwitchContainer: Container<SwitchContainerUi> {

   override val name = "SwitchContainer"
   val translate = v(0.0)
   private val children = HashMap<Int, Component>()

   @JvmOverloads
   constructor(state: SwitchContainerDb = SwitchContainerDb()): super(state) {
      translate.value = state.translate
      children += state.children.mapValues { it.value?.toDomain() }.filterNotNullValues()
      setChildrenParents()
   }

   override fun getChildren(): Map<Int, Component> = children

   override fun addChild(index: Int?, c: Component?) {
      if (index==null) return

      if (c==null) children.remove(index)
      else children[index] = c

      ui?.addTab(index, c)
      setParentRec()
   }

   override fun getEmptySpot(): Int = generateSequence(0) { if (it>0) -it else -it + 1 } // 0,1,-1,2,-2,3,-3, ...
      .first { !children.containsKey(it) }

   override fun load(): Node {
      ui = ui ?: SwitchContainerUi(this).also {
         it.align syncFrom align
         it.snap syncFrom snap
         it.switchDistAbs syncFrom minSwitchDistAbs
         it.switchDistRel syncFrom minSwitchDistRel
         it.snapThresholdAbs syncFrom snapThresholdAbs
         it.snapThresholdRel syncFrom snapThresholdRel
         it.dragInertia syncFrom dragInertia
         it.zoomScaleFactor syncFrom zoom
      }
      children.forEach { (i, c) -> ui.addTab(i, c) }
      return ui.root
   }

   override fun toDb() = SwitchContainerDb(id, translate.value, loadType.value, locked.value, children.mapValues { it.value.toDb() }, properties)

   companion object: GlobalSubConfigDelegator(conf.name) {
      private val align by cv(false).def(
         name = "Discrete mode (D)",
         info = "Use discrete (D) and forbid seamless (S) tab switching. Tabs are always aligned. Seamless mode allows any tab position."
      )
      private val minSwitchDistAbs by cv(150.0).def(
         name = "Switch drag distance (D)",
         info = "Required length of drag at"
         + " which tab switch animation gets activated. Tab switch activates if"
         + " at least one condition is fulfilled min distance or min fraction."
      )
      private val minSwitchDistRel by cv(0.15).between(0.0, 1.0).def(
         name = "Switch drag distance coefficient (D)",
         info = "Defines distance from edge in percent of tab's width in which the tab switches."
      )
      private val dragInertia by cv(1.5).between(0.0, 10.0).def(
         name = "Drag inertia (S)",
         info = "Inertia of the tab switch animation. Defines distance the dragging will travel after input has been stopped."
      )
      private val snap by cv(true).def(name = "Snap tabs (S)", info = "Align tabs when close to edge.")
      private val snapThresholdRel by cv(0.05).between(0.0, 0.5).def(
         name = "Snap distance coefficient (S)",
         info = "Defines distance from edge in "
         + "percent of tab's width in which the tab auto-aligns. Setting to maximum "
         + "(0.5) has effect of always snapping the tabs, while setting to minimum"
         + " (0) has effect of disabling tab snapping."
      )
      private val snapThresholdAbs by cv(40.0).def(
         name = "Snap distance (S)",
         info = "Required distance from edge at which tabs align. Tab snap activates if at least one condition is fulfilled min distance or min fraction."
      )
      private val zoom by cv(0.7).between(0.2, 1.0).def(name = "Zoom", info = "Zoom factor")
   }

}