package iconViewer

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Insets
import javafx.geometry.Orientation.VERTICAL
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import kotlin.reflect.KClass
import mu.KLogging
import sp.it.pl.layout.Widget
import sp.it.pl.main.WidgetTags.DEVELOPMENT
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.Df.PLAIN_TEXT
import sp.it.pl.main.HelpEntries
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconUN
import sp.it.pl.main.Widgets.ICON_BROWSER_NAME
import sp.it.pl.main.emScaled
import sp.it.pl.main.sysClipboard
import sp.it.pl.main.toS
import sp.it.pl.main.toUi
import sp.it.pl.ui.LabelWithIcon
import sp.it.pl.ui.objects.grid.GridCell
import sp.it.pl.ui.objects.grid.GridView
import sp.it.pl.ui.objects.grid.GridView.CellGap
import sp.it.pl.ui.objects.grid.GridView.SelectionOn.KEY_PRESS
import sp.it.pl.ui.objects.grid.GridView.SelectionOn.MOUSE_CLICK
import sp.it.pl.ui.objects.grid.GridView.SelectionOn.MOUSE_HOVER
import sp.it.pl.ui.objects.grid.GridViewSkin
import sp.it.pl.ui.objects.icon.Glyphs
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.icon.id
import sp.it.util.access.OrV.OrValue.Initial.Inherit
import sp.it.util.access.OrV.OrValue.Initial.Override
import sp.it.util.access.fieldvalue.IconField
import sp.it.util.access.fieldvalue.StringGetter
import sp.it.util.collections.setTo
import sp.it.util.conf.c
import sp.it.util.conf.cOr
import sp.it.util.conf.defInherit
import sp.it.util.conf.noUi
import sp.it.util.file.div
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.onEventDown
import sp.it.util.text.capitalLower
import sp.it.util.text.nameUi
import sp.it.util.ui.drag.set
import sp.it.util.ui.dsl
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.separator
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year

class IconViewer(widget: Widget): SimpleController(widget) {
   val iconSize = 75.emScaled
   val iconGroups = Glyphs.GLYPH_TYPES.sortedBy { it.simpleName.orEmpty() }.map { IconGroup(it) }
   val iconsView = GridView<GlyphIcons, GlyphIcons>({ it }, (iconSize*1.5 x iconSize/2) + (0 x 30.emScaled), 0 x 15.emScaled).apply {
      styleClass += "icon-grid"
      search.field = StringGetter.of { value, _ -> value.name() }
      filterPrimaryField = IconField.NAME
      selectOn setTo listOf(MOUSE_HOVER, MOUSE_CLICK, KEY_PRESS)
      cellFactory.value = {
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
      skinProperty() attach {
         it?.asIs<GridViewSkin<*, *>>()?.menuOrder?.dsl {
            item("Copy selected", keys = PRIMARY.nameUi) {
               sysClipboard[PLAIN_TEXT] = selectedItem.value?.id()
            }
         }
      }
   }

   val gridShowFooter by cOr(APP.ui::gridShowFooter, iconsView.footerVisible, Override(false), onClose)
      .defInherit(APP.ui::gridShowFooter)
   val gridCellAlignment by cOr<CellGap>(APP.ui::gridCellAlignment, iconsView.cellAlign, Inherit(), onClose)
      .defInherit(APP.ui::gridCellAlignment)
   var selection by c("").noUi()

   init {
      root.prefSize = 700.emScaled x 500.emScaled
      root.consumeScrolling()
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()

      root.lay += hBox(20.emScaled, CENTER) {
         lay += stackPane {
            padding = Insets(50.emScaled, 0.0, 50.emScaled, 0.0)
            minWidth = 150.emScaled
            lay += scrollPane {
               isFitToHeight = true
               isFitToWidth = true
               vbarPolicy = NEVER
               hbarPolicy = NEVER
               content = vBox(0.0, CENTER_LEFT) {
                  lay += iconGroups.map { it.label }
                  iconGroups.find { it.type.toS()==selection }?.select(true)
               }
            }
         }
         lay += separator(VERTICAL) { maxHeight = 200.emScaled }
         lay(ALWAYS) += iconsView
      }
   }

   override fun focus() {
      (iconGroups.find { it.type.toS()==selection } ?: iconGroups.firstOrNull())?.select(true)
      iconsView.requestFocus()
   }

   companion object: WidgetCompanion, KLogging() {
      override val name = ICON_BROWSER_NAME
      override val description = "Displays glyph icons of supported fonts"
      override val descriptionLong = "$description."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 1, 1)
      override val isSupported = true
      override val year = year(2020)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(UTILITY, DEVELOPMENT)
      override val summaryActions = HelpEntries.Grid
   }

   inner class IconGroup(val type: KClass<out GlyphIcons>) {
      val label = LabelWithIcon(IconMD.IMAGE_FILTER_HDR, type.toUi()).apply {
         icon.onClickDo { this@IconGroup.select(true) }
      }
      fun select(s: Boolean) {
         label.select(s)
         if (s) iconGroups.find { it!==this && it.type.toS()==selection }?.select(false)
         if (s) selection = type.toS()
         if (s) iconsView.itemsRaw setTo Glyphs.valuesOf(type)
         if (s) iconsView.requestFocus()
      }
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
            sysClipboard[PLAIN_TEXT] = glyph?.id()
         }

         lay(ALWAYS) += stackPane(graphics)
         lay += nameLabel
      }

      fun setGlyph(icon: GlyphIcons?) {
         glyph = icon
         nameLabel.text = icon?.name()?.capitalLower() ?: ""
         graphics.icon(icon)
         graphics.tooltip(icon?.let { "${it.name()}\n${it.unicodeToString()}\n${it.fontFamily}" }.orEmpty())
      }

      fun select(value: Boolean) {
         graphics.select(value)
      }
   }
}