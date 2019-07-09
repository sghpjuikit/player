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
import javafx.scene.input.MouseEvent
import javafx.scene.text.Font
import mu.KLogging
import sp.it.pl.gui.objects.rating.Rating
import sp.it.pl.gui.pane.ErrorPane
import sp.it.pl.gui.pane.InfoPane
import sp.it.pl.gui.pane.OverlayPane
import sp.it.pl.gui.pane.ScreenBgrGetter
import sp.it.pl.layout.widget.WidgetSource.OPEN
import sp.it.pl.main.APP
import sp.it.pl.main.Actions
import sp.it.pl.main.initActionPane
import sp.it.pl.main.initApp
import sp.it.util.access.Values
import sp.it.util.access.toggle
import sp.it.util.action.IsAction
import sp.it.util.collections.ObservableSetRO
import sp.it.util.collections.project
import sp.it.util.collections.setTo
import sp.it.util.conf.Configurable
import sp.it.util.conf.IsConfig
import sp.it.util.conf.IsConfigurable
import sp.it.util.conf.between
import sp.it.util.conf.c
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
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
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onItemAdded
import sp.it.util.reactive.onItemSyncWhile
import sp.it.util.reactive.plus
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncNonNullIntoWhile
import sp.it.util.ui.isAnyParentOf
import sp.it.util.ui.setFontAsStyle
import sp.it.util.units.millis
import java.io.File
import java.net.MalformedURLException
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName
import kotlin.streams.asSequence
import javafx.stage.Window as WindowFX
import sp.it.pl.gui.pane.ActionPane as PaneA
import sp.it.pl.gui.pane.ShortcutPane as PaneS
import sp.it.pl.main.Settings.Ui as SU

@IsConfigurable(SU.name)
class UiManager(val skinDir: File): Configurable<Any> {

   @IsConfig(name = "Display method", group = SU.View.name, info = "Area of content. Screen provides more space than window, but can get in the way of other apps.")
   val viewDisplay by cv(OverlayPane.Display.SCREEN_OF_MOUSE)
   @IsConfig(name = "Display background", group = SU.View.name, info = "Content background")
   val viewDisplayBgr by cv(ScreenBgrGetter.SCREEN_BGR)
   @IsConfig(name = PaneA.CLOSE_ON_DONE_NAME, info = PaneA.CLOSE_ON_DONE_INFO, group = SU.View.Action.name)
   val viewCloseOnDone by cv(true)
   @IsConfig(name = PaneS.HIDE_EMPTY_NAME, info = PaneS.HIDE_EMPTY_INFO, group = SU.View.Shortcut.name)
   val viewHideEmptyShortcuts by cv(true)

   /** Action chooser and data info view. */
   val actionPane = LazyOverlayPane { PaneA(APP.className, APP.instanceName, APP.instanceInfo).initApp().initActionPane() }
   /** Error detail view. Usually used internally by [sp.it.pl.main.AppErrors]. */
   val errorPane = LazyOverlayPane { ErrorPane().initApp() }
   /** Shortcut bindings/keymap detail view. */
   val shortcutPane = LazyOverlayPane { PaneS().initApp() }
   /** System/app info detail view. */
   val infoPane = LazyOverlayPane { InfoPane().initApp() }

   private val skinsImpl = observableSet<SkinCss>()
   /** Available application skins. Monitored and updated from disc. */
   val skins = ObservableSetRO<SkinCss>(skinsImpl)
   /** Css files applied on top of [skin]. Can be used for clever stuff like applying generated css. */
   val additionalStylesheets = observableArrayList<File>()!!

   val layoutMode: BooleanProperty = SimpleBooleanProperty(false)
   val focusChangedHandler: (Node?) -> Unit = { n ->
      val window = n?.scene
      if (n!=null)
         APP.widgetManager.widgets.findAll(OPEN)
            .filter { it.uiTemp?.root?.isAnyParentOf(n) ?: false }
            .findAny()
            .ifPresent { fw ->
               APP.widgetManager.widgets.findAll(OPEN)
                  .filter { w -> w!==fw && w.window.orNull()?.stage?.scene?.net { it===window } ?: false }
                  .forEach { w -> w.focused.value = false }
               fw.focused.value = true
            }
   }

   init {
      initSkins()
   }

   /** Skin of the application. Defined stylesheet file to be applied on `.root` of windows. */
   @IsConfig(name = "Skin", info = "Application skin.")
   val skin by cv("Main").values(skins.project { it.name })

   /** Font of the application. Overrides `-fx-font-family` and `-fx-font-size` defined by css on `.root`. */
   @IsConfig(name = "Font", info = "Application font.")
   val font by cv(Font.getDefault())

