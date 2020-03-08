package sp.it.pl.ui.objects.icon

import de.jensd.fx.glyphs.GlyphIcons
import javafx.beans.property.Property
import sp.it.util.access.toggle
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.sync
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.styleclassAdd
import sp.it.util.ui.styleclassRemove

/** Checkbox like icon. Has two-icon mode and one-icon disabling mode. */
class CheckIcon: Icon {

   /** Selection state. */
   @JvmField val selected: Property<Boolean>
   private var s: Subscription? = null

   /** Creates icon with property as selection value. [selected]===s will always be true. */
   constructor(s: Property<Boolean>?): super() {
      selected = s ?: v(true)
      styleclass(STYLECLASS)
      selected sync { pseudoClassChanged("selected", it) }
      onClickDo { selected.toggle() }
   }

   /** Creates icon with selection set to the provided value. */
   @JvmOverloads constructor(s: Boolean = true): this(v(s))

   /** Sets normal and selected icons. Overrides icon css values.  */
   fun icons(selectedIcon: GlyphIcons, unselectedIcon: GlyphIcons): CheckIcon {
      styleclassRemove(STYLECLASS_DISABLING)
      s?.unsubscribe()
      s = selected sync { icon(if (it) selectedIcon else unselectedIcon) }
      return this
   }

   /**
    * Sets normal and selected icons to the same icon. Overrides icon css values. Installs styleclass
    * that imitates 'disabled' pseudoclass when not selected. This is used as a state indicator
    * instead of different icon.
    */
   fun icons(icon: GlyphIcons): CheckIcon {
      styleclassAdd(STYLECLASS_DISABLING)
      s?.unsubscribe()
      icon(icon)
      return this
   }

   companion object {
      private const val STYLECLASS = "check-icon"
      private const val STYLECLASS_DISABLING = "check-icon-disabling"
   }
}

/** Indeterminate checkbox like icon. Has two-icon mode and one-icon disabling mode. */
class NullCheckIcon: Icon {

   /** Selection state. */
   val selected: Property<Boolean?>
   private var s: Subscription? = null

   /** Creates icon with property as selection value. [selected]===s will always be true. */
   constructor(s: Property<Boolean?>?): super() {
      selected = s ?: vn(true)
      styleclass(STYLECLASS)
      selected sync { pseudoClassChanged("selected", it==true) }
      selected sync { pseudoClassChanged("null", it==null) }
      onClickDo {
         selected.value = when(selected.value) {
            null -> true
            true -> false
            false -> null
         }
      }
   }

   /** Creates icon with selection set to the provided value. */
   constructor(s: Boolean? = true): this(vn(s))

   /** Sets normal and selected icons. Overrides icon css values.  */
   fun icons(selectedIcon: GlyphIcons, unselectedIcon: GlyphIcons, nullIcon: GlyphIcons): NullCheckIcon {
      s?.unsubscribe()
      s = selected sync {
         icon(when (it) { true -> selectedIcon false -> unselectedIcon null -> nullIcon })
      }
      return this
   }

   companion object {
      private const val STYLECLASS = "null-check-icon"
   }
}