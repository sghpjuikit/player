package sp.it.util.ui

import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import sp.it.util.dev.Dsl
import sp.it.util.dev.fail
import sp.it.util.functional.asIs

@Dsl
open class MenuBuilder<M, V>(val owner: M, val value: V) {

   /** Add the specified item to this. Base method that this dsl relies on. */
   open infix fun add(item: MenuItem) = add(listOf(item))

   /** Add the specified items to this. Base method that this dsl relies on. */
   open infix fun add(items: Iterable<MenuItem>): Unit = when (owner) {
      is ContextMenu -> owner.items += items
      is Menu -> owner.items += items
      is MutableList<*> -> owner.asIs<MutableList<MenuItem>>() += items
      else -> fail { "Menu DSL owner must be ${ContextMenu::class} or ${MenuItem::class}, but was $owner" }
   }

   /** Create and [add] to items menu with specified text and graphics. */
   @Dsl
   inline fun menu(text: String, graphics: Node? = null, crossinline then: @Dsl MenuBuilder<Menu, V>.() -> Unit): Menu = menu { Menu(text, graphics).dsl(value) { then() } }

   /** [add] to items the specified menu. */
   @Dsl
   fun menu(menu: () -> Menu) = menu().apply(::add)

   /** Create and [add] to items new menu item with specified text and action. */
   @Dsl
   inline fun item(text: String, graphics: Node? = null, crossinline action: @Dsl MenuItem.(V) -> Unit): MenuItem = item { MenuItem(text, graphics).apply { onAction = EventHandler { action(value) } } }

   /** [add] to items the specified item. */
   @Dsl
   fun item(item: () -> MenuItem) = item().apply(::add)

   /** Create and [add] to items new menu items with text and action derived from specified source. */
   @Dsl
   @Suppress("RedundantLambdaArrow")
   inline fun <A> items(source: Sequence<A>, crossinline text: (A) -> String, crossinline action: (A) -> Unit) = items { source.map { menuItem(text(it)) { _ -> action(it) } } }

   /** [add] to items the specified separator. */
   @Dsl
   fun <MI: MenuItem> items(items: () -> Sequence<MI>): List<MI> = items().sortedBy { it.text }.toList().onEach(::add)

   /** Create and [add] to items new menu separator. */
   @Dsl
   fun separator() = separator { menuSeparator() }

   /** [add] to items the specified separator. */
   @Dsl
   fun <MI: SeparatorMenuItem> separator(separator: () -> MI) = separator().apply(::add)

}

@Dsl
inline fun ContextMenu.dsl(block: (MenuBuilder<ContextMenu, Nothing?>).() -> Unit) = dsl(null, block)

@Dsl
inline fun <S: Any?> ContextMenu.dsl(selected: S, block: (MenuBuilder<ContextMenu, S>).() -> Unit) = apply { MenuBuilder(this, selected).block() }

@Dsl
fun <M: MenuItem> M.dsl(block: (MenuBuilder<M, Nothing?>).() -> Unit) = dsl(null, block)

@Dsl
fun <M: MenuItem, S> M.dsl(selected: S, block: (MenuBuilder<M, S>).() -> Unit) = apply { MenuBuilder(this, selected).block() }