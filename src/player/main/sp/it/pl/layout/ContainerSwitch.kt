package sp.it.pl.layout

import sp.it.pl.main.AppSettings.ui.tabs as conf
import javafx.scene.Node
import sp.it.util.collections.filterNotNullValues
import sp.it.util.conf.EditMode.APP
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.conf.between
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.readOnly
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.syncFrom

class ContainerSwitch: Container<ContainerSwitchUi> {

   /** Name of this container. */
   override val name = "ContainerSwitch"
   /** Position of within the infinite horizontal view in px. Default `0.0`. */
   val translate by cv(0.0).def(name = "Position", info = "Position of within the infinite horizontal view in px.", editable = APP)
   val align by cv(false).readOnly().def(conf.`discreteMode(D)`)
   val minSwitchDistAbs by cv(150.0).readOnly().def(conf.`switchDragDistance(D)`)
   val minSwitchDistRel by cv(0.15).between(0.0, 1.0).readOnly().def(conf.`switchDragDistanceCoefficient(D)`)
   val dragInertia by cv(1.5).between(0.0, 10.0).readOnly().def(conf.`dragInertia(S)`)
   val snap by cv(true).readOnly().def(conf.`snapDistance(S)`)
   val snapThresholdRel by cv(0.05).between(0.0, 0.5).readOnly().def(conf.`snapDistanceCoefficient(S)`)
   val snapThresholdAbs by cv(40.0).readOnly().def(conf.`snapDistance(S)`)
   val zoom by cv(0.7).between(0.2, 1.0).readOnly().def(conf.zoom)
   /** Invoked on [close]. */
   private val onClose = Disposer()

   @JvmOverloads
   constructor(state: ContainerSwitchDb = ContainerSwitchDb()): super(state) {
      translate.value = state.translate
      children += state.children.mapValues { it.value?.toDomain() }.filterNotNullValues()
      setChildrenParents()

      align syncFrom Companion.align on onClose
      minSwitchDistAbs syncFrom Companion.minSwitchDistAbs on onClose
      minSwitchDistRel syncFrom Companion.minSwitchDistRel on onClose
      dragInertia syncFrom Companion.dragInertia on onClose
      snap syncFrom Companion.snap on onClose
      snapThresholdRel syncFrom Companion.snapThresholdRel on onClose
      snapThresholdAbs syncFrom Companion.snapThresholdAbs on onClose
      zoom syncFrom Companion.zoom on onClose
   }

   override fun addChild(index: Int?, c: Component?) {
      if (index==null) return

      if (c==null) children.remove(index)
      else children[index] = c

      setParentRec()
      ui?.addTab(index, c)
   }

   override fun validChildIndexes() = generateSequence(0) { if (it>0) -it else -it + 1 } // 0,+1,-1,+2,-2,+3,-3, ...

   override fun validChildIndexOrder(index: Int) = if (index>=0) index*2 else -index*2 - 1

   override fun load(): Node {
      val u = ui ?: ContainerSwitchUi(this).also {
         it.align syncFrom align on onClose
         it.snap syncFrom snap on onClose
         it.switchDistAbs syncFrom minSwitchDistAbs on onClose
         it.switchDistRel syncFrom minSwitchDistRel on onClose
         it.snapThresholdAbs syncFrom snapThresholdAbs on onClose
         it.snapThresholdRel syncFrom snapThresholdRel on onClose
         it.dragInertia syncFrom dragInertia on onClose
         it.zoomScaleFactor syncFrom zoom on onClose
      }
      ui = u
      children.forEach(u::addTab)
      return u.root
   }

   override fun close() {
      super.close()
      onClose()
   }

   override fun toDb() = ContainerSwitchDb(id, translate.value, loadType.value, locked.value, children.mapValues { it.value?.toDb() }, properties)

   companion object: GlobalSubConfigDelegator(conf.name) {
      private val align by cv(false).def(conf.`discreteMode(D)`)
      private val minSwitchDistAbs by cv(150.0).def(conf.`switchDragDistance(D)`)
      private val minSwitchDistRel by cv(0.15).between(0.0, 1.0).def(conf.`switchDragDistanceCoefficient(D)`)
      private val dragInertia by cv(1.5).between(0.0, 10.0).def(conf.`dragInertia(S)`)
      private val snap by cv(true).def(conf.`snapDistance(S)`)
      private val snapThresholdRel by cv(0.05).between(0.0, 0.5).def(conf.`snapDistanceCoefficient(S)`)
      private val snapThresholdAbs by cv(40.0).def(conf.`snapDistance(S)`)
      private val zoom by cv(0.7).between(0.2, 1.0).def(conf.zoom)
   }

}