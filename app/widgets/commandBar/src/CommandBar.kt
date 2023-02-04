package commandBar

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Pos.CENTER
import javafx.geometry.Side
import javafx.scene.control.ContextMenu
import javafx.scene.input.DragEvent.DRAG_DONE
import javafx.scene.input.DragEvent.DRAG_DROPPED
import javafx.scene.input.DragEvent.DRAG_OVER
import javafx.scene.input.KeyCode.DOWN
import javafx.scene.input.KeyCode.UP
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.input.TransferMode.MOVE
import javafx.scene.shape.Rectangle
import kotlin.math.roundToInt
import sp.it.pl.conf.Command
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.feature.HorizontalDock
import sp.it.pl.main.APP
import sp.it.pl.main.Df
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconUN
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.main.configure
import sp.it.pl.main.emScaled
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.icon.Glyphs
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.window.popup.PopWindow.Companion.asPopWindow
import sp.it.pl.ui.pane.OverlayPane.Companion.asOverlayWindow
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.action.Action
import sp.it.util.collections.setTo
import sp.it.util.conf.Configurable
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.but
import sp.it.util.conf.cList
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.max
import sp.it.util.conf.min
import sp.it.util.conf.values
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.math.clip
import sp.it.util.math.max
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.text.keys
import sp.it.util.text.nameUi
import sp.it.util.ui.drag.handlerAccepting
import sp.it.util.ui.drag.set
import sp.it.util.ui.dsl
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

   val closeAfterAction by cv(false)
      .def(name = "Auto-close", info = "Try closing the window of this widget after a command is invoked. Useful for launchers that employ auto-hide.")
   val iconSize by cv(1).min(1).max(20)
      .def(name = "Icon size", info = "Size of icons specified in em units - application font size multiplies.")
   val iconAlignment by cv(CENTER)
      .def(name = "Icons alignment")
   val icons by cList<Icon>(::icon, ::asConfigurable)
      .def(name = "Icons", info = "List of icons. Icon has an icon and command.")
   val iconPlusVisible by cv(true)
      .def(name = "Show 'Add icon' icon")

   val iconPlus = icon(IconFA.CARET_DOWN).onClickDo { buildMenu().show(it, Side.BOTTOM, 0.0, 0.0) }
   val iconPane = flowPane(5.0, 5.0)
   var drag: Pair<Int,Icon>? = null
   val dragMarker = Rectangle()

   init {
      root.prefSize = 400.emScaled x 50.emScaled
      root.lay += iconPane.apply {
         iconAlignment sync ::setAlignment
      }
      root.lay += dragMarker.apply {
         style = "-fx-fill: -skin-def-font-color;"
         isMouseTransparent = true
         isManaged = false
         isVisible = true
      }

      iconPlusVisible attach { updateIcons() }
      icons.onChangeAndNow { updateIcons() }

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

      root.onEventDown(MOUSE_CLICKED, SECONDARY) { buildMenu().show(root, it) }
   }

   fun buildMenu() = ContextMenu().dsl {
      item("Add", Icon(IconFA.PLUS)) { addIcon() }
      menu("Remove", Icon(IconFA.MINUS)) {
         items(icons.asSequence(), { it.command.toUi() }, { Icon(it.glyph) }) {}
      }
      item("Settings", Icon(IconFA.COG)) { APP.windowManager.showSettings(widget, root) }
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
      // remove context menu
      icon.onEventDown(MOUSE_CLICKED, SECONDARY) {
         if (icon!=iconPlus)
            ContextMenu(menuItem("Remove") { icons -= icon }).show(icon, it)
      }
      // move on drag
      icon.onEventDown(DRAG_DETECTED, PRIMARY) {
         val db = icon.startDragAndDrop(MOVE)
         db.dragView = icon.snapshot(null, null)
         drag = icons.indexOf(icon) to icon
         db[Df.PLAIN_TEXT] = "" //dummy value
      }
      icon.addEventHandler(DRAG_OVER, handlerAccepting { drag!=null })
      icon.addEventHandler(DRAG_DROPPED) {
         it.consume()

         if (drag!=null && drag?.second !== icon) {
            icons setTo buildList {
               addAll(icons)
               remove(drag!!.second)
               add(indexOf(icon) + (if (icon.layoutBounds.width/2<it.x) 1 else 0 ), drag!!.second)
            }
         }
      }
      // move on drag - marker
      icon.addEventHandler(DRAG_OVER) {
         if (drag!=null) {
            dragMarker.isVisible = true
            dragMarker.width = 1.0
            dragMarker.height = icon.height/2
            dragMarker.layoutX = icon.layoutX + if (icon.layoutBounds.width/2>it.x) 0.0 else icon.width
            dragMarker.layoutY = icon.layoutY + (icon.height-dragMarker.height)/2
         }
      }
      icon.onEventDown(DRAG_DONE) {
         dragMarker.isVisible = false
      }
   }

   fun addIcon() {
      val icon = icon()
      icon.net(::asConfigurable).configure("New icon") { icons.add(icon) }
   }

   private fun asConfigurable(icon: Icon): Configurable<*> = object: ConfigurableBase<Any?>() {
      val glyph by cv(icon.glyph).attach { icon.icon(it) }.values(Glyphs.GLYPHS).def(name = "Icon")
      val command by cv(icon.command).attach { icon.command = it }.def(name = "Command").but(Command.parser.toUiStringHelper())
   }

   private var Icon.command: Command
      get() = properties["command"]?.asIs<Command>() ?: Command.DoNothing
      set(value) {
         properties["command"] = value
         when (value) {
            is Command.DoAction -> when (val a = value.toAction()) {
               null -> onClickDo { value(); autoClose() }
               else -> action(a.autoClosing())
            }
            else -> onClickDo { value(); autoClose() }
         }
      }

   private fun Action.autoClosing(): Action =
      Action(name, { action.run(); autoClose() }, info, group, keys)

   private fun autoClose() {
      if (closeAfterAction.value) {
         val wo = widget.window?.asOverlayWindow()
         val wp = widget.window?.asPopWindow()
         wo?.takeIf { it.isAutohide.value }?.hide()
         wp?.takeIf { it.isAutohide.value }?.hide()
      }
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
      override val tags = setOf(UTILITY)
      override val summaryActions = listOf(
         Entry("Bar", "Change icon size up", keys("CTRL+Scroll Up")),
         Entry("Bar", "Change icon size up", keys("CTRL+UP")),
         Entry("Bar", "Change icon size down", keys("CTRL+Scroll Down")),
         Entry("Bar", "Change icon size down", keys("CTRL+DOWN")),
         Entry("Bar", "Open bar menu", SECONDARY.nameUi),
         Entry("Bar icon", "Open icon menu", SECONDARY.nameUi),
      )
   }
}