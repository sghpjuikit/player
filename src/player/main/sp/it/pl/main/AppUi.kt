package sp.it.pl.main

import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections.observableSet
import javafx.geometry.NodeOrientation
import javafx.scene.Node
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
import sp.it.pl.layout.container.SwitchContainer
import sp.it.pl.layout.widget.WidgetSource.OPEN
import sp.it.pl.main.AppSettings.ui.skin
import sp.it.pl.main.AppSettings.ui.view.actionViewer.closeWhenActionEnds
import sp.it.pl.main.AppSettings.ui.view.overlayArea
import sp.it.pl.main.AppSettings.ui.view.overlayBackground
import sp.it.pl.main.AppSettings.ui.view.shortcutViewer.hideUnassignedShortcuts
import sp.it.pl.ui.objects.rating.Rating
import sp.it.pl.ui.objects.window.stage.asLayout
import sp.it.pl.ui.pane.ActionPane
import sp.it.pl.ui.pane.ErrorPane
import sp.it.pl.ui.pane.InfoPane
import sp.it.pl.ui.pane.OverlayPane
import sp.it.pl.ui.pane.OverlayPane.Display
import sp.it.pl.ui.pane.ScreenBgrGetter
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.access.toggle
import sp.it.util.action.IsAction
import sp.it.util.collections.project
import sp.it.util.collections.readOnly
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
import sp.it.util.functional.asIf
import sp.it.util.functional.traverse
import sp.it.util.reactive.Handler0
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.onItemAdded
import sp.it.util.reactive.onItemSyncWhile
import sp.it.util.reactive.plus
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.ui.asStyle
import sp.it.util.ui.isAnyParentOf
import sp.it.util.units.millis
import java.io.File
import java.net.MalformedURLException
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName
import javafx.stage.Window as WindowFX
import sp.it.pl.main.AppSettings.ui as confUi
import sp.it.pl.main.AppSettings.ui.image as confImage
import sp.it.pl.main.AppSettings.ui.table as confTable
import sp.it.pl.main.AppSettings.ui.grid as confGrid
import sp.it.pl.main.AppSettings.ui.form as confForm
import sp.it.pl.layout.widget.Widget
import sp.it.pl.ui.objects.grid.GridView.CellGap
import sp.it.pl.ui.pane.ConfigPane
import sp.it.util.access.readOnly
import sp.it.util.access.v
import sp.it.util.conf.Constraint
import sp.it.util.conf.butElement
import sp.it.util.conf.cList
import sp.it.util.file.FileType.FILE
import sp.it.util.reactive.onItemRemoved
import sp.it.util.units.em

class AppUi(val skinDir: File): GlobalSubConfigDelegator(confUi.name) {

   /** Action chooser and data info view. */
   val actionPane = LazyOverlayPane { ActionPane(APP.className, APP.instanceName, APP.instanceInfo).initApp().initActionPane() }
   /** Error detail view. Usually used internally by [sp.it.pl.main.AppEventLog]. */
   val errorPane = LazyOverlayPane { ErrorPane().initApp() }
   /** Shortcut bindings/keymap detail view. */
   val shortcutPane = LazyOverlayPane { ShortcutPane().initApp() }
   /** System/app info detail view. */
   val infoPane = LazyOverlayPane { InfoPane().initApp() }
   /** Css files applied on top of [skin]. Can be used for clever stuff like applying generated css. */
   val additionalStylesheets = AdditionalStylesheets()
   /** Available application skins. Monitored and updated from disc. */
   private val skinsImpl = observableSet<SkinCss>()

   /** Available application skins. Monitored and updated from disc. */
   val skins = skinsImpl.readOnly()

   init {
      skinsImpl setTo findSkins()
   }

