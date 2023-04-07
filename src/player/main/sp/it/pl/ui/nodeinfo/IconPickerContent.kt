package sp.it.pl.ui.nodeinfo

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Insets
import javafx.geometry.Orientation.VERTICAL
import javafx.geometry.Pos.CENTER
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import kotlin.reflect.KClass
import mu.KLogging
import sp.it.pl.layout.NodeFactory
import sp.it.pl.main.WidgetTags.DEVELOPMENT
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.WidgetFactory
import sp.it.pl.main.APP
import sp.it.pl.main.Df.PLAIN_TEXT
import sp.it.pl.main.HelpEntries
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.Widgets.ICON_BROWSER_NAME
import sp.it.pl.main.emScaled
import sp.it.pl.main.listBox
import sp.it.pl.main.listBoxRow
import sp.it.pl.main.sysClipboard
import sp.it.pl.main.toS
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.grid.GridCell
import sp.it.pl.ui.objects.grid.GridView
import sp.it.pl.ui.objects.grid.GridView.SelectionOn.KEY_PRESS
import sp.it.pl.ui.objects.grid.GridView.SelectionOn.MOUSE_CLICK
import sp.it.pl.ui.objects.grid.GridViewSkin
import sp.it.pl.ui.objects.icon.Glyphs
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.icon.id
import sp.it.util.access.WithSetterObservableValue
import sp.it.util.access.fieldvalue.IconField
import sp.it.util.access.fieldvalue.StringGetter
import sp.it.util.access.readOnly
import sp.it.util.access.toWritable
import sp.it.util.access.vn
import sp.it.util.collections.setTo
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.syncFrom
import sp.it.util.text.capitalLower
import sp.it.util.text.nameUi
import sp.it.util.ui.drag.set
import sp.it.util.ui.dsl
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.separator
import sp.it.util.ui.stackPane
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year

class IconPickerContent: StackPane() {

   val root = this
   val iconSize = 75.emScaled
   val iconGroups = (Glyphs.GLYPH_TYPES.map(::IconGroupOfGlyphClass) + IconGroupOfWidgets()).sortedBy { it.nameUi }
   val iconsView = GridView<GlyphIcons, GlyphIcons>({ it }, (iconSize*1.5 x iconSize/2) + (0 x 30.emScaled), 15 x 15.emScaled).apply {
      styleClass += "icon-grid"
      search.field = StringGetter.of { value, _ -> value.name() }
      filterPrimaryField = IconField.NAME
      selectOn setTo listOf(MOUSE_CLICK, KEY_PRESS)
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

   /** Currently selected icon */
   val selection: WithSetterObservableValue<GlyphIcons?> = iconsView.selectedItem.map { it?.raw }.readOnly().toWritable {
      val iconType = it?.raw?.net { it::class }
      (iconGroups.find { it is IconGroupOfGlyphClass && it.type==iconType } ?: iconGroups.firstOrNull())?.select(true)
      iconsView.selectedItem.value = it
      iconsView.skinImpl?.select(it)
   }
   /** Currently selected icon group */
   private val selectionGroup = vn<IconGroup>(null)
   /** Invoked on close */
   private val onClose = root.onNodeDispose

   init {
      root.prefSize = 700.emScaled x 500.emScaled
      root.consumeScrolling()

      root.lay += hBox(20.emScaled, CENTER) {
         lay += stackPane {
            padding = Insets(50.emScaled, 0.0, 50.emScaled, 0.0)
            minWidth = 150.emScaled
            lay += scrollPane {
               isFitToHeight = true
               isFitToWidth = true
               vbarPolicy = NEVER
               hbarPolicy = NEVER
               content = listBox {
                  lay += iconGroups.map { it.row }
                  iconGroups.find { it==selectionGroup.value }?.select(true)
               }
            }
         }
         lay += separator(VERTICAL) { maxHeight = 200.emScaled }
         lay(ALWAYS) += iconsView.apply {
            cellAlign syncFrom APP.ui.gridCellAlignment on onClose
            footerVisible.value = false
         }
      }
   }

   override fun requestFocus() {
      (iconGroups.find { it==selectionGroup.value } ?: iconGroups.firstOrNull())?.select(true)
      iconsView.requestFocus()
   }

   companion object: WidgetCompanion, KLogging() {
      override val name = ICON_BROWSER_NAME
      override val description = "Displays glyph icons of supported fonts"
      override val descriptionLong = "$description."
      override val icon = IconFA.FONTICONS
      override val version = version(1, 1, 1)
      override val isSupported = true
      override val year = year(2020)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(UTILITY, DEVELOPMENT)
      override val summaryActions = HelpEntries.Grid

      val GlyphIcons.raw get() = if (this is IconWidget) glyph else this
   }

   abstract inner class IconGroup(val id: String, val nameUi: String, val values: () -> Sequence<GlyphIcons>) {
      val row = listBoxRow(IconMD.IMAGE_FILTER_HDR, nameUi) {
         icon.onClickDo { this@IconGroup.select(true) }
      }
      fun select(s: Boolean) {
         row.select(s)
         if (s) iconGroups.find { it!=this && it==selectionGroup.value }?.select(false)
         if (s) selectionGroup.value = this
         if (s) iconsView.itemsRaw setTo values()
         if (s) iconsView.requestFocus()
      }
   }

   inner class IconGroupOfGlyphClass(val type: KClass<out GlyphIcons>): IconGroup(type.toS(), type.toUi(), { Glyphs.valuesOf(type) })

   inner class IconGroupOfWidgets: IconGroup("Widgets", "Widgets", { IconWidget.all() })

   class IconWidget(val name: String, val glyph: GlyphIcons): GlyphIcons by glyph {
      companion object {
         operator fun invoke(it: WidgetFactory<*>) = IconWidget(it.name, it.icon ?: IconFA.PLUG)
         operator fun invoke(it: NodeFactory<*>) = IconWidget(it.name, it.info?.icon ?: IconFA.PLUG)
         fun all() = APP.widgetManager.factories.getFactories().map { invoke(it) } + APP.instances.recommendedNodeClassesAsWidgets.map { invoke(it) }
      }
   }

   class IconCellGraphics(icon: GlyphIcons?, iconSize: Double): VBox(5.0) {
      private val nameLabel = Label()
      private val graphics: Icon
      private var glyph: GlyphIcons? = icon

      init {
         alignment = CENTER
         styleClass += "icon-grid-cell-graphics"
         graphics = Icon(glyph?.raw, iconSize).apply {
            isMouseTransparent = true
         }
         onEventDown(MOUSE_CLICKED, PRIMARY, false) {
            sysClipboard[PLAIN_TEXT] = glyph?.id()
         }

         lay(ALWAYS) += stackPane(graphics)
         lay += nameLabel
      }

      fun setGlyph(icon: GlyphIcons?) {
         glyph = icon?.raw
         nameLabel.text = (if (icon is IconWidget) icon.name else icon?.name())?.capitalLower() ?: ""
         graphics.icon(glyph)
         graphics.tooltip(glyph?.let { "$glyph.name()}\n${it.unicodeToString()}\n${it.fontFamily}" }.orEmpty())
      }

      fun select(value: Boolean) {
         graphics.select(value)
      }

   }

}