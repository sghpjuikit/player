@file:Suppress("FINAL_UPPER_BOUND")

package sp.it.util.reactive

import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.ListView
import javafx.scene.control.Menu
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.stage.Window
import sp.it.util.system.Os

/** Key for [KeyEvent.isShortcutDown]. */
val SHORTCUT: KeyCode = if (Os.OSX.isCurrent) KeyCode.META else KeyCode.CONTROL

/** Equivalent to [Window.addEventHandler]. */
fun <T: Event> Window.onEventDown(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
   val handler = eventHandler.eh
   addEventHandler(eventType, handler)
   return Subscription { removeEventHandler(eventType, handler) }
}

/** Equivalent to [Window.addEventHandler], but the handler runs at most once. */
fun <T: Event> Window.onEventDown1(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
   val handler = object: EventHandler<T> {
      override fun handle(event: T) {
         eventHandler(event)
         removeEventHandler(eventType, this)
      }
   }
   addEventHandler(eventType, handler)
   return Subscription { removeEventHandler(eventType, handler) }
}

/** Equivalent to [Window.addEventFilter]. */
fun <T: Event> Window.onEventUp(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
   val handler = eventHandler.eh
   addEventFilter(eventType, handler)
   return Subscription { removeEventFilter(eventType, handler) }
}

/** Equivalent to [Window.addEventFilter], but the handler runs at most once. */
fun <T: Event> Window.onEventUp1(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
   val handler = object: EventHandler<T> {
      override fun handle(event: T) {
         eventHandler(event)
         removeEventFilter(eventType, this)
      }
   }
   addEventHandler(eventType, handler)
   return Subscription { removeEventFilter(eventType, handler) }
}

/** Equivalent to [Scene.addEventHandler]. */
fun <T: Event> Scene.onEventDown(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
   val handler = eventHandler.eh
   addEventHandler(eventType, handler)
   return Subscription { removeEventHandler(eventType, handler) }
}

/** Equivalent to [Scene.addEventHandler], but the handler runs at most once. */
fun <T: Event> Scene.onEventDown1(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
   val handler = object: EventHandler<T> {
      override fun handle(event: T) {
         eventHandler(event)
         removeEventHandler(eventType, this)
      }
   }
   addEventHandler(eventType, handler)
   return Subscription { removeEventHandler(eventType, handler) }
}

/** Equivalent to [Scene.addEventFilter]. */
fun <T: Event> Scene.onEventUp(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
   val handler = eventHandler.eh
   addEventFilter(eventType, handler)
   return Subscription { removeEventFilter(eventType, handler) }
}

/** Equivalent to [Scene.addEventFilter], but the handler runs at most once. */
fun <T: Event> Scene.onEventUp1(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
   val handler = object: EventHandler<T> {
      override fun handle(event: T) {
         eventHandler(event)
         removeEventFilter(eventType, this)
      }
   }
   addEventHandler(eventType, handler)
   return Subscription { removeEventFilter(eventType, handler) }
}

/** Equivalent to [Node.addEventFilter]. */
fun <T: MouseEvent> Scene.onEventDown(eventType: EventType<T>, button: MouseButton, consume: Boolean = true, eventHandler: (T) -> Unit) = onEventDown(eventType) {
   if (it.button==button) {
      eventHandler(it)
      if (consume) it.consume()
   }
}

fun <T: KeyEvent> Scene.onEventDown(eventType: EventType<T>, key: KeyCode, consume: Boolean = true, eventHandler: (T) -> Unit) = onEventDown(eventType, null, key, consume, eventHandler)

fun <T: KeyEvent> Scene.onEventDown(eventType: EventType<T>, modifier: KeyCode? = null, key: KeyCode, consume: Boolean = true, eventHandler: (T) -> Unit) = onEventDown(eventType) {
   val modifierMatches = modifier==null || when {
      it.isAltDown -> modifier==KeyCode.ALT
      it.isControlDown -> modifier==KeyCode.CONTROL
      it.isMetaDown -> modifier==KeyCode.META
      it.isShiftDown -> modifier==KeyCode.SHIFT
      it.isShortcutDown -> modifier==SHORTCUT
      else -> false
   }
   if (modifierMatches && it.code==key) {
      eventHandler(it)
      if (consume) it.consume()
   }
}

/** Equivalent to [Node.addEventHandler]. */
fun <T: Event> Node.onEventDown(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
   val handler = eventHandler.eh
   addEventHandler(eventType, handler)
   return Subscription { removeEventHandler(eventType, handler) }
}


