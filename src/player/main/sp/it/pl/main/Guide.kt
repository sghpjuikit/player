package sp.it.pl.main

import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Pos.CENTER
import javafx.scene.Node
import javafx.scene.input.DataFormat
import javafx.scene.input.KeyCode.LEFT
import javafx.scene.input.KeyCode.RIGHT
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.BACK
import javafx.scene.input.MouseButton.FORWARD
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.TransferMode.COPY
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.popover.PopOver
import sp.it.pl.gui.objects.popover.ScreenPos
import sp.it.pl.layout.container.BiContainer
import sp.it.pl.layout.widget.emptyWidgetFactory
import sp.it.pl.layout.widget.initialTemplateFactory
import sp.it.pl.layout.widget.testControlContainer
import sp.it.util.access.v
import sp.it.util.action.ActionManager
import sp.it.util.action.ActionRegistrar
import sp.it.util.action.IsAction
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.async.runFX
import sp.it.util.collections.setToOne
import sp.it.util.conf.EditMode
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.conf.IsConfig
import sp.it.util.conf.c
import sp.it.util.conf.cr
import sp.it.util.conf.cv
import sp.it.util.functional.orNull
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.text.keys
import sp.it.util.ui.Util.layHorizontally
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.textArea
import sp.it.util.ui.vBox
import sp.it.util.units.millis
import sp.it.util.units.seconds
import sp.it.util.units.times
import java.util.ArrayList

class Guide(guideEvents: Handler1<Any>): GlobalSubConfigDelegator("${Settings.Plugin.name}.Guide") {

   @IsConfig(name = "Hint", editable = EditMode.APP)
   private var at by c(-1)

   @IsConfig(name = "Show guide on app start", info = "Show guide when application starts. Default true, but when guide is shown, it is set to false so the guide will never appear again on its own.")
   val firstTime by cv(true)

   @IsAction(name = "Open guide", desc = "Resume or start the guide.")
   private val openGuide by cr { open() }

   private var prevAt = -1
   private val guideTitleText = v("")
   private val guideText = v("")
   private val guideEvents = guideEvents.apply { add { handleAction(it) } }
   private val popup = lazy { buildPopup() }
   private val popupContent: VBox by lazy { buildContent() }
   private val proceedAnim = anim(400.millis) { popupContent.opacity = -(it*it - 1) }
   val hints = Hints()

   init {
      if (firstTime.value)
         APP.onStarted += { runFX(3000.millis) { open() } }
   }

   private fun buildContent() = vBox(25) {
      padding = Insets(30.0)

      onEventDown(KEY_PRESSED, LEFT) { goToPrevious() }
      onEventDown(KEY_PRESSED, RIGHT) { goToNext() }
      onEventDown(MOUSE_CLICKED) {
         if (it.isStillSincePress) {
            when (it.button) {
               PRIMARY, FORWARD -> goToNext()
               SECONDARY, BACK -> {
                  if (hints[at].action=="Navigation") {
                     runFX(proceedAnim.cycleDuration*3.0) { goToNext() }
                     runFX(proceedAnim.cycleDuration*6.0) { goToNext() }
                  } else {
                     goToPrevious()
                  }
               }
               else -> Unit
            }
         }
         it.consume()
      }

      lay(ALWAYS) += textArea {
         isEditable = false
         isMouseTransparent = true
         isWrapText = true
         minWidth = 350.0
         styleClass += STYLECLASS_TEXT
         guideText sync ::setText
      }
   }

   private fun buildPopup() = PopOver(popupContent).apply {
      isAutoHide = false
      isHideOnClick = false
      isHideOnEscape = true
      skinn.contentPadding = Insets(8.0)
      arrowSize.value = 0.0
      detached.value = true
      onHiding = EventHandler { runFX(20.millis) { hints.h03_guideClose.proceedIfActive() } }
      headerIcons += listOf(
         infoIcon("Guide info popup."
            + "\n\nThere are many others. If you see one for the first time, check it out."
            + "\n\nThis popup will close on its own when you clock somewhere. ESCAPE works too."
         ),
         // new Icon(ARROW_LEFT,11,"Previous",this::goToPrevious), // unnecessary, uses left+right mouse button navigation
         label {
            guideTitleText sync ::setText
            guideTitleText sync ::setText
         },
         // new Icon(ARROW_RIGHT,11,"Next",this::goToNext) // unnecessary, uses left+right mouse button navigation
         label()
      )
   }

