package sp.it.pl.gui

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.FXCollections.observableSet
import javafx.geometry.NodeOrientation
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Skin
import javafx.scene.control.Tooltip
import javafx.scene.input.KeyCode.TAB
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.text.Font
import mu.KLogging
import sp.it.pl.gui.objects.rating.Rating
import sp.it.pl.gui.objects.window.stage.asLayout
import sp.it.pl.gui.pane.ActionPane
import sp.it.pl.gui.pane.ErrorPane
import sp.it.pl.gui.pane.InfoPane
import sp.it.pl.gui.pane.OverlayPane
import sp.it.pl.gui.pane.OverlayPane.Display
import sp.it.pl.gui.pane.ScreenBgrGetter
import sp.it.pl.gui.pane.ShortcutPane
import sp.it.pl.layout.widget.WidgetSource.OPEN
import sp.it.pl.layout.widget.Widgets
import sp.it.pl.main.APP
import sp.it.pl.main.Actions
import sp.it.pl.main.AppSettings.ui.skin
import sp.it.pl.main.AppSettings.ui.view.actionViewer.closeWhenActionEnds
import sp.it.pl.main.AppSettings.ui.view.overlayArea
import sp.it.pl.main.AppSettings.ui.view.overlayBackground
import sp.it.pl.main.AppSettings.ui.view.shortcutViewer.hideUnassignedShortcuts
import sp.it.pl.main.initActionPane
import sp.it.pl.main.initApp
import sp.it.util.access.Values
import sp.it.util.access.toggle
import sp.it.util.action.IsAction
import sp.it.util.collections.ObservableSetRO
import sp.it.util.collections.project
import sp.it.util.collections.setTo
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.conf.appendInfo
import sp.it.util.conf.between
import sp.it.util.conf.c
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.readOnlyUnless
import sp.it.util.conf.uiConverter
import sp.it.util.conf.values
import sp.it.util.conf.valuesIn
import sp.it.util.file.FileMonitor
import sp.it.util.file.Util
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.file.isAnyParentOf
import sp.it.util.file.writeTextTry
import sp.it.util.functional.Util.set
import sp.it.util.functional.orNull
import sp.it.util.reactive.attach
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.onItemAdded
import sp.it.util.reactive.onItemSyncWhile
import sp.it.util.reactive.plus
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncNonNullIntoWhile
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.ui.isAnyParentOf
import sp.it.util.ui.setFontAsStyle
import sp.it.util.units.millis
import java.io.File
import java.net.MalformedURLException
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName
import javafx.stage.Window as WindowFX
import sp.it.pl.main.AppSettings.ui as confUi
import sp.it.pl.main.AppSettings.ui.image as confImage
import sp.it.pl.main.AppSettings.ui.table as confTable

class UiManager(val skinDir: File): GlobalSubConfigDelegator(confUi.name) {

   /** Action chooser and data info view. */
   val actionPane = LazyOverlayPane { ActionPane(APP.className, APP.instanceName, APP.instanceInfo).initApp().initActionPane() }
   /** Error detail view. Usually used internally by [sp.it.pl.main.AppErrors]. */
   val errorPane = LazyOverlayPane { ErrorPane().initApp() }
   /** Shortcut bindings/keymap detail view. */
   val shortcutPane = LazyOverlayPane { ShortcutPane().initApp() }
   /** System/app info detail view. */
   val infoPane = LazyOverlayPane { InfoPane().initApp() }
   /** Css files applied on top of [skin]. Can be used for clever stuff like applying generated css. */
   val additionalStylesheets = observableArrayList<File>()!!
   /** Available application skins. Monitored and updated from disc. */
   private val skinsImpl = observableSet<SkinCss>()

   /** Available application skins. Monitored and updated from disc. */
   val skins = ObservableSetRO<SkinCss>(skinsImpl)

   init {
      skinsImpl setTo findSkins()
   }

   /** Skin of the application. Defined stylesheet file to be applied on `.root` of windows. */
   val skin by cv("Main").values(skins.project { it.name }) def confUi.skin
   /** Font of the application. Overrides `-fx-font-family` and `-fx-font-size` defined by css on `.root`. */
   val font by cv(Font.getDefault()) def confUi.font

   init {
      monitorSkinFiles()
      observeWindowsAndSyncSkin()
      observeWindowsAndSyncWidgetFocus()
   }

