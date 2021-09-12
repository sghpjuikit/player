package sp.it.pl.layout

import java.util.UUID
import javafx.beans.InvalidationListener
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.Node
import sp.it.pl.layout.Widget.LoadType.AUTOMATIC
import sp.it.pl.main.APP
import sp.it.util.access.V
import sp.it.util.access.v
import sp.it.util.dev.fail
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on

/**
 * Defines wrapper of loadable graphical component.
 * Basis for wrappers - containers or wrapped widgets.
 */
sealed class Component(state: ComponentDb) {

   /** Unique ID. Permanent. Persists application life cycle. */
   val id: UUID
   /** Simple storage for component state. Persists application life cycle. */
   val properties = HashMap<String, Any?>()
   /** Name. */
   abstract val name: String
   /** Denotes whether component loading is delayed until user manually requests it. */
   val loadType = V(AUTOMATIC)
   /** Whether this component is locked - prevents user from editing certain properties from ui. See [lockedUnder]. */
   val locked: BooleanProperty = SimpleBooleanProperty(false)
   /** Whether this component or any of its [parent]s is [locked] */
   val lockedUnder = LockedProperty()

   /** Parent container. Root component has no parent. Every (loaded) component aside from root must have a parent. */
   var parent: Container<*>? = null
      set(value) {
         field = value
         lockedUnder.init(value)
      }

   init {
      lockedUnder.init(null)
   }

   /** Whether this component is at the top of the hierarchy. True if not loaded or [parent] is null. */
   val isRoot: Boolean
      get() = parent==null

   /** Top level parent - root of the hierarchy. [Container] can return self, [Widget] can not. */
   val rootParent: Container<*>?
      get() {
         val parent = parent
         return when {
            parent!=null -> parent.rootParent
            this is Container<*> -> this
            else -> null
         }
      }

   /** Window containing this component or null if not loaded or not in any window. */
   val window: javafx.stage.Window?
      get() = when(this) {
         is Container<*> -> root?.scene?.window
         is Widget -> graphics?.scene?.window
         else -> fail()
      }

   init {
      id = state.id
      properties.putAll(state.properties)
      locked.value = state.locked
      loadType.value = state.loading
   }

   /**
    * Loads the graphical element this container wraps.
    *
    * @return root node of the loaded container
    */
   abstract fun load(): Node

   abstract fun focus()

   /**
    * Removes this component from component graph (layout) and scene graph. Can not be undone.
    * This method is called for every child component (in any depth).
    * If this container is [sp.it.pl.layout.Layout], only its children will close.
    */
   open fun close() {
      lockedUnder.dispose()
   }

   /**
    * Equivalent to null-safe version of: getParent().indexOf(this)
    *
    * @return parent.indexOf(this) or null if no parent
    */
   fun indexInParent(): Int? = parent?.indexOf(this)

   fun swapWith(c: Container<*>?, i: Int) {
      c?.swapChildren(parent, i, this)
   }

   abstract fun toDb(): ComponentDb

   inner class LockedProperty: ObservableValue<Boolean> {
      private val v = v(false)
      private val disposer = Disposer()

      override fun addListener(listener: ChangeListener<in Boolean>) = v.addListener(listener)
      override fun addListener(listener: InvalidationListener) = v.addListener(listener)
      override fun removeListener(listener: ChangeListener<in Boolean>) = v.removeListener(listener)
      override fun removeListener(listener: InvalidationListener) = v.removeListener(listener)
      override fun getValue() = v.value

      // call when component parent changes
      fun init(newParent: Container<*>?) {
         disposer()
         val locks = listOfNotNull(newParent?.lockedUnder, locked, APP.ui.layoutLocked)
         locks.forEach {
            it attach { v.value = locks.any { it.value } } on disposer
         }
         v.value = locks.any { it.value }
      }

      // call when closing component
      fun dispose(): Unit = disposer()
   }

}