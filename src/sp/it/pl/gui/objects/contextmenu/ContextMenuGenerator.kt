package sp.it.pl.gui.objects.contextmenu

import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import sp.it.pl.gui.pane.collectionUnwrap
import sp.it.pl.gui.pane.collectionWrap
import sp.it.pl.util.collections.getElementType
import sp.it.pl.util.collections.map.ClassListMap
import sp.it.pl.util.dev.fail
import sp.it.pl.util.functional.asArray
import sp.it.pl.util.functional.asIf
import sp.it.pl.util.functional.net
import sp.it.pl.util.ui.menuItem
import sp.it.pl.util.ui.menuSeparator

val contextMenuGenerator = ContextMenuGenerator()

/** Context menu generator with per type generator registry */
class ContextMenuGenerator {
    private val mSingle = ClassListMap<(ContextMenu, Any?) -> Sequence<MenuItem>> { fail() }
    private val mMany = ClassListMap<(ContextMenu, Any?) -> Sequence<MenuItem>> { fail() }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> add(type: Class<T>, items: Builder<T>.() -> Unit) {
        mSingle.accumulate(type) { menu, item ->
            Builder(menu, collectionUnwrap(item) as T).apply(items).asSequence()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> addMany(type: Class<T>, items: Builder<Collection<T>>.() -> Unit) {
        mMany.accumulate(type) { menu, item ->
            Builder(menu, collectionWrap(item) as Collection<T>).apply(items).asSequence()
        }
    }

    inline fun <reified T: Any> add(noinline items: Builder<T>.() -> Unit) = add(T::class.java, items)

    inline fun <reified T: Any> addMany(noinline items: Builder<Collection<T>>.() -> Unit) = addMany(T::class.java, items)

    operator fun get(contextMenu: ContextMenu, value: Any?): Sequence<MenuItem> {
        val valueSingle = value?.let { collectionUnwrap(it) }
        val valueMulti = value?.let { collectionWrap(value) }?.takeUnless { it.isEmpty() }

        val items1Type = valueSingle?.javaClass ?: Void::class.java
        val items1 = mSingle.getElementsOfSuperV(items1Type)

        val itemsNType = valueMulti.asIf<Collection<*>>()?.getElementType()
        val itemsN = itemsNType?.net { mMany.getElementsOfSuperV(it) } ?: listOf()

        return (items1.asSequence()+itemsN.asSequence())
                .map { it(contextMenu, value) }
                .flatMap { sequenceOf(menuSeparator())+it }
                .drop(1)
    }

    /** Allows DSL for [ContextMenuGenerator]. */
    class Builder<T>(val contextMenu: ContextMenu, val selected: T) {

        private val items = ArrayList<MenuItem>()

        fun asSequence() = items.asSequence()

        fun <T: MenuItem> item(item: T): T {
            items.add(item)
            return item
        }

    }
}

fun ContextMenuGenerator.Builder<*>.item(text: String, handler: (ActionEvent) -> Unit) =
        item(menuItem(text, handler))

fun ContextMenuGenerator.Builder<*>.menu(text: String, graphics: Node? = null, items: Menu.() -> Unit) =
        item(Menu(text, graphics).apply(items))

fun ContextMenuGenerator.Builder<*>.menu(text: String, items: Sequence<MenuItem> = sequenceOf()) =
        item(Menu(text, null, *items.asArray()))

fun ContextMenuGenerator.Builder<*>.separator() = item(menuSeparator())