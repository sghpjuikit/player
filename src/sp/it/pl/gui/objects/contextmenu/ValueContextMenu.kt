@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package sp.it.pl.gui.objects.contextmenu

import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.MouseEvent
import sp.it.pl.gui.pane.collectionUnwrap
import sp.it.pl.gui.pane.collectionWrap
import sp.it.pl.util.access.AccessibleValue
import sp.it.pl.util.collections.map.ClassListMap
import sp.it.pl.util.dev.fail
import sp.it.pl.util.functional.asArray
import sp.it.pl.util.functional.getElementType
import sp.it.pl.util.functional.seqOf
import sp.it.pl.util.functional.setTo
import sp.it.pl.util.graphics.menuItem
import sp.it.pl.util.graphics.menuSeparator

val contextMenuItemBuilders = ContextMenuItemSuppliers()

/**
 * Context menu wrapping a value - usually an object set before showing, for menu items' action.
 * It can then generate the items based on the value from supported actions, using [contextMenuItemBuilders].
 */
open class ValueContextMenu<E: Any?>: ContextMenu(), AccessibleValue<E> {

    protected var v: E? = null

    init {
        consumeAutoHidingEvents = false
    }

    @Suppress("UNCHECKED_CAST")
    override fun getValue(): E = v as E

    override fun setValue(value: E) {
        v = value
    }

    /** Invokes [setValue] and sets items to those provided by [contextMenuItemBuilders] for the value of this menu. */
    open fun setValueAndItems(value: E) {
        setValue(value)
        items setTo contextMenuItemBuilders[this, value]
    }

    override fun show(n: Node, screenX: Double, screenY: Double) = show(n.scene.window, screenX, screenY)

    /**
     * Shows the context menu for node at proper coordinates of the event.
     *
     * Prefer this method to show context menu (especially in MouseClick handler), because when showing ContextMenu,
     * there is a difference between show(Window,x,y) and (Node,x,y). The former will not hide the menu when next click
     * happens within the node itself! This method avoids that.
     */
    fun show(n: Node, e: MouseEvent) = show(n.scene.window, e.screenX, e.screenY)

    fun show(n: Node, e: ContextMenuEvent) = show(n.scene.window, e.screenX, e.screenY)

}

/** Context menu generator with per type generator registry */
class ContextMenuItemSuppliers {
    private val mSingle = ClassListMap<(ContextMenu, Any?) -> Sequence<MenuItem>> { fail() }
    private val mMany = ClassListMap<(ContextMenu, Any?) -> Sequence<MenuItem>> { fail() }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> add(type: Class<T>, items: ContextMenuBuilder<T>.() -> Unit) {
        mSingle.accumulate(type) { menu, item ->
            ContextMenuBuilder(menu, collectionUnwrap(item) as T).apply(items).asSequence()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> addMany(type: Class<T>, items: ContextMenuBuilder<Collection<T>>.() -> Unit) {
        mMany.accumulate(type) { menu, item ->
            ContextMenuBuilder(menu, collectionWrap(item) as Collection<T>).apply(items).asSequence()
        }
    }

    inline fun <reified T: Any> add(noinline items: ContextMenuBuilder<T>.() -> Unit) = add(T::class.java, items)

    inline fun <reified T: Any> addMany(noinline items: ContextMenuBuilder<Collection<T>>.() -> Unit) = addMany(T::class.java, items)

    operator fun get(contextMenu: ContextMenu, value: Any?): Sequence<MenuItem> {
        val valueSingle = value?.let { collectionUnwrap(it) }
        val valueMulti = value?.let { collectionWrap(value) }?.takeUnless { it.isEmpty() }
        val items1 = mSingle.getElementsOfSuperV(valueSingle?.javaClass ?: Void::class.java).asSequence()
                .map { it(contextMenu, value) }
        val itemsN = if (valueMulti is Collection<*>) {
            mMany.getElementsOfSuperV(valueMulti.getElementType()).asSequence()
                    .map { it(contextMenu, value) }
        } else {
            sequenceOf()
        }
        return (items1+itemsN).flatMap { sequenceOf(menuSeparator())+it }.drop(1)
    }

}

/** Allows DSL for [ContextMenuItemSuppliers]. */
class ContextMenuBuilder<T>(val contextMenu: ContextMenu, val selected: T) {

    private val items = ArrayList<MenuItem>()

    fun asSequence() = items.asSequence()

    fun <T: MenuItem> item(item: T): T {
        items.add(item)
        return item
    }

}

fun ContextMenuBuilder<*>.item(text: String, handler: (ActionEvent) -> Unit) =
        item(menuItem(text, handler))

fun ContextMenuBuilder<*>.menu(text: String, graphic: Node? = null, items: Menu.() -> Unit) =
        item(sp.it.pl.util.graphics.menu(text, graphic, items))

fun ContextMenuBuilder<*>.menu(text: String, items: Sequence<MenuItem> = seqOf()) =
        item(Menu(text, null, *items.asArray()))

fun ContextMenuBuilder<*>.separator() = item(menuSeparator())