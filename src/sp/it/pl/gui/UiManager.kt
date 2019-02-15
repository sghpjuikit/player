package sp.it.pl.gui

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.NodeOrientation
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.ContextMenu
import javafx.scene.control.Tooltip
import javafx.scene.input.MouseEvent
import javafx.scene.text.Font
import javafx.stage.Popup
import javafx.stage.Stage
import javafx.stage.Window
import mu.KLogging
import sp.it.pl.gui.objects.popover.PopOver
import sp.it.pl.layout.widget.WidgetSource.ANY
import sp.it.pl.main.APP
import sp.it.pl.main.Actions
import sp.it.pl.main.Settings
import sp.it.pl.util.access.VarEnum
import sp.it.pl.util.action.IsAction
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.IsConfigurable
import sp.it.pl.util.conf.between
import sp.it.pl.util.conf.c
import sp.it.pl.util.conf.cv
import sp.it.pl.util.file.FileMonitor
import sp.it.pl.util.file.Util
import sp.it.pl.util.file.childOf
import sp.it.pl.util.file.div
import sp.it.pl.util.file.isAnyParentOf
import sp.it.pl.util.file.seqChildren
import sp.it.pl.util.functional.Util.set
import sp.it.pl.util.functional.net
import sp.it.pl.util.functional.orNull
import sp.it.pl.util.functional.seqOf
import sp.it.pl.util.graphics.isAnyParentOf
import sp.it.pl.util.graphics.setFontAsStyle
import sp.it.pl.util.reactive.attach
import sp.it.pl.util.reactive.onItemAdded
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.reactive.sync1IfNonNull
import sp.it.pl.util.units.millis
import java.io.File
import java.net.MalformedURLException
import java.util.HashSet

@IsConfigurable(Settings.UI)
class UiManager(val skinDir: File): Configurable<Any> {
    val skins: MutableSet<SkinCss> = HashSet()

    val layoutMode: BooleanProperty = SimpleBooleanProperty(false)
    val focusChangedHandler: (Node?) -> Unit = { n ->
        val window = n?.scene
        if (n!=null)
            APP.widgetManager.widgets.findAll(ANY)
                .filter { it.areaTemp?.root?.isAnyParentOf(n) ?: false }
                .findAny()
                .ifPresent { fw ->
                    APP.widgetManager.widgets.findAll(ANY)
                            .filter { w -> w!==fw && w.window.orNull()?.stage?.scene?.net { it===window } ?: false }
                            .forEach { w -> w.focused.value = false }
                    fw.focused.value = true
                }
    }

    @IsConfig(name = "Skin", info = "Application skin.")
    val skin by cv("Flow") { VarEnum.ofStream(it) { skins.stream().map { it.name } } }

    /**
     * Font of the application. Overrides font defined by skin. The font can be overridden programmatically or stylesheet.
     *
     * Note: font is applied only if the GUI is fully initialized, otherwise does nothing.
     */
    @IsConfig(name = "Font", info = "Application font.")
    val font by cv(Font.getDefault())

