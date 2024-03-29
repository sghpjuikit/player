package sp.it.pl.layout

import javafx.scene.Node
import sp.it.util.collections.setToOne
import sp.it.util.dev.failIf
import sp.it.util.functional.asIf
import sp.it.util.functional.toUnit
import sp.it.util.ui.setAnchors

/**
 * [Container] containing one child spanning entire area.
 *
 * To be as light-weight as possible, this container's [ui] is simply the ui of the [child].
 */
open class ContainerUni: Container<ComponentUi> {

   /** Name of this container. */
   override val name = "ContainerUni"

   /** Convenience for `[children][1]` and `addChild(1, newChild)`. */
   var child: Component?
      get() = children[1]
      set(w) = addChild(1, w)

   @JvmOverloads
   constructor(state: ContainerUniDb = ContainerUniDb()): super(state) {
      children[1] = state.child?.toDomain()
      setChildrenParents()
   }

   override fun load(): Node {
      val n = when (val c = child) {
         is Container<*> -> {
            ui = null.disposeUi()
            c.load(root)
         }
         is Widget -> {
            ui = ui.takeIf { it is WidgetUi && it.widget===child } ?: WidgetUi(this, 1, c).disposeUi()
            ui!!.root
         }
         null -> {
            ui = ui.takeIf { it is Layouter } ?: Layouter(this, 1).disposeUi()
            ui!!.root
         }
      }

      root!!.children setToOne n
      n.setAnchors(0.0)

      return n
   }

   /** @param index null does nothing, value other than 1 throws exception */
   override fun addChild(index: Int?, c: Component?) {
      if (index==null) return
      failIf(index!=1) { "Index=$index must be 1" }

      children[1] = c
      setParentRec()
      if (root!=null) load()
   }

   override fun show() = (child.asIf<Container<*>>() ?: ui)?.show().toUnit()

   override fun hide() = (child.asIf<Container<*>>() ?: ui)?.hide().toUnit()

   override fun validChildIndexes() = sequenceOf(1)

   override fun validChildIndexOrder(index: Int) = index

   override fun getEmptySpot(): Int? = if (child===null) 1 else null

   override fun toDb(): ComponentDb = ContainerUniDb(id, loadType.value, locked.value, child?.toDb(), properties)

   private fun <T> T.disposeUi() = apply { ui?.dispose() }

}