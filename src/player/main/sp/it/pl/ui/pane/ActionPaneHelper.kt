package sp.it.pl.ui.pane

import de.jensd.fx.glyphs.GlyphIcons
import javafx.event.ActionEvent
import javafx.event.Event
import javafx.scene.Node
import javafx.scene.control.MenuItem
import javafx.stage.Window
import kotlin.reflect.KClass
import sp.it.pl.main.APP
import sp.it.pl.main.toUi
import sp.it.pl.ui.pane.ActionData.GroupApply
import sp.it.pl.ui.pane.ActionData.GroupApply.FOR_ALL
import sp.it.pl.ui.pane.ActionData.GroupApply.FOR_EACH
import sp.it.pl.ui.pane.ActionData.GroupApply.NONE
import sp.it.pl.ui.pane.ActionData.Threading
import sp.it.pl.ui.pane.ActionData.Threading.BLOCK
import sp.it.pl.ui.pane.ActionData.Threading.UI
import sp.it.util.action.Action
import sp.it.util.async.FX
import sp.it.util.async.future.Fut
import sp.it.util.async.future.Fut.Companion.futOfBlock
import sp.it.util.async.runIO
import sp.it.util.collections.collectionWrap
import sp.it.util.collections.getElementClass
import sp.it.util.dev.Blocks
import sp.it.util.dev.fail
import sp.it.util.dev.failIfFxThread
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.functional.Try
import sp.it.util.functional.Util.IS
import sp.it.util.functional.asIf
import sp.it.util.functional.runTry
import sp.it.util.type.VType
import sp.it.util.type.raw
import sp.it.util.type.type

inline fun <reified T> ActionPane.register(vararg actions: ActionData<T, *>) = register(T::class, *actions)

fun getUnwrappedType(d: Any?): KClass<*> = when (d) {
   null -> Nothing::class
   is Collection<*> -> d.getElementClass().kotlin
   else -> d::class
}

fun futureUnwrap(o: Any?): Any? = when (o) {
   is Fut<*> -> when {
      o.isDone() -> {
         when (val result = o.getDone()) {
            is Fut.Result.ResultOk<*> -> result.value
            else -> null
         }
      }
      else -> o
   }
   else -> o
}

fun futureUnwrapOrThrow(o: Any?): Any? = when (o) {
   is Fut<*> -> when {
      o.isDone() -> {
         when (val result = o.getDone()) {
            is Fut.Result.ResultOk<*> -> result.value
            else -> null
         }
      }
      else -> fail { "Future not done yet" }
   }
   else -> o
}

private typealias Test<T> = (T) -> Boolean
private typealias Act<T> = ActContext.(T) -> Any?

data class ActContext(val window: Window?, val a: ActionPane?,  val node: Node?, val event: Event?) {
   constructor(window: Window): this(window, null, null, null)
   constructor(node: Node): this(node.scene?.window, null, node, null)
   constructor(event: ActionEvent): this(event.source.asIf<MenuItem>()?.parentPopup?.ownerWindow, null, event.source.asIf<MenuItem>()?.parentPopup?.ownerNode, event)

   val apOrApp: ActionPane get() = a ?: APP.ui.actionPane.orBuild
}

class ComplexActionData<R, T> {
   @JvmField val gui: (T) -> Node
   @JvmField val input: (R) -> Any

   constructor(input: (R) -> Fut<T>, gui: (T) -> Node) {
      this.gui = gui
      this.input = input
   }
}

/** Action. */
class ActionData<C, T>(name: String, type: VType<T>, description: String, icon: GlyphIcons, groupApply: GroupApply, condition: Test<T>, isLong: Boolean, action: Act<T>) {

   @JvmField val type: VType<T> = type
             val hasInput: Boolean get() = type.raw!=Unit::class
   @JvmField val name: String = name
             val nameWithDots get() = if (hasInput) "${type.toUi()}.${name.dropLastWhile { it=='.' }}..." else name
   @JvmField val description: String = description
   @JvmField val icon: GlyphIcons = icon
   @JvmField val groupApply: GroupApply = groupApply
   @JvmField val condition: Test<T> = condition
   @JvmField val isLong: Boolean = isLong
   @JvmField val action: Act<T> = action

   @JvmField var isComplex = false
   @JvmField var preventClosing = false
   @JvmField var complexData: ((ActionPane) -> ComplexActionData<T, *>)? = null

   fun preventClosing() = apply {
      preventClosing = true
   }

   fun preventClosing(action: (ActionPane) -> ComplexActionData<T, *>) = apply {
      isComplex = true
      complexData = action
      preventClosing = true
   }

