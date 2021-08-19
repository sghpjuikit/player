package sp.it.pl.layout

import java.util.function.Consumer
import javafx.scene.Node
import javafx.scene.layout.AnchorPane
import org.slf4j.LoggerFactory
import sp.it.pl.main.APP
import sp.it.pl.ui.objects.window.stage.asAppWindow
import sp.it.util.collections.materialize
import sp.it.util.conf.Config
import sp.it.util.conf.ConfigDelegator
import sp.it.util.conf.ConfigValueSource
import sp.it.util.conf.Configurable
import sp.it.util.functional.Util
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.recurseBF
import sp.it.util.functional.toUnit

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
 * Container is called pure, if it can contain only containers.
 * Container is called leaf, if it can contain only non-containers.
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

   /** The children mapped by unique index. */
   val children = HashMap<Int, Component?>()

   private val configs = HashMap<String, Config<Any?>>()

   override val configurableGroupPrefix = null

   override val configurableValueSource = object: ConfigValueSource {
      override fun register(config: Config<*>) = configs.put(config.name, config.asIs()).toUnit()
      override fun initialize(config: Config<*>) {}
   }

   override fun getConfig(name: String) = configs[name]

   override fun getConfigs() = configs.values.toList()

   /*
     * Properly links up this container with its children and propagates this
     * call down on the children and so on.
     * This method is required to fully setParentRec the layout after deserialization
     * because some field values can not be serialized and need to be manually
     * initialized.
     * Use on layout reload, immediately after the container.load() method.
     */
   // TODO: remove
   fun setParentRec() {
      for (c in children.values) {
         if (c is Container<*>) {
            c.parent = this
            c.setParentRec()
         }
      }
   }

   // TODO: this should close previous component
   /**
    * Adds component to specified index as child of the container.
    * @param index index of a child. Determines its position within container.
    * Null value allowed, but will be ignored.
    * @param c component to add. Null removes existing component from given
    * index.
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
    * Removes child of this container at specified index.
    *
    *
    * Equivalent to: addChild(index, null);   // TODO: no it isnt! fix
    * @param index of the child to remove. Null is ignored.
    */
   open fun removeChild(index: Int?) {
      if (index!=null) {
         val c = children[index] // capture before reload
         addChild(index, null) // reload
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
      LoggerFactory.getLogger(Container::class.java).info("Swapping widgets {} and {}", w1n, w2n)
      c1.addChild(i1, w2)
      toParent.addChild(i2, w1)
      c1.closeWindowIfEmpty()
      toParent.closeWindowIfEmpty()
   }

   protected val isEmptyForCloseWindowIfEmpty: Boolean
      get() = getAllWidgets().count()==0 && getAllContainers(true).none { it.properties.keys.stream().noneMatch { it.startsWith("reloading=") } }

   protected fun closeWindowIfEmpty() {
      val rp = rootParent
      val isEmpty = rp!=null && rp.isEmptyForCloseWindowIfEmpty
      val w = rp?.window
      val aw = w?.asAppWindow()
      val awIsEmpty = aw!=null && aw.layout!=null && aw.layout.isEmptyForCloseWindowIfEmpty
      if (APP.windowManager.windowDisallowEmpty.value && isEmpty && awIsEmpty) aw!!.hide()
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

   /** @return potentially infinite sequence of potential child positions in descending order by ui priority */
   abstract fun validChildIndexes(): Sequence<Int>

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
      getAllWidgets().firstOrNull()?.focus()
   }

   // TODO: make sure widgets never close twice
   override fun close() {
      super.close()
      getAllWidgets().materialize().forEach { it.close() }
      if (parent!=null) {
         parent!!.removeChild(this) // remove from layout graph
         removeGraphicsFromSceneGraph() // remove from scene graph if attached to it
      } else {
         // remove all children
         Util.list(children.keys).forEach(Consumer { index: Int? -> this.removeChild(index) })
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
}