    @IsConfig(name = "Layout mode blur bgr", info = "Layout mode use blur effect.")
    var blurLayoutMode by c(false)
    @IsConfig(name = "Layout mode fade bgr", info = "Layout mode use fade effect.")
    var opacityLayoutMode by c(true)
    @IsConfig(name = "Layout mode fade intensity", info = "Layout mode fade effect intensity.")
    var opacityLM by c(1.0).between(0.0, 1.0)
    @IsConfig(name = "Layout mode blur intensity", info = "Layout mode blur effect intensity.")
    var blurLM by c(4.0).between(0.0, 20.0)
    @IsConfig(name = "Layout mode anim length", info = "Duration of layout mode transition effects.")
    var durationLM by c(250.millis)
    @IsConfig(name = "Snap", info = "Allows snapping feature for windows and controls.")
    val snapping by cv(true)
    @IsConfig(name = "Snap activation distance", info = "Distance at which snap feature gets activated")
    val snapDistance by cv(12.0)
    @IsConfig(name = "Lock layout", info = "Locked layout will not enter layout mode.")
    val lockedLayout by cv(false) { SimpleBooleanProperty(it).apply { attach { APP.actionStream.push("Layout lock") } } }
    @IsConfig(name = "Table orientation", group = Settings.Ui.TABLE, info = "Orientation of the table.")
    val tableOrient by cv(NodeOrientation.INHERIT)
    @IsConfig(name = "Zeropad numbers", group = Settings.Ui.TABLE, info = "Adds 0s for number length consistency.")
    val tableZeropad by cv(false)
    @IsConfig(name = "Search show original index", group = Settings.Ui.TABLE, info = "Show unfiltered table item index when filter applied.")
    val tableOrigIndex by cv(false)
    @IsConfig(name = "Show table header", group = Settings.Ui.TABLE, info = "Show table header with columns.")
    val tableShowHeader by cv(true)
    @IsConfig(name = "Show table controls", group = Settings.Ui.TABLE, info = "Show table controls at the bottom of the table. Displays menu bar and table content information")
    val tableShowFooter by cv(true)
    @IsConfig(name = "Thumbnail anim duration", group = "${Settings.UI}.Images", info = "Preferred hover scale animation duration for thumbnails.")
    val thumbnailAnimDur by cv(100.millis)

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
            if (v) APP.actionStream.push(Actions.LAYOUT_MODE)
        }


    init {
        initSkins()
    }

    fun focusClickedWidget(e: MouseEvent) {
        val n = e.target as? Node
        if (n!=null)
            APP.widgetManager.widgets.findAll(ANY)
                .filter { !it.focused.value && it.isLoaded && it.load().isAnyParentOf(n) }
                .findAny().ifPresent { it.focus() }
    }

    /** Toggles lock to prevent user accidental layout change.  */
    @IsAction(name = "Toggle layout lock", desc = "Lock/unlock layout.", keys = "F4")
    fun toggleLayoutLocked() = lockedLayout.set(!lockedLayout.value)

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
        if (!anyM && anyF) {}
        else APP.windowManager.windows.forEach { it.isMinimized = false; it.focus() }
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
        APP.windowManager.windows.forEach { it.isMinimized = !m }
    }

    @IsAction(name = "Maximize window", desc = "Switch maximized mode.", keys = "F11")
    fun toggleMaximize() = APP.windowManager.active.orNull()?.toggleMaximize()

    @IsAction(name = "Loop maximized state", desc = "Switch to different maximized window states.", keys = "F3")
    fun toggleMaximizedState() = APP.windowManager.active.orNull()?.let { it.isMaximized = it.isMaximized.next() }

    @IsAction(name = "Fullscreen", desc = "Switch fullscreen mode.", keys = "F12")
    fun toggleFullscreen() = APP.windowManager.active.orNull()?.toggleFullscreen()

    @IsAction(name = "Layout align", desc = "Aligns layout of the active window", keys = "ALT+UP")
    fun tabAlign() = APP.windowManager.active.orNull()?.switchPane?.alignTabs()

    @IsAction(name = "Layout move left", desc = "Moves layout of the active window to the left.", keys = "ALT+LEFT")
    fun tabPrevious() = APP.windowManager.active.orNull()?.switchPane?.alignLeftTab()

    @IsAction(name = "Layout move right", desc = "Moves layout of the active window to the right.", keys = "ALT+RIGHT")
    fun tabNext() = APP.windowManager.active.orNull()?.switchPane?.alignRightTab()

    @IsAction(name = Actions.LAYOUT_MODE, desc = "Shows/hides layout overlay.", keys = "F8")
    fun toggleLayoutMode() {
        isLayoutMode = !layoutMode.get()
    }

    @IsAction(name = "Layout zoom in/out", desc = "Toggles layout zoom in/out.")
    fun toggleZoomMode() = APP.windowManager.active.orNull()?.switchPane?.toggleZoom()

    fun setLayoutNzoom(v: Boolean) {
        isLayoutMode = v
        setZoomMode(v)
    }

    fun setZoomMode(v: Boolean) = APP.windowManager.active.orNull()?.switchPane?.zoom(v)

    @IsAction(name = "Layout zoom overlay in/out", desc = "Shows/hides layout overlay & zooms in/out.", keys = "ALT+DOWN")
    fun toggleLayoutNzoom() {
        toggleLayoutMode()
        toggleZoomMode()
    }

    /**
     * Searches for .css files in skin folder and registers them as available
     * skins. Use on app start or to discover newly added layouts dynamically.
     */
    fun findSkins(): Set<SkinCss> {
        if (!Util.isValidatedDirectory(skinDir)) {
            logger.error("Skin lookup failed."+skinDir.path+" could not be accessed.")
            return set()
        }

        return skinDir.seqChildren()
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
        skins.add(s)
        return s
    }

    fun setSkin(s: SkinCss) {
        logger.info("Setting skin={}", s.name)

        registerSkin(s)
        skin.set(s.name)
    }

    fun setSkin(cssFile: File) {
        logger.info("Setting skin file={}", cssFile)

        val s = skins.stream()
                .filter { (_, file) -> file==cssFile }.findAny()
                .orElseGet { registerSkin(SkinCss(cssFile)) }
        setSkin(s)
    }

    private fun initSkins() {
        skins.clear()
        skins += findSkins()
        monitorSkinFiles()
        observeWindowsAndApplySkin()
    }

    private fun monitorSkinFiles() {
        FileMonitor.monitorDirectory(skinDir, true) { type, file ->
            logger.info { "Change=$type detected in skin directory for $file" }

            skins.clear()
            skins += findSkins()

            val refreshAlways = true    // skins may import each other hence it is more convenient to refresh always
            val currentSkinDir = skinDir.childOf(skin.get())
            val isActive = currentSkinDir isAnyParentOf file
            if (isActive || refreshAlways) reloadSkin()
        }
    }

    private fun observeWindowsAndApplySkin() {
        fun Parent.initializeFontAndSkin() {
            if (properties.containsKey(skinInitMarker)) return
            properties[skinInitMarker] = skinInitMarker

            skin sync { applySkinGui(it) }
            font sync { applyFontGui(it) }
        }

        fun Window.initializeFontAndSkin() {
            sceneProperty().sync1IfNonNull { it.rootProperty().sync1IfNonNull { it.initializeFontAndSkin() } }
        }

        seqOf(Popup.getWindows(), Stage.getWindows(), ContextMenu.getWindows(), PopOver.active_popups)
                .forEach { windows ->
                    windows.forEach { it.initializeFontAndSkin() }
                    windows.onItemAdded { it.initializeFontAndSkin() }
                }
        Tooltip.getWindows().onItemAdded { (it as? Tooltip)?.font = font.value }
    }

    fun applySkin(skin: String) {
        seqOf(Popup.getWindows(), Stage.getWindows(), ContextMenu.getWindows(), PopOver.active_popups)
                .flatMap { it.asSequence() }
                .mapNotNull { it?.scene?.root }
                .forEach { it.applySkinGui(skin) }
    }

    private fun Parent.applyFontGui(font: Font) {
        setFontAsStyle(font)
    }

    private fun Parent.applySkinGui(skin: String) {
        val skinFile = skinDir/skin/"$skin.css"
        val urlOld = properties[skinKey] as String?
        val urlNew = try {
            skinFile.toURI().toURL().toExternalForm()
        } catch (e: MalformedURLException) {
            logger.error(e) { "Could not load skin $skinFile" }
            null
        }
        if (urlOld!=null) stylesheets -= urlOld
        if (urlNew!=null) stylesheets += urlNew
        properties[skinKey] = urlNew
    }

    enum class OpenStrategy {
        POPUP, INSIDE
    }

    companion object: KLogging() {
        private const val skinKey = "skin_old_url"
        private const val skinInitMarker = "HAS_BEEN_INITIALIZED"
    }
}