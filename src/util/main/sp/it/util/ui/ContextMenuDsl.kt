package sp.it.util.ui

import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import sp.it.util.dev.Dsl
import sp.it.util.dev.fail
import sp.it.util.functional.asIs

@Dsl
open class MenuBuilder<M, V>(val owner: M, val value: V) {

   open infix fun add(item: MenuItem) = when (owner) {
      is ContextMenu -> owner.items += item
      is Menu -> owner.items += item
      is MutableList<*> -> owner.asIs<MutableList<MenuItem>>() += item
      else -> fail { "Menu DSL owner must be ${ContextMenu::class} or ${MenuItem::class}, but was $owner" }
   }

   /** Create and add to items menu with specified text and graphics. */
   @Dsl
   inline fun menu(text: String, graphics: Node? = null, crossinline then: @Dsl MenuBuilder<Menu, V>.() -> Unit) = this add Menu(text, graphics).dsl(value) { then() }

   /** Create and add to items new menu item with specified text and action. */
   @Dsl
   inline fun item(text: String, graphics: Node? = null, crossinline action: @Dsl MenuItem.(V) -> Unit) = this add MenuItem(text, graphics).apply { onAction = EventHandler { action(value) } }

   /** Create and add to items new menu items with text and action derived from specified source. */
   @Dsl
   @Suppress("RedundantLambdaArrow")
   inline fun <A> items(source: Sequence<A>, crossinline text: (A) -> String, crossinline action: (A) -> Unit) = source.map { menuItem(text(it)) { _ -> action(it) } }.sortedBy { it.text }.forEach { this add it }

   /** Create and add to items new menu separator. */
   @Dsl
   fun separator() = this add menuSeparator()

}

@Dsl
inline fun ContextMenu.dsl(block: (MenuBuilder<ContextMenu, Nothing?>).() -> Unit) = dsl(null, block)

@Dsl
inline fun <S: Any?> ContextMenu.dsl(selected: S, block: (MenuBuilder<ContextMenu, S>).() -> Unit) = apply { MenuBuilder(this, selected).block() }

@Dsl
fun <M: MenuItem> M.dsl(block: (MenuBuilder<M, Nothing?>).() -> Unit) = dsl(null, block)

@Dsl
fun <M: MenuItem, S> M.dsl(selected: S, block: (MenuBuilder<M, S>).() -> Unit) = apply { MenuBuilder(this, selected).block() }
