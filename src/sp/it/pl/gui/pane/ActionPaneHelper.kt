package sp.it.pl.gui.pane

import de.jensd.fx.glyphs.GlyphIcons
import javafx.scene.Node
import sp.it.pl.gui.pane.GroupApply.FOR_ALL
import sp.it.pl.gui.pane.GroupApply.FOR_EACH
import sp.it.pl.gui.pane.GroupApply.NONE
import sp.it.pl.util.action.Action
import sp.it.pl.util.async.future.Fut
import sp.it.pl.util.collections.getElementType
import sp.it.pl.util.dev.fail
import sp.it.pl.util.functional.Util.IS
import sp.it.pl.util.functional.Util.ISNT
import java.util.function.Supplier

inline fun <reified T> ActionPane.register(vararg actions: ActionData<T, *>) = register(T::class.java, *actions)

fun getUnwrappedType(d: Any?): Class<*> = when (d) {
    null -> Void::class.java
    is Collection<*> -> d.getElementType()
    else -> d.javaClass
}

fun collectionWrap(o: Any?): Collection<Any?> = o as? Collection<Any?> ?: listOf(o)

fun collectionUnwrap(o: Any?): Any? = when (o) {
    is Collection<*> -> {
        when (o.size) {
            0 -> null
            1 -> o.first()
            else -> o
        }
    }
    else -> o
}

fun futureUnwrap(o: Any?): Any? = when (o) {
    is Fut<*> -> when {
        o.isDone() -> {
            val result = o.getDone()
            when (result) {
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
            val result = o.getDone()
            when (result) {
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
    @JvmField val gui: (Supplier<T>) -> Node
    @JvmField val input: (R) -> Any

    constructor(input: (R) -> Fut<T>, gui: (Supplier<T>) -> Node) {
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
    @JvmField var complexData: ((ActionPane) -> ComplexActionData<*, *>)? = null

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

    fun preventClosing(action: (ActionPane) -> ComplexActionData<T, *>) = apply {
        isComplex = true
        complexData = action
        preventClosing = true
        return this
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
    fun prepInput(data: Any?): T = when (groupApply) {
        FOR_ALL -> collectionWrap(data) as T
        FOR_EACH -> fail { "Action with $groupApply should never get here" }
        NONE -> {
            if (data is Collection<*>) fail { "Action with $groupApply can not use collection" }
            data as T
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

    constructor(icon: GlyphIcons, action: Action): super(action.name, action.info+if (action.hasKeysAssigned()) "\n\nShortcut keys: ${action.keys}" else "", icon, NONE, IS, { action.run() })

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