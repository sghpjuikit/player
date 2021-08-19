package sp.it.pl.layout

import javafx.geometry.Orientation
import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Orientation.VERTICAL
import javafx.scene.Node
import sp.it.util.collections.filterNotNullValues
import sp.it.util.conf.between
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.readOnly
import sp.it.util.dev.failIf

/** [Container] containing two children split vertically or horizontally. */
class ContainerBi: Container<ContainerBiUi> {

   /** Name of this container. */
   override val name = "ContainerBi"
   /** Orientation of this container. Default [VERTICAL] */
   val orientation by cv(VERTICAL).def(name = "Orientation", info = "Orientation of this container.")
   /**
    * Position of the divider in not collapsed state.
    * * `[0,1]` range if [absoluteSize] 0
    * * `[0, Infinity)` range of width ([orientation] == [HORIZONTAL]) or height ([orientation] == [VERTICAL]) of the absolute child if [absoluteSize] 1 or 2.
    *
    * This value goes hand in hand with [absoluteSize], which is the dominant property in this relationship. Therefore:
    * * This value must be set to value required by [absoluteSize].
    * * This value converts itself to the other value range when [absoluteSize] changes.
    * * Initial value (before the component is displayed) will not be converted and should be provided in correct range.
    *   This is because the conversion depends on the actual ui size
    */
   val position by cv(0.5).readOnly().between(0, Double.MAX_VALUE).def(name = "Position", info = "Position of the divider when not collapsed. Value is in [0,1] when no child has fixed size, otherwise value is [0,∞) and refers to the size of the child.")
   /** Whether child is resized absolutely (retains its size on layout). 0 == none, 1 == child 1, 2 == child 2 */
   val absoluteSize by cv(0).between(0, 2).def(name = "Position fixed", info = "Whether specified child has fixed size. 0 == none, 1 == child 1, 2 == child 2.")
   /** Whether child is hidden so the other covers the entire space. 0 == none, -1 == child 1, 1 == child 2 */
   val collapsed by cv(0).between(-1, +1).def(name = "Collapsed", info = "Whether child is hidden so the other covers the entire space. 0 == none, -1 == child 1, 1 == child 2.")
   /** Whether the children should appear as one, i.e., the divider has little to no visibility */
   val joined by cv(false).def(name = "Joined", info = "Whether the children should appear as one, i.e., the divider has little to no visibility.")

   constructor(state: BiContainerDb = BiContainerDb()): super(state) {
      orientation.value = state.orientation
      position.value = state.position
      absoluteSize.value = state.absoluteSize
      collapsed.value = state.collapsed
      joined.value = state.joined
      children += state.children.filter { it.key in 1..2 }.mapValues { it.value?.toDomain() }.filterNotNullValues()
      setChildrenParents()
   }

   constructor(o: Orientation): this(BiContainerDb(orientation = o))

   override fun load(): Node {
      val u = ui ?: ContainerBiUi(this)
      ui = u

      u.setComponent(1, children[1])
      u.setComponent(2, children[2])

      return u.root
   }

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

   override fun validChildIndexes() = sequenceOf(1, 2)

   override fun show() = ui?.show() ?: Unit

   override fun hide() = ui?.hide() ?: Unit

   override fun toDb() = BiContainerDb(id, orientation.value, position.value, absoluteSize.value, collapsed.value, joined.value, loadType.value, locked.value, children.mapValues { it.value?.toDb() }, properties)

}