   private fun proceed() {
      if (hints.isEmpty()) return
      if (at<0 || at>=hints.size) at = 0
      proceedAnim.playOpenDoClose { proceedDo() }
      firstTime.set(false)
   }

   private fun proceedDo() {
      if (prevAt>=0 && prevAt<hints.size) {
         hints[prevAt].onExit()
      }

      val h = hints[at]

      h.onEnter()

      popup.value.takeIf { !it.isShowing }?.show(ScreenPos.APP_CENTER)
      guideTitleText.value = "${at + 1}/${hints.size}"
      popup.value.title.value = if (h.action.isEmpty()) "Guide" else "Guide - ${h.action}"

      guideText.value = h.text()
      popupContent.children setToOne popupContent.children[0]
      h.graphics?.invoke(h)?.let {
         it.onEventDown(MOUSE_CLICKED) { it.consume() }
         popupContent.children += it
      }

      popupContent.requestFocus()
   }

   private fun handleAction(action: Any) {
      if (hints[at].action==action) goToNext()
   }

   fun open() {
      proceed()
      guideEvents.invoke("Guide opening")
   }

   fun close() {
      popup.orNull()?.hide()
   }

   fun goToStart() {
      if (hints.isEmpty()) return
      prevAt = -1
      at = 0
      proceed()
   }

   fun goToPrevious() {
      if (at==0) return
      prevAt = at
      at--
      proceed()
   }

   fun goToNext() {
      prevAt = at
      at++
      proceed()
   }

   inner class Hint(
      val action: String,
      val text: () -> String,
      val graphics: (Hint.() -> Node)?,
      val onEnter: () -> Unit = {},
      val onExit: () -> Unit = {}
   ) {
      fun proceedIfActive() = guideEvents(action)
   }

   companion object {
      private const val ICON_SIZE = 40.0
      private const val STYLECLASS_TEXT = "guide-text"

      private fun String.pretty() = "'${keys(this)}'"
   }

