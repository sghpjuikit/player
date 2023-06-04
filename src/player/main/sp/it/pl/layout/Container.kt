package sp.it.pl.layout

import java.util.TreeMap
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.layout.AnchorPane
import mu.KLogging
import sp.it.pl.main.APP
import sp.it.pl.ui.objects.window.stage.asAppWindow
import sp.it.util.collections.materialize
import sp.it.util.conf.Config
import sp.it.util.conf.ConfigDelegator
import sp.it.util.conf.ConfigValueSource
import sp.it.util.conf.Configurable
import sp.it.util.conf.EditMode
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.cvro
import sp.it.util.conf.def
import sp.it.util.conf.noPersist
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.recurseBF
import sp.it.util.functional.toUnit
import sp.it.util.reactive.attach

/**
 * Component able to store other Components.
 *
 * The key element for layouts and their modularity.
 *
 * Containers are components storing their children and with layout-defining
 * behavior such as loading itself and its content and supporting layout
 * operations requiring the awareness of the component within layout hierarchy.
 *
 * Containers are not graphical components, Containers wrap them. This creates
 * an abstraction layer that allows for defining layout hierarchy - layout maps
 * separately from scene-graph - the graphical hierarchy.
 * Layout mapB is complete hierarchical structure of Components spanning from
 * single Container called root.
 *
 * Containers need to be lightweight wrappers able to be serialized so its layout
 * mapB can be later be reconstructed during/by deserialization.
 *
 * The children of the container are indexed in order to identify their position
 * within the container. How the indexes are interpreted is left up on the container's
 * implementation logic. The children collection is implemented as `Map<Integer,Component>`.
 *
 * Container is pure, if it can contain only containers.
 * Container is leaf, if it can contain only non-containers.
 * Note the difference between containing and able to contain! The pure and leaf
 * containers can have their own class implementation.
 *
 * Container implementation (extending class) must handle
 * - adding the child to its child mapB (includes the index interpretation)
 * - removing previously assigned children
 * - reload itself so the layout change transforms into graphical change.
 * NOTE: invalid index (for example out of range) must be ignored for some
 * behavior to work correctly.This is because indexOf() method returns invalid (but still number)
 * index if component is not found. Therefore, such index must be ignored.
 */
sealed class Container<G: ComponentUi?>(state: ComponentDb): Component(state), Configurable<Any?>, ConfigDelegator, AltState {

   /**
    * Root of this container. The container is attached to the scene
    * graph through this root. The root is parent node of all the nodes of
    * this container (including its children).
    */
   var root: AnchorPane? = null
      private set

   /** Ui. */
   @JvmField
   var ui: G? = null

   /**
    * The children mapped by unique index.
    * The index can be any number. The natural order of the component is computed from its index using [validChildIndexOrder].
    * The map is sorted by [validChildIndexOrder] ASC, thus its iteration order respects natural component order. */
   val children = TreeMap<Int, Component?> { a, b -> validChildIndexOrder(a) compareTo validChildIndexOrder(b) }

   private var isClosed = false
   private val configs = HashMap<String, Config<Any?>>()

   override val configurableGroupPrefix = null

   override val configurableValueSource = object: ConfigValueSource {
      override fun register(config: Config<*>) = configs.put(config.name, config.asIs()).toUnit()
      override fun initialize(config: Config<*>) {}
   }

   override fun getConfig(name: String) = configs[name]

   override fun getConfigs() = configs.values.toList()

   /** Content padding or null if left up on skin to decide. */
   val padding by cvn<Insets>(null).def(name = "Padding", info = "Content padding or null if left up on skin to decide`. ")

   /** Whether this container loads eagerly or lazily by user. */
   val loadTypeCfg by cv(loadType).noPersist().def(name = "Load type", info = "Whether this container loads eagerly or lazily by user")

   /** Whether this container or any parent is locked. */
   val lockedCfg by cv(locked).noPersist().def(name = "Locked", info = "Whether this container is locked")

   /** Whether this container or any parent is locked. */
   val lockedUnderCfg by cvro(lockedUnder).noPersist().def(name = "Locked (under)", info = "Whether this container or any parent is locked", editable = EditMode.APP)

   init {
      padding.value = state.properties["padding"].asIf<Insets>()
      padding attach { properties["padding"] = it }
   }

   /**
    * Sets this as [parent] for all [children] recursively.
    * Required after deserialization, call just before the children are loaded.
    */
   protected fun setParentRec() {
      for (c in children.values) {
         if (c is Container<*>) {
            c.parent = this
            c.setParentRec()
         }
      }
   }

   /**
    * Adds specified component to specified index as child of the container.
    * @param index index of a child. Determines its position within container. Null value allowed, but will be ignored.
    * @param c component to add. Null removes existing component from given index (see [removeChild])
    */
   abstract fun addChild(index: Int?, c: Component?)

   /**
    * Removes child of this container if it exists.
    * @param c component to remove
    */
   fun removeChild(c: Component) {
      val i = indexOf(c)
      if (i!=null) {
         addChild(i, null)
         c.close()
      }
   }

