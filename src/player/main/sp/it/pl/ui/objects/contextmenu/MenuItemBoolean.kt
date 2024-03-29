package sp.it.pl.ui.objects.contextmenu

import java.util.function.Consumer
import javafx.beans.property.Property
import javafx.event.ActionEvent.ACTION
import javafx.scene.control.MenuItem
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.util.access.toggle
import sp.it.util.access.v
import sp.it.util.functional.ifNotNull
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attachTrue
import sp.it.util.reactive.onEventDown

/**
 * Simple [MenuItem] implementation with check icon.
 * Clicking on the icon always has the same effect as clicking on the menu item text.
 * The icon selection changes on/off on mouse click but this default behavior can be
 * changed using [installOnMouseClick] which overrides it.
 *
 * Features:
 *  *  Check icon reflecting selection state
 *  *  Observable selection state
 *  *  Custom click implementation
 */
class MenuItemBoolean(text: String? = "", selectedInitial: Property<Boolean> = v(false)): MenuItem(text) {
   /** Selection icon */
   private val icon = CheckIcon(selectedInitial)
   /** Selection mouse click disposer */
   private var mouseClicking: Subscription? = null
   /** Selection state reflected by the icon. Changes on click. Default false. */
   val selected = icon.selected

   constructor(text: String? = "", selectedInitial: Boolean = false): this(text, v(selectedInitial))

   init {
      graphic = icon
      icon.styleclass(STYLECLASS_ICON)
      icon.gap(0)
      icon.isMouseTransparent = true
      icon.isFocusTraversable = false
      installOnMouseClick { selected.toggle() }
   }

   /**
    * Overrides default click implementation which changes the selection value.
    * After using this method, icon will still reflect the selection, but it
    * will not change unless changed manually from the handler.
    *
    * This is useful for cases, where the menu lists items to choose from and
    * exactly one must be selected at any time. This requires deselection to
    * be impossible.
    */
   private fun installOnMouseClick(block: () -> Unit) {
      mouseClicking?.unsubscribe()
      mouseClicking = onEventDown(ACTION) { block() }
      icon.onClickDo { block() }
   }

   companion object {
      private const val STYLECLASS = "checkicon-menu-item"
      private const val STYLECLASS_ICON = "checkicon-menu-item-icon"
      private const val STYLECLASS_ICON_SINGLE_SEL = "checkicon-menu-item-icon-single-selection"

      /**
       * @param inputSelected must be in [inputs], by object reference
       */
      fun <I> buildSingleSelectionMenu(inputs: Collection<I>, inputSelected: I, toNameUi: (I) -> String, action: Consumer<I>): List<MenuItem> = inputs.asSequence()
         .map { input ->
            MenuItemBoolean(toNameUi(input), input===inputSelected).apply {
               icon.styleclass(STYLECLASS_ICON_SINGLE_SEL)
               installOnMouseClick { selected.value = true }
               userData = input
               selected attachTrue {
                  parentMenu.ifNotNull { p ->
                     p.items.forEach { i: MenuItem -> (i as MenuItemBoolean).selected.value = i===this }
                     action.accept(input)
                  }
               }
            }
         }
         .sortedBy { it.text }
         .toList()
   }

}