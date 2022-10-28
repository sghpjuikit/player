package sp.it.util

abstract class HierarchicalBase<T, H: Hierarchical<H>>(@JvmField val value: T, @JvmField val parent: H?): Hierarchical<H> {
   override val hParent: H? get() = parent
}