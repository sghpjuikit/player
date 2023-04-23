package sp.it.pl.layout

import javafx.geometry.Orientation
import javafx.geometry.Orientation.VERTICAL
import javafx.geometry.Pos
import javafx.scene.Node
import sp.it.util.collections.filterNotNullValues
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.dev.failIf

/** [Container] containing two children split vertically or horizontally. */
class ContainerSeq: Container<ContainerSeqUi> {

   /** Name of this container. */
   override val name = "ContainerSeq"
   /** Orientation of this container. Default [VERTICAL] */
   val orientation by cv(VERTICAL).def(name = "Orientation", info = "Orientation of this container.")
   val fill by cv(false).def(name = "Fill", info = "Whether or not resizable children will be resized to fill the full size of the box or be resized to their preferred size and aligned according to the alignment value. Note that if the box alignment is set to BASELINE, then this property will be ignored and children will be resized to their preferred size.")
   val alignment by cv(Pos.CENTER).def(name = "Alignment", info = "")

   constructor(state: SeqContainerDb = SeqContainerDb()): super(state) {
      orientation.value = state.orientation
      fill.value = state.fill
      alignment.value = state.alignment
      children += state.children.mapValues { it.value?.toDomain() }.filterNotNullValues()
      setChildrenParents()
   }

   constructor(o: Orientation): this(SeqContainerDb(orientation = o))

   override fun load(): Node {
      val u = ui ?: ContainerSeqUi(this)
      ui = u

      if (children.isEmpty()) children[0] = null
      children.forEach(u::setComponent)

      return u.root
   }

   /**
    * @param index null does nothing, value other than 1 or 2 throws exception
    */
   override fun addChild(index: Int?, c: Component?) {
      if (index==null) return
      failIf(index<0) { "Index=$index must be >=0" }

      if (c==null) children.remove(index)
      else children[index] = c

      setParentRec()
      ui?.setComponent(index, c)
   }

   override fun validChildIndexes() = generateSequence(0) { it+1 }

   override fun validChildIndexOrder(index: Int) = index

   override fun show() = ui?.show() ?: Unit

   override fun hide() = ui?.hide() ?: Unit

   override fun toDb() = SeqContainerDb(id, orientation.value, fill.value, alignment.value, loadType.value, locked.value, children.mapValues { it.value?.toDb() }, properties)

}