   /**
    * Removes child of this container at specified index and invokes its [Component.close].
    *
    * Similar to `addChild(index, null)` but calls close on the removed child.
    * @param index of the child to remove. Null is ignored.
    */
   open fun removeChild(index: Int?) {
      if (index!=null) {
         val c = children[index]
         addChild(index, null)
         c?.close()
         closeWindowIfEmpty()
      }
   }

   /**
    * Swaps children in the layout.
    *
    * @param i1 index of the child of this container to swap.
    * @param toParent container containing the child to swap with
    * @param toChild child to swap with
    */
   @Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS")
   fun swapChildren(toParent: Container<*>?, i1: Int?, toChild: Component?) {
      val c1: Container<*> = this
      if (toParent==null || i1==null) return
      val w1 = c1.children[i1]
      val w2 = toChild ?: return
      val i2 = toParent.indexOf(w2)
      val w1n = w1?.name ?: "null"
      val w2n = w2?.name ?: "null"
      logger.info("Swapping components {} and {}", w1n, w2n)
      c1.addChild(i1, w2)
      toParent.addChild(i2, w1)
      c1.closeWindowIfEmpty()
      toParent.closeWindowIfEmpty()
   }

   protected val isEmptyForCloseWindowIfEmpty: Boolean
      get() = getAllWidgets().count()==0 && getAllContainers(true).none { it.properties.keys.stream().noneMatch { it.startsWith("reloading=") } }

   protected fun closeWindowIfEmpty() {
      val r = rootParent
      val rIsEmpty = r?.isEmptyForCloseWindowIfEmpty==true
      val w = r?.window?.asAppWindow()
      val wIsEmpty = w?.layout?.isEmptyForCloseWindowIfEmpty==true
      if (APP.windowManager.windowDisallowEmpty.value && rIsEmpty && wIsEmpty) w?.hide()
   }

   /**
    * Returns index of a child or null if no child or parameter null.
    * @param c component
    * @return index of a child or null if no child
    */
   fun indexOf(c: Component?): Int? {
      if (c==null) return null
      for ((key, value) in children)
         if (value===c) return key
      return null
   }

   /** @return potentially infinite sequence of all possible child indexes in descending order by ui priority */
   abstract fun validChildIndexes(): Sequence<Int>

   /** @return natural order of the child component index in [validChildIndexes] */
   abstract fun validChildIndexOrder(index: Int): Int

   /** @return available index for child or null if none available. */
   open fun getEmptySpot(): Int? = validChildIndexes().firstOrNull { it !in children }

   /** @return all child components in breadth-first order. This container is the first element. */
   fun getAllChildren(): Sequence<Component> = asIs<Component>()
      .recurseBF {
         when (it) {
            is Container<*> -> it.children.values.asSequence().filterNotNull().asIterable()
            is Widget -> listOf()
         }
      }

   /** @return all child widgets in breadth-first order. */
   fun getAllWidgets(): Sequence<Widget> = getAllChildren().filterIsInstance<Widget>()

   /** @return all child containers in breadth-first order. This container is the first element if [includeSelf] is true. */
   fun getAllContainers(includeSelf: Boolean): Sequence<Container<*>> = getAllChildren().filterIsInstance<Container<*>>().drop(if (includeSelf) 0 else 1)

   /**
    * Loads the graphical element this container wraps. Furthermore, all the children
    * get loaded too.
    * Use for as the first load of the controller to assign the parent_pane.
    * Here, the term parent is not parent Container, but instead the very AnchorPane
    * this container will be loaded into.
    *
    * @return the result of the call to [.load]
    */
   open fun load(parentPane: AnchorPane?): Node {
      this.root = parentPane
      return load()
   }

   /**
    * Effectively reload.
    * Loads the whole container and its children - the whole layout sub-branch
    * having this container as root - to its parent_pane. The parent_pane must be assigned
    * before calling this method.
    * {@inheritDoc}
    */
   abstract override fun load(): Node

   override fun focus() {
      children.entries.asSequence().mapNotNull { it.value }.firstOrNull()?.focus()
   }

   override fun close() {
      if (isClosed) return
      isClosed = true
      super.close()

      getAllWidgets().materialize().forEach { it.close() }
      if (parent!=null) {
         parent!!.removeChild(this)
         removeGraphicsFromSceneGraph()
      } else {
         children.keys.materialize().forEach { removeChild(it) }
      }
      // free resources of all guis, we need to do this because we do not
      // close the sub containers, they can not even override this method to
      // implement their own implementation because it will not be invoked
      getAllContainers(false).forEach { it.disposeGraphics() }
   }

   private fun removeGraphicsFromSceneGraph() {
      ui.ifNotNull {
         root!!.children.remove(it.root)
      }
   }

   private fun disposeGraphics() {
      ui?.dispose()
   }

   override fun show() {
      ui?.show()
      children.values.asSequence().filterIsInstance<AltState>().forEach { it.show() }
   }

   override fun hide() {
      ui?.hide()
      children.values.asSequence().filterIsInstance<AltState>().forEach { it.hide() }
   }

   protected fun setChildrenParents() {
      children.values.forEach { if (it is Container<*>) it.parent = this }
   }

   companion object: KLogging()
}