   val viewDisplay by cv(Display.SCREEN_OF_MOUSE) def overlayArea
   val viewDisplayBgr by cv(ScreenBgrGetter.SCREEN_BGR) def overlayBackground.appendInfo("\nIgnored when `${overlayArea.name}` is `${Display.WINDOW.nameUi}`")
   val viewCloseOnDone by cv(true) def closeWhenActionEnds
   val viewHideEmptyShortcuts by cv(true) def hideUnassignedShortcuts

   /** Application layout mode. When true, ui editing controls are visible. */
   val layoutMode: BooleanProperty = SimpleBooleanProperty(false)
   val layoutModeBlur by cv(false) def confUi.layoutModeBlurBgr
   val layoutModeOpacity by cv(true) def confUi.layoutModeFadeBgr
   var layoutModeOpacityStrength by c(1.0).between(0.0, 1.0).readOnlyUnless(layoutModeOpacity) def confUi.layoutModeFadeIntensity
   var layoutModeBlurStrength by c(4.0).between(0.0, 20.0).readOnlyUnless(layoutModeBlur) def confUi.layoutModeBlurIntensity
   var layoutModeDuration by c(250.millis) def confUi.layoutModeAnimLength
   val layoutLocked by cv(false) { SimpleBooleanProperty(it) } attach { APP.actionStream("Layout lock") } def confUi.lockLayout
   val snapping by cv(true) def confUi.snap
   val snapDistance by cv(12.0) def confUi.snapActivationDistance

   val tableOrient by cv(NodeOrientation.INHERIT) def confTable.tableOrientation
   val tableZeropad by cv(false) def confTable.zeropadNumbers
   val tableOrigIndex by cv(false) def confTable.searchShowOriginalIndex
   val tableShowHeader by cv(true) def confTable.showTableHeader
   val tableShowFooter by cv(true) def confTable.showTableControls

   val thumbnailAnimDur by cv(100.millis) def confImage.thumbnailAnimDuration

   val ratingIconCount by cv(5).between(0, 10) def confUi.ratingIconAmount
   val ratingIsPartial by cv(true) def confUi.ratingAllowPartial
   val ratingSkin by cvn<KClass<out Skin<Rating>>>(null).valuesIn(APP.instances).uiConverter {
      it?.simpleName ?: "<none> (App skin decides)"
   } def confUi.ratingSkin sync {
      val f = APP.locationTmp/"user-rating-skin.css"
      additionalStylesheets -= f
      it?.let {
         f.writeTextTry(""".rating { -fx-skin: "${it.jvmName}"; }""", Charsets.UTF_8)
            .ifError { logger.error(it) { "Failed to apply rating skin=$it" } }
         additionalStylesheets += f
      }
   }


   /**
    * Sets layout mode for all active components.
    *
    * Note that this method consistently returns FALSE at the time
    * of entering and leaving the layout mode, thus allowing to safely query
    * layout mode state just before the state change.
    *
    * @return whether layout mode is on or not.
    */
   var isLayoutMode: Boolean
      get() = layoutMode.get()
      set(v) {
         if (layoutMode.get()==v) return
         if (v) {
            APP.widgetManager.layouts.findAll(OPEN).forEach { it.show() }
            layoutMode.set(v)
         } else {
            layoutMode.set(v)
            APP.widgetManager.layouts.findAll(OPEN).forEach { it.hide() }
            setZoomMode(false)
         }
         if (v) APP.actionStream(Actions.LAYOUT_MODE)
      }

   fun focusClickedWidget(e: MouseEvent) {
      val n = e.target as? Node
      if (n!=null)
         APP.widgetManager.widgets.findAll(OPEN)
            .find { !it.focused.value && it.isLoaded && it.load().isAnyParentOf(n) }
            ?.focus()
   }

   /** Toggles lock to prevent user accidental layout change.  */
   @IsAction(name = "Toggle layout lock", desc = "Lock/unlock layout.", keys = "F4")
   fun toggleLayoutLocked() = layoutLocked.toggle()

   /** Loads/refreshes active layout.  */
   @IsAction(name = "Reload layout", desc = "Reload layout.", keys = "F6")
   fun loadLayout() = APP.widgetManager.layouts.findAll(OPEN).forEach { it.load() }

   /** Toggles layout controlling mode.  */
   @IsAction(name = "Reload skin", desc = "Reloads skin.", keys = "F7")
   fun reloadSkin() {
      logger.info("Reloading skin={}", skin.value)
      applySkin(skin.value)
   }