   @Suppress("UNCHECKED_CAST")
   fun invokeIsDoable(data: Any?): Boolean {
      return when (groupApply) {
         FOR_ALL -> condition(collectionWrap(data) as T)
         FOR_EACH -> when (data) {
            is Collection<*> -> (data as Collection<T>).all(condition)
            else -> condition(data as T)
         }
         NONE -> data !is Collection<*> && condition(data as T)
      }
   }

   @Suppress("UNCHECKED_CAST")
   @Blocks
   @Throws(Throwable::class)
   operator fun invoke(context: ActContext, data: Any?): Any? {
      if (isLong) failIfFxThread()

      return when (groupApply) {
         FOR_ALL -> context.action(collectionWrap(data) as T)
         FOR_EACH -> when (data) {
            is Collection<*> -> (data as Collection<T>).map { context.action(it) }
            else -> context.action(data as T)
         }
         NONE -> context.action(data as T)
      }
   }

   @Blocks
   fun invokeTry(context: ActContext, data: Any?): Try<Any?, Throwable> {
      return runTry { invoke(context, data) }.mapError { RuntimeException("Running action=$name failed", it) }
   }

   fun invokeFut(context: ActContext, data: Any?): Fut<*> {
      failIfNotFxThread()

      return if (isLong) runIO { invokeTry(context, data).orThrow }
      else futOfBlock { invokeTry(context, data).orThrow }
   }

   fun invokeFutAndProcess(context: ActContext, data: Any?) {
      failIfNotFxThread()

      invokeFut(context, data).onDone(FX) {
         it.toTryRaw()
            .ifError { context.apOrApp.show(it) }
            .ifOk { if (!isResultUnit(it)) context.apOrApp.show(it) }
      }
   }

   fun isResultUnit(result: Any?): Boolean {
      return when (groupApply) {
         FOR_ALL, NONE -> when {
            result is Unit -> true
            else -> false
         }
         FOR_EACH -> when {
            result is Unit -> true
            result is Collection<*> && result.all { it==Unit } -> true
            else -> false
         }
      }
   }

   @Suppress("UNCHECKED_CAST")
   fun prepInputExact(data: Any?): T = prepInput(data) as T

   fun prepInput(data: Any?): Any? = when (groupApply) {
      FOR_ALL -> collectionWrap(data)
      FOR_EACH -> fail { "Action with $groupApply should never get here" }
      NONE -> data
   }

   override fun toString() = "ActionData(\"$name\")"

   enum class GroupApply {
      FOR_EACH, FOR_ALL, NONE
   }
   
   enum class Threading {
      UI, BLOCK
   }
}

/** [ActionData] that executes synchronously - simply consumes the input. */
inline fun <C, reified T> actionBase(name: String, description: String, icon: GlyphIcons, groupApply: GroupApply, threading: Threading = UI, noinline constriction: Test<T>, noinline action: Act<T>) = ActionData<C, T>(name, type(), description, icon, groupApply, constriction, threading==BLOCK, action)

/** [action] that consumes simple input - its type is the same as type of the action. */
inline fun <reified T> action(name: String, description: String, icon: GlyphIcons, threading: Threading = UI, noinline action: Act<T>) = actionBase<T, T>(name, description, icon, NONE, threading, IS, action)

/** [action] that consumes simple input - its type is the same as type of the action. */
inline fun <reified T> action(name: String, description: String, icon: GlyphIcons, threading: Threading = UI, noinline constriction: Test<T>, noinline action: Act<T>) = actionBase<T, T>(name, description, icon, NONE, threading, constriction, action)

/** [action] that consumes simple input - its type is the same as type of the action. */
inline fun <reified T> action(icon: GlyphIcons, action: Action) = actionBase<T, T>(action.name, action.info + if (action.hasKeysAssigned()) "\n\nShortcut keys: ${action.keys}" else "", icon, NONE, UI, IS, { action.run() })

/** [action] that consumes collection input - its input type is collection of its type. */
inline fun <reified T> actionAll(name: String, description: String, icon: GlyphIcons, threading: Threading = UI, noinline action: Act<Collection<T>>) = actionBase<T, Collection<T>>(name, description, icon, FOR_ALL, threading, IS, action)

/** [action] that consumes collection input - its input type is collection of its type. */
inline fun <reified T> actionAll(name: String, description: String, icon: GlyphIcons, threading: Threading = UI, crossinline constriction: Test<T>, noinline action: Act<Collection<T>>) = actionBase<T, Collection<T>>(name, description, icon, FOR_ALL, threading, { it.all(constriction) }, action)
