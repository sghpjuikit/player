package sp.it.pl.layout.container

import javafx.scene.Node
import sp.it.pl.layout.Component
import sp.it.util.access.v
import java.util.HashMap

class FreeFormContainer: Container<FreeFormContainerUi>() {

   val showHeaders = v(true)
   private val children = HashMap<Int, Component>()

   override fun getChildren(): Map<Int, Component> = children

   override fun addChild(index: Int?, c: Component?) {
      if (index==null) return

      if (c==null) children.remove(index)
      else children[index] = c

      ui?.loadWindow(index, c)
      setParentRec()
   }

   override fun removeChild(index: Int?) {
      if (index==null) return

      ui.closeWindow(index)
      getChildren()[index]?.close()
      children.remove(index)
   }

   override fun getEmptySpot() = null

   override fun load(): Node {
      if (ui==null) ui = FreeFormContainerUi(this)
      ui.load()
      return ui.root
   }

   override fun show() = ui?.show() ?: Unit

   override fun hide() = ui?.hide() ?: Unit

}