   @IsAction(name = "Show application", desc = "Equal to switching minimized mode.", global = true)
   fun minimizeFocusTrue() {
      val anyM = APP.windowManager.windows.any { it.isMinimized }
      val anyF = APP.windowManager.windows.any { it.focused.value }
      if (anyM || !anyF) APP.windowManager.windows.forEach { it.isMinimized = false; it.focus() }
   }

   @IsAction(name = "Show/Hide application", desc = "Equal to switching minimized mode.", keys = "CTRL+ALT+W", global = true)
   fun toggleMinimizeFocus() {
      val anyM = APP.windowManager.windows.any { it.isMinimized }
      val anyF = APP.windowManager.windows.any { it.focused.value }
      if (!anyM && anyF) APP.windowManager.windows.forEach { it.isMinimized = true }
      else APP.windowManager.windows.forEach { it.isMinimized = false; it.focus() }
   }

   @IsAction(name = "Show all windows", desc = "Shows all application windows.", global = true)
   fun showApp() = APP.windowManager.windows.forEach { it.isMinimized = false }

   @IsAction(name = "Hide all windows", desc = "Hides all application windows.", global = true)
   fun hideApp() = APP.windowManager.windows.forEach { it.isMinimized = true }

   @IsAction(name = "Show/hide all windows", desc = "Shows/hides all application windows.", global = true)
   fun toggleMinimize() {
      val m = APP.windowManager.windows.any { it.isMinimized }
      APP.windowManager.windows.forEach {
         it.isMinimized = !m
         if (m) it.focus()
      }
   }

   @IsAction(name = "Maximize window", desc = "Switch maximized mode.", keys = "F11")
   fun toggleMaximize() = APP.windowManager.getActive().orNull()?.toggleMaximize()

   @IsAction(name = "Loop maximized state", desc = "Switch to different maximized window states.", keys = "F3")
   fun toggleMaximizedState() = APP.windowManager.getActive().orNull()?.let { it.isMaximized = Values.next(it.isMaximized) }

   @IsAction(name = "Fullscreen", desc = "Switch fullscreen mode.", keys = "F12")
   fun toggleFullscreen() = APP.windowManager.getActive().orNull()?.toggleFullscreen()

   @IsAction(name = "Layout align", desc = "Aligns layout of the active window", keys = "ALT+UP")
   fun tabAlign() = APP.windowManager.getActive().orNull()?.switchPane?.alignTabs()

   @IsAction(name = "Layout move left", desc = "Moves layout of the active window to the left.", keys = "ALT+LEFT")
   fun tabPrevious() = APP.windowManager.getActive().orNull()?.switchPane?.alignLeftTab()

   @IsAction(name = "Layout move right", desc = "Moves layout of the active window to the right.", keys = "ALT+RIGHT")
   fun tabNext() = APP.windowManager.getActive().orNull()?.switchPane?.alignRightTab()

   @IsAction(name = Actions.LAYOUT_MODE, desc = "Shows/hides layout overlay.", keys = "F8")
   fun toggleLayoutMode() {
      isLayoutMode = !layoutMode.get()
   }

   @IsAction(name = "Layout zoom in/out", desc = "Toggles layout zoom in/out.")
   fun toggleZoomMode() = APP.windowManager.getActive().orNull()?.switchPane?.toggleZoom()

   fun setLayoutNzoom(v: Boolean) {
      if (isLayoutMode && APP.windowManager.getActive().orNull()?.switchPane?.isZoomed!=true) {
         setZoomMode(true)
      } else {
         isLayoutMode = v
         setZoomMode(v)
      }
   }

   fun setZoomMode(v: Boolean) = APP.windowManager.getActive().orNull()?.switchPane?.zoom(v)

   @IsAction(name = "Layout zoom overlay in/out", desc = "Shows/hides layout overlay & zooms in/out.", keys = "ALT+DOWN")
   fun toggleLayoutNzoom() {
      setLayoutNzoom(!layoutMode.value)
   }

   /**
    * Searches for .css files in skin folder and registers them as available
    * skins. Use on app start or to discover newly added layouts dynamically.
    */
   fun findSkins(): Set<SkinCss> {
      if (!Util.isValidatedDirectory(skinDir)) {
         logger.error("Skin lookup failed." + skinDir.path + " could not be accessed.")
         return set()
      }

      return skinDir.children()
         .filter { it.isDirectory }
         .mapNotNull {
            val name = it.name
            val css = File(it, "$name.css")
            if (Util.isValidFile(css)) {
               logger.info("Registering skin: $name")
               SkinCss(css)
            } else {
               null
            }
         }
         .toSet()
   }