/** Equivalent to [Node.addEventFilter]. */
fun <T: MouseEvent> Node.onEventDown(eventType: EventType<T>, button: MouseButton, consume: Boolean = true, eventHandler: (T) -> Unit) = onEventDown(eventType) {
   if (it.button==button) {
      eventHandler(it)
      if (consume) it.consume()
   }
}

fun <T: KeyEvent> Node.onEventDown(eventType: EventType<T>, key: KeyCode, consume: Boolean = true, eventHandler: (T) -> Unit) = onEventDown(eventType, null, key, consume, eventHandler)

fun <T: KeyEvent> Node.onEventDown(eventType: EventType<T>, modifier: KeyCode? = null, key: KeyCode, consume: Boolean = true, eventHandler: (T) -> Unit) = onEventDown(eventType) {
   val modifierMatches = modifier==null || when {
      it.isAltDown -> modifier==KeyCode.ALT
      it.isControlDown -> modifier==KeyCode.CONTROL
      it.isMetaDown -> modifier==KeyCode.META
      it.isShiftDown -> modifier==KeyCode.SHIFT
      it.isShortcutDown -> modifier==SHORTCUT
      else -> false
   }
   if (modifierMatches && it.code==key) {
      eventHandler(it)
      if (consume) it.consume()
   }
}

/** Equivalent to [Node.addEventFilter]. */
fun <T: Event> Node.onEventUp(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
   val handler = eventHandler.eh
   addEventFilter(eventType, handler)
   return Subscription { removeEventFilter(eventType, handler) }
}

/** Equivalent to [Node.addEventFilter]. */
fun <T: MouseEvent> Node.onEventUp(eventType: EventType<T>, button: MouseButton, consume: Boolean = true, eventHandler: (T) -> Unit) = onEventUp(eventType) {
   if (it.button==button) {
      eventHandler(it)
      if (consume) it.consume()
   }
}

fun <T: KeyEvent> Node.onEventUp(eventType: EventType<T>, key: KeyCode, consume: Boolean = true, eventHandler: (T) -> Unit) = onEventUp(eventType, null, key, consume, eventHandler)

fun <T: KeyEvent> Node.onEventUp(eventType: EventType<T>, modifier: KeyCode? = null, key: KeyCode, consume: Boolean = true, eventHandler: (T) -> Unit) = onEventUp(eventType) {
   val modifierMatches = modifier==null || when {
      it.isAltDown -> modifier==KeyCode.ALT
      it.isControlDown -> modifier==KeyCode.CONTROL
      it.isMetaDown -> modifier==KeyCode.META
      it.isShiftDown -> modifier==KeyCode.SHIFT
      it.isShortcutDown -> modifier==SHORTCUT
      else -> false
   }
   if (modifierMatches && it.code==key) {
      eventHandler(it)
      if (consume) it.consume()
   }
}

/** Equivalent to [TreeItem.addEventHandler]. */
fun <R, T: Event> TreeItem<R>.onEventDown(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
   val handler = eventHandler.eh
   addEventHandler(eventType, handler)
   return Subscription { removeEventHandler(eventType, handler) }
}

/** Equivalent to [Menu.addEventHandler]. */
fun <T: Event> Menu.onEventDown(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
   val handler = eventHandler.eh
   addEventHandler(eventType, handler)
   return Subscription { removeEventHandler(eventType, handler) }
}

/** Consume [ScrollEvent.ANY] events, e.g. to prevent certain scrollable JavaFx controls (table, tree, list, ...) from consuming non-deterministically. */
fun Node.consumeScrolling() = onEventDown(ScrollEvent.ANY) { if (!it.isShortcutDown) it.consume() }

/** Re-fire [KeyCode.ESCAPE] key events, e.g. to bypass consuming, which for example [TreeView] does by default. */
fun ListView<*>.propagateESCAPE() = onEventDown(KeyEvent.ANY) {
   if (editingIndex==-1 && it.code==KeyCode.ESCAPE) {
      parent?.fireEvent(it)
      it.consume()
   }
}

/** Re-fire [KeyCode.ESCAPE] key events, e.g. to bypass consuming, which for example [TreeView] does by default. */
fun TreeView<*>.propagateESCAPE() = onEventDown(KeyEvent.ANY) {
   if (editingItem==null && it.code==KeyCode.ESCAPE) {
      parent?.fireEvent(it)
      it.consume()
   }
}

private val <T: Event> ((T) -> Unit).eh: EventHandler<T> get() = EventHandler { this(it) }