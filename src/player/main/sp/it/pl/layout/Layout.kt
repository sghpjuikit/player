package sp.it.pl.layout

import javafx.scene.Node
import javafx.scene.layout.AnchorPane

class Layout: ContainerUni {

   /** Name of this container. */
   override val name = "Layout"

   @JvmOverloads
   constructor(state: RootContainerDb = RootContainerDb()): super(state.toUni())

   /**
    * @param parentPane root node to load the layout into.
    * @return root node of the layout
    */
   override fun load(parentPane: AnchorPane?): Node {
      val n = super.load(parentPane)
      setParentRec()
      return n
   }

   override fun toString() = "${Layout::class} name=$name"

   override fun toDb() = RootContainerDb(id, loadType.value, locked.value, child?.toDb(), properties)

   companion object {
      fun openStandalone(root: AnchorPane): Layout {
         val l = Layout()
         l.isStandalone.value = true
         l.load(root)
         return l
      }
   }

}