package sp.it.pl.layout.container

import javafx.scene.Node
import sp.it.pl.layout.Component
import sp.it.pl.layout.ComponentDb
import sp.it.pl.layout.Layouter
import sp.it.pl.layout.UniContainerDb
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.WidgetUi
import sp.it.util.collections.setToOne
import sp.it.util.dev.failCase
import sp.it.util.dev.failIf
import sp.it.util.ui.setAnchors


/** [Container] containing one child spanning entire area. */
open class UniContainer: Container<ComponentUi> {

   protected var _child: Component? = null
   protected var isStandalone = false

   /** Equal to [getChildren]`.get(1)` and [addChild]`(1, newChild)`. */
   var child: Component?
      get() = _child
      set(w) = addChild(1, w)

   constructor(state: UniContainerDb = UniContainerDb()): super(state) {
      _child = state.child?.toDomain()
      setChildrenParents()
   }

   override fun getName() = "UniContainer"

   override fun load(): Node {
      val n = when (val c = _child) {
         is Container<*> -> {
            ui = null.disposeUi()
            c.load(root)
         }
         is Widget -> {
            ui = ui.takeIf { it is WidgetUi && it.widget===_child } ?: WidgetUi(this, 1, c).disposeUi().apply {
               if (isStandalone)
                  setStandaloneStyle()
            }
            ui.root
         }
         null -> {
            ui = ui.takeIf { it is Layouter } ?: Layouter(this, 1).disposeUi()
            ui.root
         }
         else -> failCase(c)
      }

      root.children setToOne n
      n.setAnchors(0.0)

      return n
   }

   override fun getChildren(): Map<Int, Component?> = if (_child==null) mapOf() else mapOf(1 to _child)

   /**
    * @param index null does nothing, value other than 1 throws exception
    */
   override fun addChild(index: Int?, c: Component?) {
      if (index==null) return
      failIf(index!=1) { "Index=$index must be 1" }

      if (c is Container<*>) c.parent = this
      _child = c
      load()
      setParentRec()
   }

   override fun indexOf(c: Component): Int? = if (c===_child) 1 else null

   override fun getEmptySpot(): Int? = if (_child===null) 1 else null

   private fun <T> T.disposeUi() = apply { if (ui is Layouter || ui is WidgetUi) ui.dispose() }

   override fun toDb(): ComponentDb = UniContainerDb(id, loadType.value, locked.value, child?.toDb(), properties)

}