   /** Skin of the application. Determines single stylesheet file applied on `.root` of all windows. */
   val skin by cv("Main").values(skins.project { it.name }) def confUi.skin
   /** Additional stylesheet files applied on `.root` of all windows. Override styles set by the skin. Applied in the specified order. */
   val skinExtensions by cList<File>().butElement(Constraint.FileActor(FILE)) def confUi.skinExtensions
   /** Font of the application. Overrides `-fx-font-family` and `-fx-font-size` defined by css on `.root`. */
   val font by cvn<Font>(null) def confUi.font sync {
      val f = APP.locationTmp/"user-font-skin.css"
      if (it==null) {
         additionalStylesheets -= f
      } else {
         val style = it.asStyle(1.em)
         f.writeTextTry(style).ifError { logger.error(it) { "Failed to apply font skin=$it" } }
         additionalStylesheets += f
      }
   }

   init {
      monitorSkinFiles()
      observeWindowsAndSyncSkin()
      observeWindowsAndSyncWidgetFocus()
      skinExtensions.forEach { additionalStylesheets += it }
      skinExtensions.onItemAdded { additionalStylesheets += it }
      skinExtensions.onItemRemoved { additionalStylesheets -= it }
   }

   /** [overlayArea] */
   val viewDisplay by cv(Display.SCREEN_OF_MOUSE) def overlayArea
   /** [overlayBackground] */
   val viewDisplayBgr by cv(ScreenBgrGetter.SCREEN_BGR) def overlayBackground.appendInfo("\nIgnored when `${overlayArea.name}` is `${Display.WINDOW.nameUi}`")
   /** [closeWhenActionEnds] */
   val viewCloseOnDone by cv(true) def closeWhenActionEnds
   /** [hideUnassignedShortcuts] */
   val viewHideEmptyShortcuts by cv(true) def hideUnassignedShortcuts

   /** [layoutModeImpl] */
   val layoutModeImpl = v(false)
   /** Application layout mode. When true, ui editing controls are visible. Changes state immediately after showing and immediately before hiding.  */
   val layoutMode = layoutModeImpl.readOnly()
   /** [layoutModeBroadImpl] */
   val layoutModeBroadImpl = v(false)
   /** Application layout mode. When true, ui editing controls are visible. Changes state immediately before showing and immediately after hiding. */
   val layoutModeBroad = layoutModeBroadImpl.readOnly()
   /** [confUi.layoutModeBlurBgr] */
   val layoutModeBlur by cv(false) def confUi.layoutModeBlurBgr
   /** [confUi.layoutModeFadeBgr] */
   val layoutModeOpacity by cv(true) def confUi.layoutModeFadeBgr
   /** [confUi.layoutModeFadeIntensity] */
   var layoutModeOpacityStrength by c(1.0).between(0.0, 1.0).readOnlyUnless(layoutModeOpacity) def confUi.layoutModeFadeIntensity
   /** [confUi.layoutModeBlurIntensity] */
   var layoutModeBlurStrength by c(4.0).between(0.0, 20.0).readOnlyUnless(layoutModeBlur) def confUi.layoutModeBlurIntensity
   /** [confUi.layoutModeAnimLength] */
   var layoutModeDuration by c(250.millis) def confUi.layoutModeAnimLength
   /** [confUi.lockLayout] */
   val layoutLocked by cv(false) { SimpleBooleanProperty(it) } attach { APP.actionStream("Layout lock") } def confUi.lockLayout
   /** [confUi.snap] */
   val snapping by cv(true) def confUi.snap
   /** [confUi.snapActivationDistance] */
   val snapDistance by cv(12.0) def confUi.snapActivationDistance

   /** [confForm.layout] */
   val formLayout by cv(ConfigPane.uiDefault) def confForm.layout

   /** [confTable.tableOrientation] */
   val tableOrient by cv(NodeOrientation.INHERIT) def confTable.tableOrientation
   /** [confTable.zeropadNumbers] */
   val tableZeropad by cv(false) def confTable.zeropadNumbers
   /** [confTable.searchShowOriginalIndex] */
   val tableOrigIndex by cv(false) def confTable.searchShowOriginalIndex
   /** [confTable.showTableHeader] */
   val tableShowHeader by cv(true) def confTable.showTableHeader
   /** [confTable.showTableFooter] */
   val tableShowFooter by cv(true) def confTable.showTableFooter

