package sp.it.pl.ui.objects.contextmenu

import java.util.function.Consumer
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.util.functional.ifNotNull
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.ui.styleclassToggle

/**
 * Simple [Menu] implementation with check icon. Clicking on the icon
 * always has the same effect as clicking on the menu item text. The icon
 * selection changes on/off on mouse click but this default behavior can be
 * changed using [.setOnMouseClicked] which overrides it.
 *
 * Features:
 *  *  Check icon reflecting selection state
 *  *  Observable selection state
 *  *  Custom click implementation
 */
class SelectionMenuItem(text: String? = "", selectedInitial: Boolean = false): Menu(text) {
   /** Selection icon */
   private val icon = CheckIcon(selectedInitial)
   /** Selection mouse click disposer */
   private var mouseClicking: Subscription? = null
   /** Selection state reflected by the icon. Changes on click. Default false. */
   val selected = icon.selected

   init {
      graphic = icon
      icon.styleclass(STYLECLASS_ICON)
      icon.gap(0)

      // action = toggle selection
      installOnMouseClick { selected.setValue(!selected.value) }

      // hide open submenu arrow if no children
      styleClass.add(STYLECLASS_NO_CHILDREN)
      items.onChange {
         styleclassToggle(STYLECLASS_NO_CHILDREN, items.isEmpty())
      }
   }

   /**
    * Overrides default click implementation which changes the selection value.
    * After using this method, icon will still reflect the selection, but it it
    * will not change unless changed manually from the handler.
    *
    * This is useful for cases, where the menu lists items to choose from and
    * exactly one must be selected at any time. This requires deselection to
    * be impossible.
    */
   private fun installOnMouseClick(block: () -> Unit) {
      mouseClicking?.unsubscribe()
      mouseClicking = onEventDown(MOUSE_CLICKED) { block() }
      icon.onClickDo { block() }
   }

   companion object {
      private const val STYLECLASS = "checkicon-menu-item"
      private const val STYLECLASS_ICON = "checkicon-menu-item-icon"
      private const val STYLECLASS_ICON_SINGLE_SEL = "checkicon-menu-item-icon-single-selection"
      private const val STYLECLASS_NO_CHILDREN = "menu-nochildren"

      /**
       * @param inputSelected must be in [inputs], by object reference
       */
      fun <I> buildSingleSelectionMenu(inputs: Collection<I>, inputSelected: I, toNameUi: (I) -> String, action: Consumer<I>): List<MenuItem> = inputs.asSequence()
         .map { input: I ->
            SelectionMenuItem(toNameUi(input), input===inputSelected).apply {
               icon.styleclass(STYLECLASS_ICON_SINGLE_SEL)
               installOnMouseClick {
                  parentMenu.ifNotNull { p ->
                     p.items.forEach { i: MenuItem -> (i as SelectionMenuItem).selected.value = false }
                     selected.value = true
                     action.accept(input)
                  }
               }
            }
         }
         .sortedBy { it.text }
         .toList()
   }

}