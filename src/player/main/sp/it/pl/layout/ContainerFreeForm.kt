package sp.it.pl.layout

import javafx.scene.Node
import sp.it.util.collections.filterNotNullValues
import sp.it.util.conf.cv
import sp.it.util.conf.def

class ContainerFreeForm: Container<ContainerFreeFormUi> {

   /** Name of this container. */
   override val name = "ContainerFreeForm"
   /** Whether this container shows child window headers. Default true. */
   val showHeaders by cv(true).def(name = "Show header", info = "Whether window headers are visible")

   @JvmOverloads
   constructor(state: FreeFormContainerDb = FreeFormContainerDb()): super(state) {
      showHeaders.value = state.showHeaders
      children += state.children.mapValues { it.value?.toDomain() }.filterNotNullValues()
      setChildrenParents()
   }

   override fun addChild(index: Int?, c: Component?) {
      if (index==null) return

      if (c==null) children.remove(index)
      else children[index] = c

      setParentRec()
      ui?.load(index, c)
   }

   override fun removeChild(index: Int?) {
      if (index==null) return

      ui?.closeWindow(index)
      children[index]?.close()
      children -= index
      closeWindowIfEmpty()
   }

   override fun validChildIndexes() = generateSequence(1) { it + 1 }

   override fun getEmptySpot() = null

   override fun load(): Node {
      if (ui==null) ui = ContainerFreeFormUi(this)
      ui!!.load()
      return ui!!.root
   }

   override fun toDb() = FreeFormContainerDb(id, loadType.value, locked.value, showHeaders.value, children.mapValues { it.value?.toDb() }, properties)

   override fun show() = ui?.show() ?: Unit

   override fun hide() = ui?.hide() ?: Unit

}