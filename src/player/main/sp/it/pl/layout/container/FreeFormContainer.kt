package sp.it.pl.layout.container

import javafx.scene.Node
import sp.it.pl.layout.Component
import sp.it.pl.layout.FreeFormContainerDb
import sp.it.util.access.v
import sp.it.util.collections.filterNotNullValues
import java.util.HashMap
import sp.it.util.conf.cv
import sp.it.util.conf.def

class FreeFormContainer: Container<FreeFormContainerUi> {

   /** Name of this container. */
   override val name = "FreeFormContainer"
   /** Whether this container shows child window headers. Default true. */
   val showHeaders by cv(true).def(name = "Show header", info = "Whether window headers are visible")
   /** Children of this container by index */
   private val children = HashMap<Int, Component>()

   @JvmOverloads
   constructor(state: FreeFormContainerDb = FreeFormContainerDb()): super(state) {
      showHeaders.value = state.showHeaders
      children += state.children.mapValues { it.value?.toDomain() }.filterNotNullValues()
      setChildrenParents()
   }

   override fun getChildren(): Map<Int, Component> = children

   override fun addChild(index: Int?, c: Component?) {
      if (index==null) return

      if (c==null) children.remove(index)
      else children[index] = c

      ui?.load(index, c)
      setParentRec()
   }

   override fun removeChild(index: Int?) {
      if (index==null) return

      ui.closeWindow(index)
      getChildren()[index]?.close()
      children.remove(index)
      closeWindowIfEmpty()
   }

   override fun getEmptySpot() = null

   override fun load(): Node {
      if (ui==null) ui = FreeFormContainerUi(this)
      ui.load()
      return ui.root
   }

   override fun toDb() = FreeFormContainerDb(id, loadType.value, locked.value, showHeaders.value, children.mapValues { it.value.toDb() }, properties)

   override fun show() = ui?.show() ?: Unit

   override fun hide() = ui?.hide() ?: Unit

}