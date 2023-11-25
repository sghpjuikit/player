package sp.it.pl.ui.pane

import de.jensd.fx.glyphs.GlyphIcons
import javafx.beans.value.WritableValue
import javafx.event.ActionEvent
import javafx.event.Event
import javafx.scene.Node
import javafx.stage.Window
import kotlin.reflect.KClass
import org.jetbrains.annotations.Blocking
import sp.it.pl.main.APP
import sp.it.pl.main.App
import sp.it.pl.main.configure
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
import sp.it.util.async.VT
import sp.it.util.async.future.Fut
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.async.future.Fut.Companion.futOfBlock
import sp.it.util.collections.collectionWrap
import sp.it.util.collections.getElementClass
import sp.it.util.conf.ConfList
import sp.it.util.conf.Config
import sp.it.util.conf.ConfigDef
import sp.it.util.conf.Constraint
import sp.it.util.conf.EditMode
import sp.it.util.conf.ListConfig
import sp.it.util.conf.ValueConfig
import sp.it.util.conf.but
import sp.it.util.dev.fail
import sp.it.util.dev.failIfFxThread
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.functional.Try
import sp.it.util.functional.Util.IS
import sp.it.util.functional.asIs
import sp.it.util.functional.runTry
import sp.it.util.type.VType
import sp.it.util.type.isObject
import sp.it.util.type.raw
import sp.it.util.type.type
import sp.it.util.type.typeNothingNullable
import sp.it.util.ui.sourceMenuItem
import sp.it.util.ui.traverseToPopupOwnerNode
import sp.it.util.ui.traverseToPopupOwnerWindow
import sp.it.util.units.millis

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

data class ActContext(val window: Window?, val a: ActionPane?,  val node: Node?, val event: Event?, val progress: WritableValue<Number>?) {
   constructor(window: Window): this(window, null, null, null, null)
   constructor(node: Node): this(node.scene?.window, null, node, null, null)
   constructor(node: ActionPane): this(node.scene?.window, null, node, null, node.actionProgress.progressProperty())
   constructor(event: ActionEvent): this(event.sourceMenuItem()?.traverseToPopupOwnerWindow(), null, event.sourceMenuItem()?.traverseToPopupOwnerNode(), event, null)

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
class ActionData<T1, TN>(name: String, type: VType<TN>, type1: VType<T1>, description: String, icon: GlyphIcons, groupApply: GroupApply, condition: Test<TN>, condition1: Test<T1>, isLong: Boolean, action: Act<TN>) {

   @JvmField val type: VType<TN> = type
   @JvmField val typeN: VType<TN> = type
   @JvmField val type1: VType<T1> = type1
             val hasInput: Boolean get() = type.raw!=Unit::class
   @JvmField val name: String = name
             val nameWithDots get() = if (hasInput) "${type.toUi()}.${name.dropLastWhile { it=='.' }}..." else name
   @JvmField val description: String = description
   @JvmField val icon: GlyphIcons = icon
   @JvmField val groupApply: GroupApply = groupApply
   @JvmField val condition: Test<TN> = condition
   @JvmField val conditionN: Test<TN> = condition
   @JvmField val condition1: Test<T1> = condition1
   @JvmField val isLong: Boolean = isLong
   @JvmField val action: Act<TN> = action

   @JvmField var isComplex = false
   @JvmField var preventClosing = false
   @JvmField var complexData: ((ActionPane) -> ComplexActionData<TN, *>)? = null

   fun preventClosing() = apply {
      preventClosing = true
   }

   fun preventClosing(action: (ActionPane) -> ComplexActionData<TN, *>) = apply {
      isComplex = true
      complexData = action
      preventClosing = true
   }

   @Suppress("UNCHECKED_CAST")
   fun invokeIsDoable(data: Any?): Boolean {
      return when (groupApply) {
         FOR_ALL -> condition(collectionWrap(data) as TN)
         FOR_EACH -> when (data) {
            is Collection<*> -> (data as Collection<TN>).all(condition)
            else -> condition(data as TN)
         }
         NONE -> data !is Collection<*> && condition(data as TN)
      }
   }

   @Suppress("UNCHECKED_CAST")
   @Blocking
   @Throws(Throwable::class)
   operator fun invoke(context: ActContext, data: Any?): Any? {
      if (isLong) failIfFxThread()

      return when (groupApply) {
         FOR_ALL -> context.action(collectionWrap(data) as TN)
         FOR_EACH -> when (data) {
            is Collection<*> -> (data as Collection<TN>).map { context.action(it) }
            else -> context.action(data as TN)
         }
         NONE -> context.action(data as TN)
      }
   }

   @Blocking
   fun invokeTry(context: ActContext, data: Any?): Try<Any?, Throwable> {
      return runTry { invoke(context, data) }.mapError { RuntimeException("Running action=$name failed", it) }
   }

   fun invokeFut(context: ActContext, data: Any?): Fut<*> {
      failIfNotFxThread()

      return if (isLong)
         fut()
            .ui { context.progress?.value = -1.0 }
            .then(VT) { invokeTry(context, data).orThrow }
            .thenWait(150.millis)  // very short actions 'pretend' to be busy for a while
            .ui { context.progress?.value = +1.0; it }
      else
         futOfBlock { invokeTry(context, data).orThrow }
            .thenWait(150.millis)  // very short actions 'pretend' to be busy for a while
   }

