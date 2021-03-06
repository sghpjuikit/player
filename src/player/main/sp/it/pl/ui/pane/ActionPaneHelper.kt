package sp.it.pl.ui.pane

import de.jensd.fx.glyphs.GlyphIcons
import javafx.scene.Node
import sp.it.pl.ui.pane.GroupApply.FOR_ALL
import sp.it.pl.ui.pane.GroupApply.FOR_EACH
import sp.it.pl.ui.pane.GroupApply.NONE
import sp.it.util.action.Action
import sp.it.util.async.future.Fut
import sp.it.util.collections.collectionWrap
import sp.it.util.collections.getElementClass
import sp.it.util.collections.getElementType
import sp.it.util.dev.fail
import sp.it.util.functional.Util.IS
import sp.it.util.functional.Util.ISNT
import kotlin.reflect.KClass

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
abstract class ActionData<C, T> {

   @JvmField val name: String
   @JvmField val description: String
   @JvmField val icon: GlyphIcons
   @JvmField val groupApply: GroupApply
   @JvmField val condition: Test<T>
   @JvmField val isLong: Boolean
   @JvmField val action: Act<T>

   @JvmField var isComplex = false
   @JvmField var preventClosing = false
   @JvmField var complexData: ((ActionPane) -> ComplexActionData<T, *>)? = null

   protected constructor(name: String, description: String, icon: GlyphIcons, groupApply: GroupApply, condition: Test<T>, isLong: Boolean, action: Act<T>) {
      this.name = name
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

}

/** Action that executes synchronously - simply consumes the input. */
abstract class FastActionBase<C, T>: ActionData<C, T> {
   protected constructor(name: String, description: String, icon: GlyphIcons, groupApply: GroupApply, constriction: Test<T>, action: Act<T>):
      super(name, description, icon, groupApply, constriction, false, action)
}

/** FastAction that consumes simple input - its type is the same as type of the action. */
class FastAction<T>: FastActionBase<T, T> {

   constructor(name: String, description: String, icon: GlyphIcons, action: Act<T>): super(name, description, icon, NONE, IS, action)

   constructor(name: String, description: String, icon: GlyphIcons, constriction: Test<T>, action: Act<T>): super(name, description, icon, NONE, constriction, action)

   constructor(icon: GlyphIcons, action: Action): super(action.name, action.info + if (action.hasKeysAssigned()) "\n\nShortcut keys: ${action.keys}" else "", icon, NONE, IS, { action.run() })

}

/** FastAction that consumes collection input - its input type is collection of its type. */
class FastColAction<T>: FastActionBase<T, Collection<T>> {

   constructor(name: String, description: String, icon: GlyphIcons, action: Act<Collection<T>>): super(name, description, icon, FOR_ALL, ISNT, action)

   constructor(name: String, description: String, icon: GlyphIcons, constriction: Test<T>, action: Act<Collection<T>>): super(name, description, icon, FOR_ALL, { it.none(constriction) }, action)

}

/** Action that executes asynchronously - receives a future, processes the data and returns it. */
abstract class SlowActionBase<C, T>: ActionData<C, T> {
   protected constructor(name: String, description: String, icon: GlyphIcons, groupApply: GroupApply, constriction: Test<T>, action: Act<T>):
      super(name, description, icon, groupApply, constriction, true, action)
}

/** SlowAction that processes simple input - its type is the same as type of the action. */
class SlowAction<T>: SlowActionBase<T, T> {

   constructor(name: String, description: String, icon: GlyphIcons, action: Act<T>): super(name, description, icon, NONE, IS, action)

   constructor(name: String, description: String, icon: GlyphIcons, groupApply: GroupApply, action: Act<T>): super(name, description, icon, groupApply, IS, action)

   constructor(name: String, description: String, icon: GlyphIcons, constriction: Test<T>, action: Act<T>): super(name, description, icon, NONE, constriction, action)

}

/** SlowAction that processes collection input - its input type is collection of its type. */
class SlowColAction<T>: SlowActionBase<T, Collection<T>> {

   constructor(name: String, description: String, icon: GlyphIcons, action: Act<Collection<T>>): super(name, description, icon, FOR_ALL, ISNT, action)

   constructor(name: String, description: String, icon: GlyphIcons, constriction: Test<T>, action: Act<Collection<T>>): super(name, description, icon, FOR_ALL, { it.none(constriction) }, action)

}

enum class GroupApply {
   FOR_EACH, FOR_ALL, NONE
}