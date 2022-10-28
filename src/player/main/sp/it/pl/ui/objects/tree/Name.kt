package sp.it.pl.ui.objects.tree

import sp.it.util.HierarchicalBase
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull

/** Hierarchical tree of strings. Often represented by a text path delimiting parts with [DELIMITER]. */
class Name: HierarchicalBase<String, Name> {
   val pathUp: String
   private var children = ArrayList<Name>()
   override val hChildren: List<Name> get() = children

   private constructor(value: String, pathUp: String, parent: Name?): super(value, parent) {
      this.pathUp = pathUp
   }

   operator fun contains(path: String): Boolean = children.any { path==it.value }

   operator fun plusAssign(path: Collection<String>): Unit = path.forEach { this += it }

   operator fun plusAssign(path: String) {
      if (path.isEmpty()) return

      val i = path.indexOf(DELIMITER)
      val isLeaf = i<0
      if (isLeaf) {
         if (path in this)
            children += child(path)
      } else {
         val prefix = path.substring(0, i)
         val suffix = path.substring(i + 1)
         children.find { prefix==it.value }
            .ifNotNull { it += suffix }
            .ifNull { children += child(prefix).apply { this += suffix } }
      }
   }

   fun sort() {
      children.sortBy { it.value }
      children.forEach { it.sort() }
   }

   private fun child(path: String): Name = Name(path, if (pathUp.isEmpty()) path else pathUp + DELIMITER + path, parent)

   companion object {
      private const val DELIMITER = '.'

      fun treeOfPaths(rootName: String, paths: Collection<String>): Name =
         Name(rootName, "", null).apply {
            this += paths
            sort()
         }
   }
}