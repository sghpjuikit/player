package iconViewer

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Pos.CENTER
import javafx.scene.control.Label
import javafx.scene.control.SelectionMode.SINGLE
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import javafx.util.Callback
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.emScaled
import sp.it.pl.ui.objects.grid.GridCell
import sp.it.pl.ui.objects.grid.GridView
import sp.it.pl.ui.objects.grid.GridView.SelectionOn.KEY_PRESS
import sp.it.pl.ui.objects.grid.GridView.SelectionOn.MOUSE_CLICK
import sp.it.pl.ui.objects.grid.GridView.SelectionOn.MOUSE_HOVER
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.access.fieldvalue.StringGetter
import sp.it.util.collections.setTo
import sp.it.util.file.div
import sp.it.util.functional.asIf
import sp.it.util.functional.net
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.system.copyToSysClipboard
import sp.it.util.type.Util.getEnumConstants
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.listView
import sp.it.util.ui.listViewCellFactory
import sp.it.util.ui.minPrefMaxWidth
import sp.it.util.ui.prefSize
import sp.it.util.ui.stackPane
import sp.it.util.ui.x

@Widget.Info(
   author = "Martin Polakovic",
   name = "Icon browser",
   description = "Displays glyph icons of supported fonts.",
   version = "1.0.0",
   year = "2020",
   group = Widget.Group.DEVELOPMENT
)
class IconViewer(widget: Widget): SimpleController(widget) {
   val iconSize = 120.emScaled
   val iconsView = GridView<GlyphIcons, GlyphIcons>(GlyphIcons::class.java, { it }, iconSize, iconSize + 30, 5.0, 5.0).apply {
      styleClass += "icon-grid"
      search.field = StringGetter.of { value, _ -> value.name() }
      selectOn setTo listOf(MOUSE_HOVER, MOUSE_CLICK, KEY_PRESS)
      cellFactory.value = Callback {
         object: GridCell<GlyphIcons, GlyphIcons>() {

            init {
               styleClass += "icon-grid-cell"
               isPickOnBounds = true
            }

            public override fun updateItem(item: GlyphIcons?, empty: Boolean) {
               super.updateItem(item, empty)
               graphic = when {
                  empty || item==null -> null
                  else -> graphic.asIf<IconCellGraphics>()?.apply { setGlyph(item) } ?: IconCellGraphics(item, iconSize)
               }
            }

            override fun updateSelected(selected: Boolean) {
               super.updateSelected(selected)
               graphic.asIf<IconCellGraphics>()?.select(selected)
            }

         }
      }
   }
   val groupsView = listView<Class<GlyphIcons>> {
      minPrefMaxWidth = 200.0.emScaled
      cellFactory = listViewCellFactory { group, empty ->
         text = if (empty) null else group.simpleName
      }
      selectionModel.selectionMode = SINGLE
      selectionModel.selectedItemProperty() sync {
         iconsView.itemsRaw setTo it?.net { getEnumConstants<GlyphIcons>(it) }.orEmpty()
      }
      items setTo Icon.GLYPH_TYPES
   }

   init {
      root.prefSize = 700.emScaled x 500.emScaled
      root.consumeScrolling()
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()

      root.lay += hBox(20, CENTER) {
         lay += groupsView
         lay(ALWAYS) += stackPane {
            lay += iconsView
         }
      }
   }

   override fun focus() = groupsView.selectionModel.select(0)

}

class IconCellGraphics(icon: GlyphIcons?, iconSize: Double): VBox(5.0) {

   private val nameLabel = Label()
   private val graphics: Icon
   private var glyph: GlyphIcons? = icon

   init {
      alignment = CENTER
      styleClass += "icon-grid-cell-graphics"
      graphics = Icon(icon, iconSize).apply {
         isMouseTransparent = true
      }
      onEventDown(MOUSE_CLICKED, PRIMARY) {
         copyToSysClipboard(glyph?.name() ?: "")
      }

      lay(ALWAYS) += stackPane(graphics)
      lay += nameLabel
   }

   fun setGlyph(icon: GlyphIcons?) {
      glyph = icon
      nameLabel.text = icon?.name()?.toLowerCase()?.capitalize() ?: ""
      graphics.icon(icon)
      graphics.tooltip(
         when (icon) {
            null -> ""
            else -> "${icon.name()}\n${icon.unicodeToString()}\n${icon.fontFamily}"
         }
      )
   }

   fun select(value: Boolean) {
      graphics.select(value)
   }

}