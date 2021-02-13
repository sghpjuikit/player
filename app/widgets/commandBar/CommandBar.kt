package commandBar

import de.jensd.fx.glyphs.GlyphIcons
import javafx.scene.control.ContextMenu
import javafx.scene.input.KeyCode.DOWN
import javafx.scene.input.KeyCode.UP
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.ScrollEvent.SCROLL
import kotlin.math.roundToInt
import sp.it.pl.conf.Command
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.WidgetCompanion
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.HorizontalDock
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconUN
import sp.it.pl.main.configure
import sp.it.pl.main.emScaled
import sp.it.pl.ui.objects.icon.Glyphs
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.access.VarEnum
import sp.it.util.access.v
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.conf.Configurable
import sp.it.util.conf.ListConfigurable
import sp.it.util.conf.cList
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.max
import sp.it.util.conf.min
import sp.it.util.functional.asIf
import sp.it.util.functional.net
import sp.it.util.math.clip
import sp.it.util.math.max
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.text.keys
import sp.it.util.text.nameUi
import sp.it.util.type.property
import sp.it.util.ui.flowPane
import sp.it.util.ui.lay
import sp.it.util.ui.menuItem
import sp.it.util.ui.prefSize
import sp.it.util.ui.show
import sp.it.util.ui.x
import sp.it.util.units.em
import sp.it.util.units.version
import sp.it.util.units.year

class CommandBar(widget: Widget): SimpleController(widget), HorizontalDock {

   val iconSize by cv(1).min(1).max(20)
      .def(name = "Icon size", info = "Size of icons specified in em units - application font size multiplies")
   val icons by cList<Icon>(::icon, ::asConfigurable)
      .def(name = "Icons", info = "List of icons. Icon has an icon and command.")
   val iconPlusVisible by cv(true)
      .def(name = "Show 'Add icon' icon")
   val iconPlus = icon(IconFA.PLUS).tooltip("Add icon")
   val iconPane = flowPane(5.0, 5.0)

   init {
      root.prefSize = 400.emScaled x 50.emScaled
      root.lay += iconPane

      iconPlus.onClickDo { addIcon() }

      iconPlusVisible attach { updateIcons() }
      icons.onChange { updateIcons() } on onClose
      updateIcons()

      APP.ui.font attach { updateIconSize() } on onClose
      iconSize attach { updateIconSize() }

      root.onEventDown(KEY_PRESSED, DOWN) { changeIconSize(-1) }
      root.onEventDown(KEY_PRESSED, UP) { changeIconSize(+1) }
      root.onEventDown(SCROLL) {
         val isInc = it.deltaY>0 || it.deltaX<0
         val by = if (isInc) +1 else -1
         changeIconSize(by)
         it.consume()
      }

      root.onEventDown(MOUSE_CLICKED, SECONDARY) {
         ContextMenu(menuItem("Add") { addIcon() }).show(root, it)
      }
   }

   fun updateIcons() {
      iconPane.children setTo (icons + listOf(iconPlus).filter { iconPlusVisible.value })
   }

   fun changeIconSize(by: Int) {
      iconSize.setValueOf { (it + (it*0.2).max(1.0).times(by).roundToInt()).clip(1, 20) }
   }

   fun updateIconSize() {
      val s = iconSize.value.em.emScaled
      icons.forEach { it.size(s) }
      iconPlus.size(s)
   }

   fun icon(glyph: GlyphIcons = IconFA.CIRCLE): Icon = Icon(glyph, iconSize.value.em.emScaled).also { icon ->
      icon.onEventDown(MOUSE_CLICKED, SECONDARY) {
         if (icon!=iconPlus)
            ContextMenu(menuItem("Remove") { icons -= icon }).show(icon, it)
      }
   }

   fun addIcon() {
      val icon = icon()
      icon.net(::asConfigurable).configure("New icon") { icons.add(icon) }
   }

   companion object: WidgetCompanion {
      override val name = "Command bar"
      override val description = "Icon toolbar for launching commands"
      override val descriptionLong = "$description."
      override val icon = IconUN(0x2e2a)
      override val version = version(0, 10, 0)
      override val isSupported = true
      override val year = year(2014)
      override val author = "spit"
      override val contributor = ""
      override val summaryActions = listOf(
         Entry("Bar", "Change icon size up", keys("CTRL+Scroll Up")),
         Entry("Bar", "Change icon size up", keys("CTRL+UP")),
         Entry("Bar", "Change icon size down", keys("CTRL+Scroll Down")),
         Entry("Bar", "Change icon size down", keys("CTRL+DOWN")),
         Entry("Bar", "Open bar menu", SECONDARY.nameUi),
         Entry("Bar icon", "Open icon menu", SECONDARY.nameUi),
      )
      override val group = Widget.Group.OTHER

      fun asConfigurable(icon: Icon): Configurable<*> = ListConfigurable.heterogeneous(
         Config.forProperty<GlyphIcons>("Icon", VarEnum(icon.glyph, Glyphs.GLYPHS).apply { attach { icon.icon(it) } }),
         Config.forProperty<Command>("Command", v(icon.command).apply { attach { icon.command = it } })
      )

      var Icon.commandImpl: Command by property { Command.DoNothing }
      var Icon.command: Command
         get() = commandImpl
         set(value) {
            commandImpl = value
            when (value) {
               is Command.DoAction -> when (val a = value.toAction()) {
                  null -> onClickDo { value() }
                  else -> action(a)
               }
               else -> onClickDo { value() }
            }
         }
   }
}