   @IsConfig(name = "Layout mode blur bgr", info = "Layout mode use blur effect.")
   val blurLayoutMode by cv(false)
   @IsConfig(name = "Layout mode fade bgr", info = "Layout mode use fade effect.")
   val opacityLayoutMode by cv(true)
   @IsConfig(name = "Layout mode fade intensity", info = "Layout mode fade effect intensity.")
   var opacityLM by c(1.0).between(0.0, 1.0).readOnlyUnless(opacityLayoutMode)
   @IsConfig(name = "Layout mode blur intensity", info = "Layout mode blur effect intensity.")
   var blurLM by c(4.0).between(0.0, 20.0).readOnlyUnless(blurLayoutMode)
   @IsConfig(name = "Layout mode anim length", info = "Duration of layout mode transition effects.")
   var durationLM by c(250.millis)
   @IsConfig(name = "Snap", info = "Allows snapping feature for windows and controls.")
   val snapping by cv(true)
   @IsConfig(name = "Snap activation distance", info = "Distance at which snap feature gets activated")
   val snapDistance by cv(12.0)
   @IsConfig(name = "Lock layout", info = "Locked layout will not enter layout mode.")
   val lockedLayout by cv(false) { SimpleBooleanProperty(it) }.attach { APP.actionStream("Layout lock"); println("lock") }

   @IsConfig(name = "Table orientation", group = SU.Table.name, info = "Orientation of the table.")
   val tableOrient by cv(NodeOrientation.INHERIT)
   @IsConfig(name = "Zeropad numbers", group = SU.Table.name, info = "Adds 0s for number length consistency.")
   val tableZeropad by cv(false)
   @IsConfig(name = "Search show original index", group = SU.Table.name, info = "Show unfiltered table item index when filter applied.")
   val tableOrigIndex by cv(false)
   @IsConfig(name = "Show table header", group = SU.Table.name, info = "Show table header with columns.")
   val tableShowHeader by cv(true)
   @IsConfig(name = "Show table controls", group = SU.Table.name, info = "Show table controls at the bottom of the table. Displays menu bar and table content information")
   val tableShowFooter by cv(true)

   @IsConfig(name = "Thumbnail anim duration", group = SU.Image.name, info = "Preferred hover scale animation duration for thumbnails.")
   val thumbnailAnimDur by cv(100.millis)

   @IsConfig(name = "Rating skin", info = "Rating ui component skin")
   val ratingSkin by cvn<KClass<out Skin<Rating>>>(null).valuesIn(APP.instances).uiConverter {
      it?.simpleName ?: "<none> (App skin decides)"
   } sync {
      val f = APP.locationTmp/"user-rating-skin.css"
      additionalStylesheets -= f
      it?.let {
         f.writeTextTry(""".rating { -fx-skin: "${it.jvmName}"; }""", Charsets.UTF_8)
            .ifError { logger.error(it) { "Failed to apply rating skin=$it" } }
         additionalStylesheets += f
      }
   }

   @IsConfig(name = "Rating icon amount", info = "Number of icons in rating control.")
   val maxRating by cv(5).between(0, 10)

   @IsConfig(name = "Rating allow partial", info = "Allow partial values for rating.")
   val partialRating by cv(true)

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
            APP.widgetManager.layouts.findAllActive().forEach { it.show() }
            layoutMode.set(v)
         } else {
            layoutMode.set(v)
            APP.widgetManager.layouts.findAllActive().forEach { it.hide() }
            setZoomMode(false)
         }
         if (v) APP.actionStream(Actions.LAYOUT_MODE)
      }

   fun focusClickedWidget(e: MouseEvent) {
      val n = e.target as? Node
      if (n!=null)
         APP.widgetManager.widgets.findAll(OPEN).asSequence()
            .find { !it.focused.value && it.isLoaded && it.load().isAnyParentOf(n) }
            ?.focus()
   }

   /** Toggles lock to prevent user accidental layout change.  */
   @IsAction(name = "Toggle layout lock", desc = "Lock/unlock layout.", keys = "F4")
   fun toggleLayoutLocked() = lockedLayout.toggle()

   /** Loads/refreshes active layout.  */
   @IsAction(name = "Reload layout", desc = "Reload layout.", keys = "F6")
   fun loadLayout() = APP.widgetManager.layouts.findAllActive().forEach { it.load() }

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

   private fun initSkins() {
      skinsImpl setTo findSkins()
      monitorSkinFiles()
      observeWindowsAndApplySkin()
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

   private fun observeWindowsAndApplySkin() {
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

   fun hide() = pane?.hide() ?: Unit
}