   fun invokeFutAndProcess(context: ActContext, data: Any?) {
      failIfNotFxThread()

      invokeFut(context, data).thenFlatten().onDone(FX) {
         it.toTryRaw()
            .ifError { context.apOrApp.show(it) }
            .ifOk { if (!isResultUnit(it)) context.apOrApp.show(it) }
      }
   }

   fun invokeWithForm() = invokeWithForm {  }

   fun invokeWithForm(block: Config<*>.() -> Unit) {
      val context = ActContext(null, null, null, null, null)
      when {
         type.raw.isObject && !type.isNullable -> invokeFutAndProcess(context, type.raw.objectInstance.asIs())
         type.raw==App::class && !type.isNullable -> invokeFutAndProcess(context, APP.asIs())
         type.raw == Unit::class -> invokeFutAndProcess(context, Unit.asIs())
         type == typeNothingNullable() -> invokeFutAndProcess(context, null.asIs())
         else -> {
            val receiver = when {
               type1!=typeN -> {
                  val t1: VType<T1> = when (type1.raw) {
                     Any::class -> VType(String::class.java, type1.isNullable).asIs()  // Any::class does not have an editor, but String editor is still plenty useful
                     else -> type1
                  }
                  val confList = ConfList(t1, null, { Config.forValue(t1, "Item", it).constrain { but(buildConstraint1()); but(Constraint.ObjectNonNull) } })
                  ListConfig("Input", ConfigDef("Input", "Input", "", EditMode.USER), confList, "", setOf(), setOf()).constrain {
                     addConstraint(buildConstraintN().asIs())
                     addConstraint(Constraint.CollectionSize(1, null))
                     but()
                  }
               }
               else -> {
                  val tn: VType<TN> = when (type.raw) {
                     Any::class -> VType(String::class.java, type.isNullable).asIs()  // Any::class does not have an editor, but String editor is still plenty useful
                     else -> type
                  }
                  ValueConfig(tn, "Input", "Input", null, "", description, EditMode.USER).constrain { but(buildConstraintN()) }
               }
            }

            receiver.apply(block).configure(nameWithDots) { invokeFutAndProcess(context, it.value.asIs()) }
         }
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
   fun prepInputExact(data: Any?): TN = prepInput(data) as TN

   fun prepInput(data: Any?): Any? = when (groupApply) {
      FOR_ALL -> collectionWrap(data)
      FOR_EACH -> fail { "Action with $groupApply should never get here" }
      NONE -> data
   }

   fun buildConstraint1(): Constraint<T1?> = object: Constraint<T1?> {
      override fun isValid(value: T1?) = value==null || condition1(value)
      override fun message() = "Not a valid input"
   }

   fun buildConstraintN(): Constraint<TN?> = object: Constraint<TN?> {
      override fun isValid(value: TN?) = value==null || conditionN(value)
      override fun message() = "Not a valid input"
   }

   override fun toString() = "ActionData(\"$name\")"

   enum class GroupApply {
      FOR_EACH, FOR_ALL, NONE
   }
   
   enum class Threading {
      UI, BLOCK
   }

   @JvmInline value class UiResult(val ui: Node)
}

/** [ActionData] that executes synchronously - simply consumes the input. */
inline fun <reified T1, reified TN> actionBase(name: String, description: String, icon: GlyphIcons, groupApply: GroupApply, threading: Threading = UI, noinline constriction: Test<TN>, noinline constriction1: Test<T1>, noinline action: Act<TN>) = ActionData<T1, TN>(name, type(), type(), description, icon, groupApply, constriction, constriction1, threading==BLOCK, action)

/** [action] that consumes simple input - its type is the same as type of the action. */
inline fun <reified T> action(name: String, description: String, icon: GlyphIcons, threading: Threading = UI, noinline action: Act<T>) = actionBase<T, T>(name, description, icon, NONE, threading, IS, IS, action)

/** [action] that consumes simple input - its type is the same as type of the action. */
inline fun <reified T> action(name: String, description: String, icon: GlyphIcons, threading: Threading = UI, noinline constriction: Test<T>, noinline action: Act<T>) = actionBase<T, T>(name, description, icon, NONE, threading, constriction, constriction, action)

/** [action] that consumes simple input - its type is the same as type of the action. */
inline fun <reified T> action(icon: GlyphIcons, action: Action) = actionBase<T, T>(action.name, action.info + if (action.hasKeysAssigned()) "\n\nShortcut keys: ${action.keys}" else "", icon, NONE, UI, IS, IS, { action.run() })

/** [action] that consumes collection input - its input type is collection of its type. */
inline fun <reified T> actionAll(name: String, description: String, icon: GlyphIcons, threading: Threading = UI, noinline action: Act<Collection<T>>) = actionBase<T, Collection<T>>(name, description, icon, FOR_ALL, threading, IS, IS, action)

/** [action] that consumes collection input - its input type is collection of its type. */
inline fun <reified T> actionAll(name: String, description: String, icon: GlyphIcons, threading: Threading = UI, noinline constriction: Test<T>, noinline action: Act<Collection<T>>) = actionBase<T, Collection<T>>(name, description, icon, FOR_ALL, threading, { it.all(constriction) }, constriction, action)
