package sp.it.pl.layout.container

import javafx.geometry.Orientation
import javafx.geometry.Orientation.VERTICAL
import javafx.scene.Node
import sp.it.pl.layout.BiContainerDb
import sp.it.pl.layout.Component
import sp.it.util.access.v
import sp.it.util.collections.filterNotNullValues
import sp.it.util.dev.failIf
import java.util.HashMap

/** [Container] containing two children split vertically or horizontally. */
class BiContainer: Container<BiContainerUi> {

   /** Orientation of this container. */
   val orientation = v(VERTICAL)
   val position = v(0.5)
   val absoluteSize = v(0)
   val collapsed = v(0)
   private val children = HashMap<Int, Component>()

   constructor(state: BiContainerDb = BiContainerDb()): super(state) {
      orientation.value = state.orientation
      position.value = state.position
      absoluteSize.value = state.absoluteSize
      collapsed.value = state.collapsed
      children += state.children.filter { it.key in 1..2 }.mapValues { it.value?.toDomain() }.filterNotNullValues()
      setChildrenParents()
   }

   constructor(o: Orientation): this(BiContainerDb(orientation = o))

   override fun getName() = "BiContainer"

   override fun load(): Node {
      ui = ui ?: BiContainerUi(this)

      ui.setComponent(1, children[1])
      ui.setComponent(2, children[2])

      return ui.root
   }

   override fun getChildren() = children

   /**
    * @param index null does nothing, value other than 1 or 2 throws exception
    */
   override fun addChild(index: Int?, c: Component?) {
      if (index==null) return
      failIf(index!=1 && index!=2) { "Index=$index must be 1 or 2" }

      if (c==null) children.remove(index)
      else children[index] = c

      ui?.setComponent(index, c)
      setParentRec()
   }

   fun swapChildren() {
      val c1 = children[1]
      val c2 = children[2]
      when {
         c1==null && c2==null -> return
         c1==null && c2!=null -> c2.swapWith(this, 1)
         c1!=null && c2==null || c1!=null && c2!=null -> c1.swapWith(this, 2)
      }
   }

   override fun getEmptySpot() = when {
      children[1]==null -> 1
      children[2]==null -> 2
      else -> null
   }

   override fun show() = ui?.show() ?: Unit

   override fun hide() = ui?.hide() ?: Unit

   override fun toDb() = BiContainerDb(id, orientation.value, position.value, absoluteSize.value, collapsed.value, loadType.value, locked.value, children.mapValues { it.value.toDb() }, properties)

}