   private fun registerSkin(s: SkinCss): SkinCss {
      logger.info("Registering skin={}", s.name)
      skinsImpl += s
      return s
   }

   fun setSkin(s: SkinCss) {
      logger.info("Setting skin={}", s.name)

      registerSkin(s)
      skin.value = s.name
   }

   fun setSkin(cssFile: File) {
      logger.info("Setting skin file={}", cssFile)

      val s = skins.stream()
         .filter { (_, file) -> file==cssFile }.findAny()
         .orElseGet { registerSkin(SkinCss(cssFile)) }
      setSkin(s)
   }

   private fun monitorSkinFiles() {
      FileMonitor.monitorDirectory(skinDir, true) { type, file ->
         logger.info { "Change=$type detected in skin directory for $file" }

         skinsImpl setTo findSkins()

         val refreshAlways = true    // skins may import each other hence it is more convenient to refresh always
         val currentSkinDir = skinDir/skin.value
         val isActive = currentSkinDir isAnyParentOf file
         if (isActive || refreshAlways) reloadSkin()
      }
   }

   private fun observeWindowsAndSyncWidgetFocus() {
      WindowFX.getWindows().onItemSyncWhile { window ->
         window.sceneProperty().syncNonNullWhile { scene ->
            val s1 = scene.focusOwnerProperty() attach { Widgets.focusChangedHandler(it) }
            val s2 = scene.rootProperty() syncNonNullWhile { root ->
               val ss1 = root.onEventUp(MOUSE_PRESSED) { e ->
                  if (e.button==MouseButton.PRIMARY)
                     APP.ui.focusClickedWidget(e)
               }
               val ss2 = root.onEventUp(KEY_PRESSED) { e ->
                  if (e.code==TAB && e.isShortcutDown) {
                     e.consume()

                     val layout = window.asLayout()
                     if (layout!=null) {
                        if (e.isShiftDown)
                           APP.widgetManager.widgets.selectPreviousWidget(layout)
                        else
                           APP.widgetManager.widgets.selectNextWidget(layout)
                     }
                  }
               }
               ss1 + ss2
            }
            s1 + s2
         }
      }
      Tooltip.getWindows().onItemAdded { (it as? Tooltip)?.font = font.value }
   }

   private fun observeWindowsAndSyncSkin() {
      WindowFX.getWindows().onItemSyncWhile {
         it.sceneProperty().syncNonNullIntoWhile(Scene::rootProperty) { root ->
            val s2 = skin sync { root.applySkinGui(it) }
            val s1 = font sync { root.applyFontGui(it) }
            val s3 = additionalStylesheets.onChange { root.applySkinGui(skin.value) }
            s1 + s2 + s3
         }
      }
      Tooltip.getWindows().onItemAdded { (it as? Tooltip)?.font = font.value }
   }

   fun applySkin(skin: String) {
      WindowFX.getWindows().forEach { it.scene?.root?.applySkinGui(skin) }
   }

   private fun Parent.applyFontGui(font: Font) {
      setFontAsStyle(font)
   }

   private fun Parent.applySkinGui(skin: String) {
      stylesheets.clear()
      this addStyleSheet skinDir/skin/"$skin.css"
      additionalStylesheets.forEach { this addStyleSheet it }
   }

   data class SkinCss(val name: String, val file: File) {
      constructor(cssFile: File): this(cssFile.nameWithoutExtension, cssFile)
   }

   enum class OpenStrategy {
      POPUP, INSIDE
   }

   companion object: KLogging() {

      private fun File.toStyleSheet() = try {
         toURI().toURL().toExternalForm()
      } catch (e: MalformedURLException) {
         logger.error(e) { "Could not load css file $this" }
         null
      }

      private infix fun Parent.addStyleSheet(file: File) = file.toStyleSheet()?.let { stylesheets += it }

   }

}

class LazyOverlayPane<OT, T: OverlayPane<OT>>(private val builder: () -> T) {
   private var pane: T? = null

   val orBuild: T
      get() {
         val p = pane ?: builder()
         pane = p
         return p
      }

   val orNull: T?
      get() = pane

   fun isShown() = orNull?.isShown()==true
   fun show(ot: OT) = orBuild.show(ot)
   fun hide() = pane?.hide() ?: Unit
}