   /** [confGrid.cellAlignment] */
   val gridCellAlignment by cv<CellGap>(CellGap.CENTER) def confGrid.cellAlignment
   /** [confGrid.showGridFooter] */
   val gridShowFooter by cv(true) def confGrid.showGridFooter

   /** [confImage.thumbnailAnimDuration] */
   val thumbnailAnimDur by cv(100.millis) def confImage.thumbnailAnimDuration

   /** [confUi.ratingIconAmount] */
   val ratingIconCount by cv(5).between(0, 10) def confUi.ratingIconAmount
   /** [confUi.ratingAllowPartial] */
   val ratingIsPartial by cv(true) def confUi.ratingAllowPartial
   /** [confUi.ratingSkin] */
   val ratingSkin by cvn<KClass<out Skin<Rating>>>(null).valuesIn(APP.instances).uiConverter {
      it?.simpleName ?: "<none> (App skin decides)"
   } def confUi.ratingSkin sync {
      val f = APP.locationTmp/"user-rating-skin.css"
      if (it==null) {
         additionalStylesheets -= f
      } else {
         val style = """.rating { -fx-skin: "${it.jvmName}"; }"""
         f.writeTextTry(style).ifError { logger.error(it) { "Failed to apply rating skin=$it" } }
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
      get() = layoutMode.value
      set(v) {
         if (layoutMode.value==v) return
         if (v) {
            layoutModeBroadImpl.value = v
            APP.widgetManager.layouts.findAll(OPEN).forEach { it.show() }
            layoutModeImpl.value = v
         } else {
            layoutModeImpl.value = v
            APP.widgetManager.layouts.findAll(OPEN).forEach { it.hide() }
            layoutModeBroadImpl.value = v
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

   /** Toggles lock to prevent user accidental layout change. */
   @IsAction(
      name = "Toggle layout lock",
      info = "Lock layout\n\nRestricts certain layout operations to prevent accidents and configuration getting in the way. " +
             "Widgets, containers and layouts can also be locked individually.",
      keys = "F4"
   )
   fun toggleLayoutLocked() = layoutLocked.toggle()

   /** Loads/refreshes active layout. */
   @IsAction(name = "Reload layout", info = "Reload layout.", keys = "F6")
   fun loadLayout() = APP.widgetManager.layouts.findAll(OPEN).forEach { it.load() }

   /** Toggles layout controlling mode.  */
   @IsAction(name = "Reload skin", info = "Reloads skin.", keys = "F7")
   fun reloadSkin() {
      logger.info("Reloading skin={}", skin.value)
      applySkin(skin.value)
   }

   @IsAction(name = "Show application", info = "Equal to switching minimized mode.", global = true)
   fun minimizeFocusTrue() {
      val anyM = APP.windowManager.windows.any { it.isMinimized }
      val anyF = APP.windowManager.windows.any { it.focused.value }
      if (anyM || !anyF) APP.windowManager.windows.forEach { it.isMinimized = false; it.focus() }
   }

   @IsAction(name = "Show/Hide application", info = "Equal to switching minimized mode.", keys = "CTRL+ALT+W", global = true)
   fun toggleMinimizeFocus() {
      val anyM = APP.windowManager.windows.any { it.isMinimized }
      val anyF = APP.windowManager.windows.any { it.focused.value }
      if (!anyM && anyF) APP.windowManager.windows.forEach { it.isMinimized = true }
      else APP.windowManager.windows.forEach { it.isMinimized = false; it.focus() }
   }

   @IsAction(name = "Show all windows", info = "Shows all application windows.", global = true)
   fun showApp() = APP.windowManager.windows.forEach { it.isMinimized = false }

   @IsAction(name = "Hide all windows", info = "Hides all application windows.", global = true)
   fun hideApp() = APP.windowManager.windows.forEach { it.isMinimized = true }

   @IsAction(name = "Show/hide all windows", info = "Shows/hides all application windows.", global = true)
   fun toggleMinimize() {
      val m = APP.windowManager.windows.any { it.isMinimized }
      APP.windowManager.windows.forEach {
         it.isMinimized = !m
         if (m) it.focus()
      }
   }

   @IsAction(name = "Layout align", info = "Aligns layout of the active window", keys = "ALT+UP")
   fun tabAlign() = APP.windowManager.getActive()?.switchPane?.alignTabs()

   @IsAction(name = "Layout move left", info = "Moves layout of the active window to the left.", keys = "ALT+LEFT")
   fun tabPrevious() = APP.windowManager.getActive()?.switchPane?.alignLeftTab()

   @IsAction(name = "Layout move right", info = "Moves layout of the active window to the right.", keys = "ALT+RIGHT")
   fun tabNext() = APP.windowManager.getActive()?.switchPane?.alignRightTab()

   @IsAction(name = Actions.LAYOUT_MODE, info = "Shows/hides layout overlay.", keys = "F8")
   fun toggleLayoutMode() {
      isLayoutMode = !layoutMode.value
   }

   @IsAction(name = "Layout zoom in/out", info = "Toggles layout zoom in/out.")
   fun toggleZoomMode() = APP.windowManager.getActive()?.switchPane?.toggleZoom()

   fun setLayoutNzoom(v: Boolean) {
      if (isLayoutMode && APP.windowManager.getActive()?.switchPane?.isZoomed!=true) {
         setZoomMode(true)
      } else {
         isLayoutMode = v
         setZoomMode(v)
      }
   }

   fun setZoomMode(v: Boolean) {
      if (v) APP.windowManager.getActive()?.switchPane?.zoom(v)
      else APP.widgetManager.layouts.findAll(OPEN).forEach { it.child.asIf<SwitchContainer>()?.ui?.zoom(v) }
   }

   @IsAction(name = "Layout zoom overlay in/out", info = "Shows/hides layout overlay & zooms in/out.", keys = "ALT+DOWN")
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

         val skinFile = file.traverse { it.parentFile }.find { it.parentFile==skinDir }?.name?.let { name -> skinsImpl.find { it.name==name} }?.file
         if (file!=skinFile) {
            skinFile?.setLastModified(System.currentTimeMillis())
            return@monitorDirectory
         }

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
            var allowTraversal = false
            val s1 = scene.focusOwnerProperty() attach { Widget.focusChangedHandler(it, allowTraversal) }
            val s2 = scene.rootProperty() syncNonNullWhile { root ->
               val ss1 = root.onEventUp(MOUSE_PRESSED) { e ->
                  allowTraversal = false
                  if (e.button==MouseButton.PRIMARY)
                     APP.ui.focusClickedWidget(e)
               }
               val ss2 = root.onEventUp(KEY_PRESSED) { e ->
                  allowTraversal = true
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
      Tooltip.getWindows().onItemAdded { if (font.value != null) (it as? Tooltip)?.font = font.value }
   }

   private fun observeWindowsAndSyncSkin() {
      WindowFX.getWindows().onItemSyncWhile {
         it.sceneProperty().syncNonNullWhile { scene ->
            val s1 = skin sync { scene.applySkinGui(it) }
            val s2 = additionalStylesheets.onChange attach {
               scene.applySkinGui(skin.value)
            }
            s1 + s2
         }
      }
      Tooltip.getWindows().onItemAdded { if (font.value != null) (it as? Tooltip)?.font = font.value }
   }

   fun applySkin(skin: String) {
      WindowFX.getWindows().forEach { it.scene?.applySkinGui(skin) }
   }

   private fun Scene.applySkinGui(skin: String) {
      stylesheets.clear()
      this setUserAgentStyleSheet skinDir/skin/"$skin.css"
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

      private infix fun Scene.setUserAgentStyleSheet(file: File) = file.toStyleSheet()?.let { userAgentStylesheet = it }
      private infix fun Scene.addStyleSheet(file: File) = file.toStyleSheet()?.let { stylesheets += it }

   }

}

class AdditionalStylesheets(private val set: MutableSet<File> = LinkedHashSet()): Set<File> by set {
   val onChange = Handler0()

   operator fun minusAssign(stylesheet: File) {
      set -= stylesheet
      onChange()
   }

   operator fun plusAssign(stylesheet: File) {
      set += stylesheet
      onChange()
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