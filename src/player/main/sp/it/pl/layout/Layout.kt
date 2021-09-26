package sp.it.pl.layout

import javafx.scene.Node
import javafx.scene.layout.AnchorPane
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.readOnly

class Layout: ContainerUni {

   /** Name of this container. */
   override val name = "Layout"
   /** Whether this container is designed as a root and overrides its ui decorations. Default `false`. */
   val isStandalone by cv(false).readOnly().def(name = "Standalone", info = "Whether this container is designed as a root and overrides its ui decorations.")

   @JvmOverloads
   constructor(state: RootContainerDb = RootContainerDb()): super(state.toUni())

   /**
    * @param parentPane root node to load the layout into.
    * @return root node of the layout
    */
   override fun load(parentPane: AnchorPane?): Node {
      setParentRec()
      val n = super.load(parentPane)
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