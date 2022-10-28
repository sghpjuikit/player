package sp.it.util

import sp.it.util.functional.recurse
import sp.it.util.functional.traverse

/**
 * Object in a tree hierarchy, having an optional parent and any number of children.
 * All objects in the hierarchy inherit from *hierarchy* type H.
 *
 * @param <H> hierarchy type
 */
@Suppress("UNCHECKED_CAST")
interface Hierarchical<H: Hierarchical<H>> {
   /** Hierarchy parent of this hierarchical item */
   val hParent: H?
   /** Hierarchy children of this hierarchical item */
   val hChildren: List<H>
   /** Hierarchy traversal sequence of direct children of this hierarchical item */
   val hChildrenTraverse: Sequence<H>
      get() = hChildren.asSequence()
   /** Hierarchy traversal recursive sequence of all children of this hierarchical item including this as first element */
   val hChildrenRecurse: Sequence<H>
      get() = (this as H).recurse { it.hChildren }
   /** Hierarchy traversal recursive sequence of all leaf children of this hierarchical item including this as first element */
   val hChildrenRecurseLeafs: Sequence<H>
      get() = hChildrenRecurse.filter { it.isHLeaf }
   /** Whether this hierarchical item is oot of the hierarchy */
   val isHRoot: Boolean
      get() = hParent==null
   /** Whether this hierarchical item is leaf - has no children */
   val isHLeaf: Boolean
      get() = hChildren.isEmpty()
   /** Hierarchy root of this hierarchical item */
   val hRoot: H?
      get() = (this as H).traverse { it.hParent }.last()

   fun isHChildOf(h: H): Boolean = if (this===h) false else (this as H).traverse { it.hParent }.any { it===h }

   fun isHParentOf(h: H?): Boolean = h!=null && h.isHChildOf(this as H)

}