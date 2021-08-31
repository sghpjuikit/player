package sp.it.pl.ui.pane

import de.jensd.fx.glyphs.GlyphIcons
import javafx.scene.Node
import kotlin.reflect.KClass
import sp.it.pl.ui.pane.GroupApply.FOR_ALL
import sp.it.pl.ui.pane.GroupApply.FOR_EACH
import sp.it.pl.ui.pane.GroupApply.NONE
import sp.it.util.action.Action
import sp.it.util.async.future.Fut
import sp.it.util.collections.collectionWrap
import sp.it.util.collections.getElementClass
import sp.it.util.dev.fail
import sp.it.util.functional.Util.IS
import sp.it.util.functional.Util.ISNT
import sp.it.util.type.VType
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
private typealias Act<T> = (T) -> Unit

interface ConvertingConsumer<T>: Act<T>

class ComplexActionData<R, T> {
   @JvmField val gui: (T) -> Node
   @JvmField val input: (R) -> Any

   constructor(input: (R) -> Fut<T>, gui: (T) -> Node) {
      this.gui = gui
      this.input = input
   }

}

/** Action. */
class ActionData<C, T> {

   @JvmField val name: String
   @JvmField val type: VType<T>
   @JvmField val description: String
   @JvmField val icon: GlyphIcons
   @JvmField val groupApply: GroupApply
   @JvmField val condition: Test<T>
   @JvmField val isLong: Boolean
   @JvmField val action: Act<T>

   @JvmField var isComplex = false
   @JvmField var preventClosing = false
   @JvmField var complexData: ((ActionPane) -> ComplexActionData<T, *>)? = null

   constructor(name: String, type: VType<T>, description: String, icon: GlyphIcons, groupApply: GroupApply, condition: Test<T>, isLong: Boolean, action: Act<T>) {
      this.name = name
      this.type = type
      this.description = description
      this.icon = icon
      this.groupApply = groupApply
      this.condition = condition
      this.isLong = isLong
      this.action = action
      if (action is ConvertingConsumer<*>) preventClosing = true
   }

   fun preventClosing() = apply {
      preventClosing = true
   }

   fun preventClosing(action: (ActionPane) -> ComplexActionData<T, *>) = apply {
      isComplex = true
      complexData = action
      preventClosing = true
   }

   @Suppress("UNCHECKED_CAST")
   operator fun invoke(data: Any?) {
      when (groupApply) {
         FOR_ALL -> action(collectionWrap(data) as T)
         FOR_EACH -> {
            if (data is Collection<*>) {
               (data as Collection<T>).forEach(action)
            } else {
               action(data as T)
            }
         }
         NONE -> {
            if (data is Collection<*>) fail { "Action with $groupApply can not use collection" }
            action(data as T)
         }
      }
   }

   @Suppress("UNCHECKED_CAST")
   fun prepInputExact(data: Any?): T = prepInput(data) as T

   fun prepInput(data: Any?): Any? = when (groupApply) {
      FOR_ALL -> collectionWrap(data)
      FOR_EACH -> fail { "Action with $groupApply should never get here" }
      NONE -> {
         if (data is Collection<*>) fail { "Action with $groupApply can not use collection" }
         data
      }
   }

   override fun toString() = "ActionData(\"$name\")"
}

/** [ActionData] that executes synchronously - simply consumes the input. */
inline fun <C, reified T> FastActionBase(name: String, description: String, icon: GlyphIcons, groupApply: GroupApply, noinline constriction: Test<T>, noinline action: Act<T>) = ActionData<C, T>(name, type<T>(), description, icon, groupApply, constriction, false, action)

/** [FastAction] that consumes simple input - its type is the same as type of the action. */
inline fun <reified T> FastAction(name: String, description: String, icon: GlyphIcons, noinline action: Act<T>) = FastActionBase<T, T>(name, description, icon, NONE, IS, action)

/** [FastAction] that consumes simple input - its type is the same as type of the action. */
inline fun <reified T> FastAction(name: String, description: String, icon: GlyphIcons, noinline constriction: Test<T>, noinline action: Act<T>) = FastActionBase<T, T>(name, description, icon, NONE, constriction, action)

/** [FastAction] that consumes simple input - its type is the same as type of the action. */
inline fun <reified T> FastAction(icon: GlyphIcons, action: Action) = FastActionBase<T, T>(action.name, action.info + if (action.hasKeysAssigned()) "\n\nShortcut keys: ${action.keys}" else "", icon, NONE, IS, { action.run() })

/** [FastAction] that consumes collection input - its input type is collection of its type. */
inline fun <reified T> FastColAction(name: String, description: String, icon: GlyphIcons, noinline action: Act<Collection<T>>) = FastActionBase<T, Collection<T>>(name, description, icon, FOR_ALL, ISNT, action)

/** [FastAction] that consumes collection input - its input type is collection of its type. */
inline fun <reified T> FastColAction(name: String, description: String, icon: GlyphIcons, crossinline constriction: Test<T>, noinline action: Act<Collection<T>>) = FastActionBase<T, Collection<T>>(name, description, icon, FOR_ALL, { it.none(constriction) }, action)

/** [ActionData] that executes asynchronously - receives a future, processes the data and returns it. */
inline fun <C, reified T> SlowActionBase(name: String, description: String, icon: GlyphIcons, groupApply: GroupApply, noinline constriction: Test<T>, noinline action: Act<T>) = ActionData<C, T>(name, type<T>(), description, icon, groupApply, constriction, true, action)

/** [SlowActionBase] that processes simple input - its type is the same as type of the action. */
inline fun <reified T> SlowAction(name: String, description: String, icon: GlyphIcons, noinline action: Act<T>) = SlowActionBase<T, T>(name, description, icon, NONE, IS, action)

/** [SlowActionBase] that processes simple input - its type is the same as type of the action. */
inline fun <reified T> SlowAction(name: String, description: String, icon: GlyphIcons, groupApply: GroupApply, noinline action: Act<T>) = SlowActionBase<T, T>(name, description, icon, groupApply, IS, action)

/** [SlowActionBase] that processes simple input - its type is the same as type of the action. */
inline fun <reified T> SlowAction(name: String, description: String, icon: GlyphIcons, noinline constriction: Test<T>, noinline action: Act<T>) = SlowActionBase<T, T>(name, description, icon, NONE, constriction, action)

/** [SlowActionBase] that processes collection input - its input type is collection of its type. */
inline fun <reified T> SlowColAction(name: String, description: String, icon: GlyphIcons, noinline action: Act<Collection<T>>) = SlowActionBase<T, Collection<T>>(name, description, icon, FOR_ALL, ISNT, action)

/** [SlowActionBase] that processes collection input - its input type is collection of its type. */
inline fun <reified T> SlowColAction(name: String, description: String, icon: GlyphIcons, crossinline constriction: Test<T>, noinline action: Act<Collection<T>>) = SlowActionBase<T, Collection<T>>(name, description, icon, FOR_ALL, { it.none(constriction) }, action)

enum class GroupApply {
   FOR_EACH, FOR_ALL, NONE
}