   @Suppress("PropertyName")
   inner class Hints: ArrayList<Hint>(30) {

      fun hint(action: String, text: String, graphics: (Hint.() -> Node)? = null) =
         Hint(action, { text }, graphics, {}, {}).also { this += it }

      fun hint(action: String, text: () -> String, graphics: (Hint.() -> Node)? = null) =
         Hint(action, text, graphics, {}, {}).also { this += it }

      fun hint(action: String, text: () -> String, graphics: (Hint.() -> Node)?, onEnter: () -> Unit = {}, onExit: () -> Unit = {}) =
         Hint(action, text, graphics, onEnter, onExit).also { this += it }

      private fun layH(vararg graphics: Node) = layHorizontally(10.0, CENTER, *graphics)

      val h00_intro = hint("Intro",
         "Hi, this is guide for this application. It will show you around. " + "\n\nBut first let's play some music.",
         {
            layH(Icon(IconFA.MUSIC, ICON_SIZE, null) { _ ->
               val c = APP.windowManager.getActiveOrNew().switchPane.container
               val w = initialTemplateFactory.create()
               c.addChild(c.emptySpot, w)
               c.ui.alignTab(w)

               proceedIfActive()
            })
         }
      )
      val h01_GuideHints = hint("Guide hints",
         "Guide consists of hints. You can proceed manually or by completing a "
            + "hint. To navigate, use left and right mouse button."
            + "\n\nClick anywhere on this guide."
      )
      val h02_guideNav = hint("Navigation",
         "Navigation with mouse buttons is used across the entire app. Remember:\n"
            + "\n\t• Left click: go next"
            + "\n\t• Right click: go back"
            + "\n\nTry going back by right-clicking anywhere on this guide"
      )
      val h03_guideClose = hint("Guide closing",
         "Know your ESCAPE. It will help you. With what? Completing this guide. "
            + "And closing windows. And canceling table selections, filtering and searching and... You get the idea."
            + "\n\nClose guide to proceed (the guide will continue after you close it). Use close "
            + "button or ${"ESCAPE".pretty()} key."
      )
      val h04_guideOpen = hint("Guide opening",
         "Guide can be opened from app window header. It will resume "
            + "from where you left off."
            + "\nNow you will know where to go when you lose your way."
            + "\n\nClick on the guide button in the app window top header. It looks like: ",
         { layH(Icon(IconFA.GRADUATION_CAP, ICON_SIZE)) }
      )
      val h05_uiIcons = hint("Icons",
         "Icons, icons everywhere. Picture is worth a thousand words, they say."
            + "\nThe icons try to tell their function visually."
            + "\n\nOne icon leads to the next hint.",
         {
            layH(
               Icon(IconFA.WHEELCHAIR, ICON_SIZE).onClick { it -> (it.source as Icon).isDisable = true },
               Icon(IconMD.WALK, ICON_SIZE).onClick { it -> (it.source as Icon).isDisable = true },
               Icon(IconMD.RUN, ICON_SIZE).onClick { _ -> runFX(1500.millis) { proceedIfActive() } }
            )
         }
      )
      val h06_uiTooltips = hint("Tooltips",
         "Tooltips will teach you the way if the icon is not enough. Use them well."
            + "\n\nThere can be a meaning where you don't see it. Hover above the icons to find out.",
         {
            layH(
               Icon(IconMD.GAMEPAD_VARIANT, ICON_SIZE).tooltip("Now switch to tooltip of the icon to the right"),
               Icon(IconMD.HAND_POINTING_RIGHT, ICON_SIZE).tooltip("Tooltip switching does not take as long as showing a new one."),
               Icon(IconFA.GRADUATION_CAP, ICON_SIZE).tooltip("Click to claim the trophy")
                  .onClick { _ -> runFX(1000.millis) { proceedIfActive() } }
            )
         }
      )
      val h06_uiPopups = hint("Info popup",
         "There is more... Info buttons explain various app sections and how to "
            + "use them in more detail."
            + "\n\nSee the corner of this hint? Click the help button. It looks like:",
         { layH(Icon(IconFA.INFO, ICON_SIZE)) }
      )
      val h07_uiShortcuts = hint("Shortcuts",
         {
            ("Shortcuts are keys and key combinations that invoke some action."
               + "\n\nTo configure shortcuts, visit Settings > Shortcuts. Shortcuts can be global "
               + "or local. Global shortcuts will work even if the application has no focus."
               + "\n\nTo see all the available shortcuts, simply press "
               + ActionRegistrar["Show shortcuts"].keys.pretty() + ".")
         },
         null
      )
      val h07_moduleAll = hint("Modules",
         "The application consists of:\n"
            + "\n\t• Core"
            + "\n\t• Behavior"
            + "\n\t\t• Widgets"
            + "\n\t\t• Plugins"
            + "\n\t• UI (user interface) layout"
            + "\n\t\t• Windows"
            + "\n\t\t• Components"
            + "\n\t\t\t• Containers"
            + "\n\t\t\t• Widgets")
      val h08_moduleComponents = hint("Component",
         "Components compose the layout. There are graphical Widgets and "
            + "virtual Containers."
            + "\n\nComponents are:\n"
            + "\n\t• Independent - multiple instances of the same component type can run at the same time."
            + "\n\t• Exportable - each component can be exported into a small file and then launched "
            + "as a standalone application. More on this later."
      )
      val h09_moduleWidgetNew = hint("Widget",
         "Widget is a graphical component with some functionality, for example "
            + "Playlist widget. Using the application mostly about using the widgets.\n"
            + "\n\nWidgets are:\n"
            + "\n\t• Configurable - each widget instance has its own state and settings."
            + "\n\t• Functional units - Widget can have inputs and outputs and communicate with other widgets."
            + "\n\t• Pluggable - It is possible to load custom developed widgets easily."
      )
      val h10_moduleContainer = hint("Container",
         "Container is a non-graphical component. Containers group components "
            + "together so they can be manipulated as a group.\n"
            + "Containers are invisible and create nested hierarchy of components"
            + "\n\nTo create an UI and use widgets, containers are necessary. Every window "
            + "comes with a top level container. So it is only a matter of filling it with "
            + "content.")
      val h11_moduleWidgetNew = hint("New widget",
         "Creating widget involves:\n"
            + "\n\t• Deciding where to put it"
            + "\n\t• Creating appropriate container"
            + "\n\t• Adding widget to the container"
            + "\n\nClick anywhere within empty space (not inside widget) and choose 'Place widget' "
            + "to add new widget. All available widgets will display for you to choose from. "
            + "There is a widget called 'Empty' which contains no content. Select it.")
      val h12_moduleWidgetControl = hint("Widget control",
         "Widget controls are located in the widget header. It is displayed "
            + "automatically when mouse cursor enters right top corner of the widget."
            + "\nThis is known as layout mode. It is an overlay UI for manipulating the content. "
            + "There is an info button in the widget controls, so use it to learn more about what "
            + "the controls can do."
            + "\n\nMove the mouse cursor to the top right corner of the widget to display the controls.")
      val h13_layoutDivide = hint("Divide layout",
         "In order to create layout that best suits your needs, you need to create " +
            "more containers for storing the widgets, by dividing the layout - horizontally or vertically." +
            "\nThe orientation determines how the layout gets split by the divider and can be changed later. " +
            "The divider can be dragged by mouse to change the sizes of the sub containers." +
            "\n\nClick anywhere within empty space and choose one of the 'Split' choices.")
      val h14_layoutMode = hint("Layout mode",
         {
            "When widget header is visible, the widget is in layout mode. Layout mode is used " +
               "for advanced manipulation with the widget. In order to quickly make changes to the layout, layout " +
               "mode can be activated by shortcut." +
               "\n\nPress " + ActionRegistrar[Actions.LAYOUT_MODE].keys.pretty() + " to enter/leave layout mode"
         }
      )
      val h15_layoutMode = hint("Layout mode",
         {
            "For layout mode, there is also fast-shortcut reacting on key press and release." +
               "\n\nPress " + ActionManager.keyManageLayout.name.pretty() + " to temporarily enter layout mode. (If the shortcut " +
               "is empty (disabled) go to next hint manually)."
         }
      )
      val h16_layoutTraverse = hint("Layout traversal",
         "Container can be controlled just like a widget, but you need "
            + "to navigate to its controls first. Use mouse buttons:\n"
            + "\n\t• Right click: go 'up' - visit parent container"
            + "\n\t• Left click: go 'down' - visit children"
            + "\n\nTry out container navigation:",
         {
            layH(Icon(IconMD.PALETTE_ADVANCED, ICON_SIZE, "") { _ ->
               val w = APP.windowManager.getActiveOrNew()
               val i = w.topContainer.emptySpot
               w.topContainer.ui.alignTab(i)
               fut()
                  .thenWait(1.seconds)
                  .ui { w.topContainer.addChild(i, testControlContainer()) }
                  .thenWait(1.seconds)
                  .ui { APP.ui.isLayoutMode = true }
            }.withText("Start"))
         }
      )
      val h17_layoutLock = hint("Layout lock",
         {

            "Because automatic layout mode can be intrusive, the layout can be " +
               "locked. Locked layout will enter layout mode only with shortcut." +
               "\nYou may want to lock the layout after configuring it to your needs." +
               "\n\nClick on the lock button in the window header or press " +
               ActionRegistrar["Lock layout - toggle"].keys.pretty() + " to lock layout."

         }
      )
      val h18_layoutLock = hint("Widget layout lock",
         "Widgets and containers can be locked as well. Locking widget "
            + "is useful if automatic layout mode gets in the way of the particular widget. Locking "
            + "a container will lock all its children."
            + "\n\nWidget will behave as locked if it or any of its parent containers or whole layout "
            + "is locked. Thus, you can combine locks however you wish."
            + "\n\nClick on the lock button in any widget's header."
      )
      val h19_tableColumns = hint("Table columns",
         "Table columns have customizable:"
            + "\n\t• Width - drag column by its border. Some columns do not support this and "
            + "resize automatically."
            + "\n\t• Order - drag column to left or right"
            + "\n\t• Visibility - use table column menu to show/hide columns"
            + "\n\nSome tables group items by some column (similar to SQL). In such case, the "
            + "column to group by can be specified in the table column menu"
            + "\n\nTo show table column menu click on any table column with right mouse button"
      )
      val h20_tableSearch = hint("Table search",
         "To look up item in a table, focus the table and simply type what "
            + "you are looking for."
            + "\n\nTable search is customizable (Settings > Table). Search works as follows:\n"
            + "\n\t• Search looks for matches in specified column. The column can be set for "
            + "each table in the table column menu (right click on the table column headers). "
            + "Only columns that display text are available."
            + "\n\t• Search is closed after specific period of inactivity or on ${"ESCAPE".pretty()} press"
            + "\n\t• Search query is reset after a specific delay after the last key press"
            + "\n\t• Table will scroll the row with the first match to the center"
            + "\n\t• All rows with a match will be highlighted"
            + "\n\t• Search algorithm can be chosen"
            + "\n\t• Searching can be case sensitive or insensitive"
      )
      val h21_tableFilter = hint("Table filter",
         "Tables support powerful filters. Use ${"CTRL+F".pretty()} to show/hide "
            + "the filter."
            + "\nFiltering is different from searching. Filtering affects the actual table content."
            + "\nFiltering works by specifying a condition/rule. Multiple conditions can be created "
            + "and chained."
            + "\n\nEach filter:\n"
            + "\n\t• Can be individually disabled and enabled"
            + "\n\t• Can be negated, so it has the opposite effect"
            + "\n\t• Filters items by specified column using a rule, chosen from rule "
            + "set of available rules. The rule set depends on the type of content."
      )
      val h22_tableEscape = hint("Table ESCAPE",
         "Here is a comprehensive guide to using ${"ESCAPE".pretty()} key in a table. When "
            + "${"ESCAPE".pretty()} is pressed, one of the following scenarios will happen (in this order):\n"
            + "\n\t• If search is active, search will be closed"
            + "\n\t• If filtering is active and not empty it will be cleared. Table will show all items."
            + "\n\t• If filtering is active and empty, it will be hidden"
            + "\n\t• All selected items will be unselected."
      )
      val h23_dragDrop = hint("Drag & drop",
         "Many widgets support drag & drop for various content: files, text, etc. "
            + "When you drag some content, any drag supporting area will signal that it can accept the "
            + "content when you drag over it and show the type of action that will execute."
            + "\n\nIn regards to drag & drop, are can:\n"
            + "\n\t• Ignore drag (if area does not accept drags)"
            + "\n\t• Accept drag (if the drag content matches)."
            + "\n\t• Refuse drag (if drag matches, but some condition is not met, e.g., dragging from "
            + "self to self is usually forbidden)."
            + "\n\nDrag areas may cover each other! Therefore, if an accept signal is visible, moving "
            + "the mouse within the area can still activate different area (child area)."
            + "\n\nBelow you can start a tutorial and see the drag behavior by dragging '2' or '3' onto the test UI",
         {
            layH(
               Icon(IconMD.PALETTE_ADVANCED, ICON_SIZE, "") { _ ->
                  val wd = APP.windowManager.getActiveOrNew()
                  val i = wd.topContainer.emptySpot
                  wd.topContainer.ui.alignTab(i)

                  runFX(1000.millis) {
                     val w = emptyWidgetFactory.create()
                     val root = BiContainer(HORIZONTAL)
                     root.addChild(1, w)
                     wd.topContainer.addChild(i, root)

                     installDrag(
                        w.load(),
                        IconMD.DICE_2,
                        ("Accepts text containing digit '2' and does nothing"
                           + "\n\t• Release mouse to drop drag and execute action"
                           + "\n\t• Continue moving to try elsewhere"),
                        { e -> e.dragboard.hasText() && e.dragboard.getText().let { "2" in it } },
                        { }
                     )
                     installDrag(
                        root.root,
                        IconMD.DICE_2,
                        ("Accepts text containing digit '2' or '3' and does nothing"
                           + "\n\t• Release mouse to drop drag and execute action"
                           + "\n\t• Continue moving to try elsewhere"),
                        { e -> e.dragboard.hasText() && e.dragboard.getText().let { "2" in it || "3" in it } },
                        { }
                     )
                  }
               }.withText("Start"),
               Icon(IconMD.DICE_2, ICON_SIZE).apply {
                  onDragDetected = EventHandler {
                     startDragAndDrop(COPY).setContent(mapOf(DataFormat.PLAIN_TEXT to "2"))
                     it.consume()
                  }
               }.withText("Drag '2'"),
               Icon(IconMD.DICE_3, ICON_SIZE).apply {
                  onDragDetected = EventHandler {
                     startDragAndDrop(COPY).setContent(mapOf(DataFormat.PLAIN_TEXT to "3"))
                     it.consume()
                  }
               }.withText("Drag '3'")
            )
         }
      )
      val h24_widgetSwitch = hint("Component switching",
         "Components (widgets or containers) can be switched among each "
            + "other. This is accomplished simply by drag & drop in layout mode."
            + "\n\nSteps:\n"
            + "\n\t• Enter layout mode of the widget or container."
            + "\n\t• Drag the component."
            + "\n\t• Drop the container on different component."
            + "\n\nThis allows fast content switching and layout customization. To make it faster, "
            + "components can be dragged by their header - you do not have to enter layout mode. "
            + "Simply enter right top corner of the component and drag the component by its header."
      )
   }

}