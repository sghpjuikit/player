@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package sp.it.pl.gui.objects.contextmenu

import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.MouseEvent
import mu.KotlinLogging
import sp.it.pl.util.access.AccessibleValue
import sp.it.pl.util.collections.map.ClassListMap
import sp.it.pl.util.dev.fail
import sp.it.pl.util.functional.getElementType

private typealias ItemsSupply = (ImprovedContextMenu<*>, Any?) -> Sequence<MenuItem>

private val logger = KotlinLogging.logger { }
val contextMenuItemBuilders = ContextMenuItemSuppliers()

/**
 * Context menu wrapping a value - usually an object set before showing, for menu items' action. It can then generate
 * the items based on the value from supported actions, using [contextMenuItemBuilders].
 *
 * Usually [ValueContextMenu] is better choice.
 */
open class ImprovedContextMenu<E: Any?>: ContextMenu(), AccessibleValue<E> {

    protected var v: E? = null

    init {
        consumeAutoHidingEvents = false
    }

    @Suppress("UNCHECKED_CAST")
    override fun getValue(): E = v as E

    override fun setValue(value: E) {
        v = value
    }

    /** Convenience for [setValue] & [setItemsForValue]. */
    open fun setValueAndItems(value: E) {
        setValue(value)
        setItemsForValue()
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

    /**
     * Add menu items for specified value or current value if none is specified. Previous items are not removed.
     *
     * Usually [setItemsForValue] is better choice.
     */
    fun addItemsForValue(value: E? = v) {
        items += contextMenuItemBuilders[this, value]
    }

    /**
     * Clear and add menu items for specified value or current value if none is specified. Previous items are removed.
     */
    fun setItemsForValue(value: E? = v) {
        items.clear()
        addItemsForValue(value)
    }

}

/**
 * Generic [ImprovedContextMenu], which supports collection unwrapping in [setValue] (empty collection will be handled
 * as null and collection with one element handled as that one element). This is convenient for multi-select controls.
 */
class ValueContextMenu: ImprovedContextMenu<Any?>() {

    override fun setValue(value: Any?) {
        v = when (value) {
            is Collection<*> -> {
                when (value.size) {
                    0 -> null
                    1 -> value.firstOrNull()
                    else -> value
                }
            }
            else -> value
        }
    }

}

class ContextMenuItemSuppliers {
    private val mSingle = ClassListMap<ItemsSupply> { fail() }
    private val mMany = ClassListMap<ItemsSupply> { fail() }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> add(type: Class<T>, items: (ImprovedContextMenu<*>, T) -> Sequence<MenuItem>) {
        mSingle.accumulate(type, items as ItemsSupply)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> addMany(type: Class<T>, items: (ImprovedContextMenu<*>, Collection<T>) -> Sequence<MenuItem>) {
        mMany.accumulate(type, items as ItemsSupply)
    }

    inline fun <reified T: Any> add(noinline items: (ImprovedContextMenu<*>, T) -> Sequence<MenuItem>) {
        add(T::class.java, items)
    }

    inline fun <reified T: Any> addMany(noinline items: (ImprovedContextMenu<*>, Collection<T>) -> Sequence<MenuItem>) {
        addMany(T::class.java, items)
    }

    operator fun get(contextMenu: ImprovedContextMenu<*>, value: Any?): Sequence<MenuItem> {
        val items1 = mSingle.getElementsOfSuperV(value?.javaClass ?: Void::class.java).asSequence()
                .map { it(contextMenu, value) }
                .flatMap { it }
        val itemsN = if (value is Collection<*>) {
            mMany.getElementsOfSuperV(value.getElementType()).asSequence()
                    .map { it(contextMenu, contextMenu.value) }
                    .flatMap { it }
        } else {
            sequenceOf()
        }
        return items1+itemsN
    }
}