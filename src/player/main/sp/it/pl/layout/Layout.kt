package sp.it.pl.layout

import javafx.scene.Node
import javafx.scene.layout.AnchorPane
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.readOnly
import sp.it.util.functional.ifNotNull
import sp.it.util.ui.layFullArea

class Layout: ContainerUni {

   /** Name of this container. */
   override val name = "Layout"
   /** Whether this container is designed as a root and overrides its ui decorations. Default `false`. */
   val isStandalone by cv(false).readOnly().def(name = "Standalone", info = "Whether this container is designed as a root and overrides its ui decorations.")

   private var uiL: LayoutUi? = null

   @JvmOverloads
   constructor(state: RootContainerDb = RootContainerDb()): super(state.toUni())

   override fun load(): Node {
      setParentRec()

      uiL = when (child) {
         is ContainerSwitch -> null.disposeUiL()
         else -> uiL ?: LayoutUi(this)
      }

      val n = super.load()

      uiL.ifNotNull { root!!.layFullArea += it.io }

      return n
   }

   override fun show() {
      super.show()
      uiL?.show()
   }

   override fun hide() {
      super.hide()
      uiL?.hide()
   }

   override fun toString() = "${Layout::class} name=$name"

   override fun toDb() = RootContainerDb(id, loadType.value, locked.value, child?.toDb(), properties)

   private fun <T> T.disposeUiL() = apply { uiL?.dispose() }

   companion object {
      fun openStandalone(root: AnchorPane): Layout = Layout().apply {
         isStandalone.value = true
         load(root)
      }
   }

}